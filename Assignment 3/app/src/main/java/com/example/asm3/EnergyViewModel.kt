package com.example.asm3

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.asm3.data.ExcelEnergyParser
import com.example.asm3.data.FirebaseEnergyParser
import com.example.asm3.data.MockEnergyRepository
import com.example.asm3.model.BmsState
import com.example.asm3.model.DashboardUiState
import com.example.asm3.model.EnergySnapshot
import com.example.asm3.model.EnergyUiState
import com.example.asm3.model.SettingsState
import com.example.asm3.model.StatsRange
import java.time.Duration
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EnergyViewModel(
    application: Application
) : AndroidViewModel(application) {
    
    private val repository: MockEnergyRepository = MockEnergyRepository()
    
    // Firebase parser for fetching data from Firebase Realtime Database
    private val firebaseParser: FirebaseEnergyParser by lazy {
        FirebaseEnergyParser()
    }
    
    // Lazy initialization to avoid crash if POI is not compatible
    private val parser: ExcelEnergyParser? by lazy {
        try {
            ExcelEnergyParser(application)
        } catch (e: Exception) {
            null // POI not available, will use mock data
        }
    }

    private val _uiState = MutableStateFlow(
        EnergyUiState(
            dashboard = DashboardUiState(isLoading = true),
            stats = repository.createStatsState(),
            bms = repository.createBmsState(),
            settings = repository.createSettingsState()
        )
    )
    val uiState: StateFlow<EnergyUiState> = _uiState.asStateFlow()

    private var records: List<EnergySnapshot> = emptyList()
    private var playbackJob: Job? = null

    init {
        // Load data from Firebase on startup
        viewModelScope.launch {
            delay(100)
            reloadSnapshotsFromFirebase()
        }
    }

    fun refreshDashboard() {
        reloadSnapshotsFromFirebase()
    }

    fun selectStartSlot(slot: LocalDateTime) {
        val dashboard = _uiState.value.dashboard
        val newEnd = dashboard.selectedEnd?.takeIf { !slot.isAfter(it) } ?: slot
        updateDashboard {
            copy(selectedStart = slot, selectedEnd = newEnd)
        }
        startPlayback(slot, newEnd)
    }

    fun selectEndSlot(slot: LocalDateTime) {
        val dashboard = _uiState.value.dashboard
        val newStart = dashboard.selectedStart?.takeIf { !slot.isBefore(it) } ?: slot
        updateDashboard {
            copy(selectedStart = newStart, selectedEnd = slot)
        }
        startPlayback(newStart, slot)
    }

    fun toggleNotifications(enabled: Boolean) = updateSettings {
        copy(notificationsEnabled = enabled)
    }

    fun toggleBatteryAlerts(enabled: Boolean) = updateSettings {
        copy(batteryAlertsEnabled = enabled)
    }

    fun toggleDarkMode(enabled: Boolean) = updateSettings {
        copy(darkMode = enabled)
    }

    fun toggleAutoExport(enabled: Boolean) = updateSettings {
        copy(autoExportReports = enabled)
    }

    fun updateFrequency(seconds: Int) = updateSettings {
        copy(updateFrequencySeconds = seconds)
    }

    fun onExcelFileSelected(uri: Uri?, displayName: String?) {
        updateSettings {
            copy(
                excelFileUri = uri?.toString(),
                excelFileName = displayName ?: uri?.lastPathSegment
            )
        }
        reloadSnapshots(uri?.toString())
    }
    
    fun onFirebasePathChanged(path: String) {
        updateSettings {
            copy(
                excelFileUri = null,
                excelFileName = "Firebase: $path"
            )
        }
        reloadSnapshotsFromFirebase(path)
    }

    fun selectStatsRange(range: StatsRange) {
        _uiState.update { state ->
            if (state.stats.selectedRange == range) state
            else state.copy(stats = state.stats.copy(selectedRange = range))
        }
    }

    fun updateChargeCurrent(value: Float) = updateBms {
        copy(maxChargeCurrentA = value)
    }

    fun updateDischargeCurrent(value: Float) = updateBms {
        copy(maxDischargeCurrentA = value)
    }

    fun updateCutOffVoltage(value: Float) = updateBms {
        copy(cutOffVoltageV = value)
    }

    private fun reloadSnapshotsFromFirebase(path: String = "") {
        viewModelScope.launch {
            updateDashboard { copy(isLoading = true, errorMessage = null) }
            
            // Process all heavy operations on background thread
            val result = withContext(Dispatchers.Default) {
                runCatching {
                    val snapshots = firebaseParser.loadSnapshots(path)
                    
                    if (snapshots.isEmpty()) {
                        throw Exception("Empty data")
                    }
                    
                    // Process data on background thread
                    // Ensure records are sorted chronologically
                    val sortedSnapshots = snapshots.sortedBy { it.timestamp }
                    
                    // Verify sorting and log for debugging
                    if (sortedSnapshots.size > 1) {
                        val isSorted = sortedSnapshots.zipWithNext().all { (first, second) ->
                            !first.timestamp.isAfter(second.timestamp)
                        }
                        if (!isSorted) {
                            println("Warning: Records are not properly sorted!")
                        }
                    }
                    
                    val start = sortedSnapshots.first().timestamp.truncatedToHour()
                    val end = sortedSnapshots.last().timestamp.truncatedToHour()
                    
                    // Use HashSet for faster distinct operation
                    val slotsSet = mutableSetOf<LocalDateTime>()
                    sortedSnapshots.forEach { snapshot ->
                        slotsSet.add(snapshot.timestamp.truncatedToHour())
                    }
                    val slots = slotsSet.sorted()
                    
                    // Log for debugging
                    println("EnergyViewModel: Loaded ${sortedSnapshots.size} records")
                    println("EnergyViewModel: Time range: ${sortedSnapshots.first().timestamp} to ${sortedSnapshots.last().timestamp}")
                    println("EnergyViewModel: Available slots: ${slots.size} hours")
                    
                    // Use Pair of Triple and List for 4 values
                    Result.success(Pair(Triple(sortedSnapshots, start, end), slots))
                }.getOrElse { exception ->
                    // Fallback to mock data if Firebase fetch fails
                    val mockSnapshots = repository.generateMockSnapshots()
                    if (mockSnapshots.isNotEmpty()) {
                        val sortedMock = mockSnapshots.sortedBy { it.timestamp }
                        val start = sortedMock.first().timestamp.truncatedToHour()
                        val end = sortedMock.last().timestamp.truncatedToHour()
                        val slotsSet = mutableSetOf<LocalDateTime>()
                        sortedMock.forEach { snapshot ->
                            slotsSet.add(snapshot.timestamp.truncatedToHour())
                        }
                        val slots = slotsSet.sorted()
                        Result.success(Pair(Triple(sortedMock, start, end), slots))
                    } else {
                        Result.failure(exception)
                    }
                }
            }
            
            // Update UI on main thread
            result.fold(
                onSuccess = { (triple, slots) ->
                    val (snapshots, start, end) = triple
                    records = snapshots
                    val firstSnapshot = snapshots.first()
                    updateDashboard {
                        copy(
                            isLoading = false,
                            currentSnapshot = firstSnapshot,
                            availableSlots = slots,
                            selectedStart = start,
                            selectedEnd = end,
                            errorMessage = if (snapshots.size < 100) {
                                "Using mock data (Firebase error or empty)"
                            } else null
                        )
                    }
                    // Update BMS state from first snapshot
                    updateBmsFromSnapshot(firstSnapshot)
                    startPlayback(start, end)
                },
                onFailure = { exception ->
                    updateDashboard {
                        copy(
                            isLoading = false,
                            errorMessage = "Error loading data from Firebase: ${exception.message ?: "Unknown error"}"
                        )
                    }
                    playbackJob?.cancel()
                    records = emptyList()
                }
            )
        }
    }

    private fun reloadSnapshots(uriString: String?) {
        viewModelScope.launch {
            updateDashboard { copy(isLoading = true, errorMessage = null) }
            
            // If parser is null (POI not available), use mock data directly
            if (parser == null) {
                val mockSnapshots = repository.generateMockSnapshots()
                if (mockSnapshots.isNotEmpty()) {
                    records = mockSnapshots
                    val start = mockSnapshots.first().timestamp.truncatedToHour()
                    val end = mockSnapshots.last().timestamp.truncatedToHour()
                    val slots = mockSnapshots.map { it.timestamp.truncatedToHour() }.distinct()
                    updateDashboard {
                        copy(
                            isLoading = false,
                            currentSnapshot = mockSnapshots.first(),
                            availableSlots = slots,
                            selectedStart = start,
                            selectedEnd = end,
                            errorMessage = "Using mock data (POI library not available)"
                        )
                    }
                    startPlayback(start, end)
                    return@launch
                }
            }
            
            val snapshots = runCatching {
                withContext(Dispatchers.IO) { parser?.loadSnapshots(uriString) ?: emptyList() }
            }.getOrElse { exception ->
                // Fallback to mock data if Excel parsing fails
                val mockSnapshots = repository.generateMockSnapshots()
                if (mockSnapshots.isNotEmpty()) {
                    records = mockSnapshots
                    val start = mockSnapshots.first().timestamp.truncatedToHour()
                    val end = mockSnapshots.last().timestamp.truncatedToHour()
                    val slots = mockSnapshots.map { it.timestamp.truncatedToHour() }.distinct()
                    updateDashboard {
                        copy(
                            isLoading = false,
                            currentSnapshot = mockSnapshots.first(),
                            availableSlots = slots,
                            selectedStart = start,
                            selectedEnd = end,
                            errorMessage = "Using mock data (Excel parser error: ${exception.message?.take(50)})"
                        )
                    }
                    startPlayback(start, end)
                    return@launch
                }
                updateDashboard {
                    copy(
                        isLoading = false,
                        errorMessage = "Error reading Excel file: ${exception.message ?: "Unknown error"}"
                    )
                }
                playbackJob?.cancel()
                records = emptyList()
                return@launch
            }
            if (snapshots.isEmpty()) {
                // Fallback to mock data
                val mockSnapshots = repository.generateMockSnapshots()
                if (mockSnapshots.isNotEmpty()) {
                    records = mockSnapshots
                    val start = mockSnapshots.first().timestamp.truncatedToHour()
                    val end = mockSnapshots.last().timestamp.truncatedToHour()
                    val slots = mockSnapshots.map { it.timestamp.truncatedToHour() }.distinct()
                    updateDashboard {
                        copy(
                            isLoading = false,
                            currentSnapshot = mockSnapshots.first(),
                            availableSlots = slots,
                            selectedStart = start,
                            selectedEnd = end,
                            errorMessage = "Using mock data (Excel file is empty)"
                        )
                    }
                    startPlayback(start, end)
                    return@launch
                }
                updateDashboard {
                    copy(
                        isLoading = false,
                        errorMessage = "No data found in Excel file."
                    )
                }
                playbackJob?.cancel()
                records = emptyList()
                return@launch
            }
            records = snapshots
            val start = snapshots.first().timestamp.truncatedToHour()
            val end = snapshots.last().timestamp.truncatedToHour()
            val slots = snapshots.map { it.timestamp.truncatedToHour() }.distinct()

            updateDashboard {
                copy(
                    isLoading = false,
                    currentSnapshot = snapshots.first(),
                    availableSlots = slots,
                    selectedStart = start,
                    selectedEnd = end,
                    errorMessage = null
                )
            }
            startPlayback(start, end)
        }
    }

    private fun startPlayback(startSlot: LocalDateTime?, endSlot: LocalDateTime?) {
        playbackJob?.cancel()
        val window = buildWindow(startSlot, endSlot)
        if (window.isEmpty()) {
            updateDashboard { copy(errorMessage = "No data available for selected time range.") }
            return
        }
        updateDashboard { copy(errorMessage = null) }
        playbackJob = viewModelScope.launch {
            var index = 0
            while (isActive) {
                if (index >= window.size) {
                    // Loop back to start when reaching the end
                    index = 0
                }
                val snapshot = window[index]
                updateDashboard { copy(currentSnapshot = snapshot) }
                // Update BMS state from current snapshot
                updateBmsFromSnapshot(snapshot)
                index++
                
                // Fixed 2 second delay between each record
                delay(2000)
            }
        }
    }

    private fun buildWindow(startSlot: LocalDateTime?, endSlot: LocalDateTime?): List<EnergySnapshot> {
        if (records.isEmpty()) return emptyList()
        
        // If slots are provided, they are already truncated to hour
        // Filter records that fall within the hour range
        val start = startSlot ?: records.first().timestamp.truncatedToHour()
        val end = endSlot ?: records.last().timestamp.truncatedToHour()
        
        // Include all records from start hour (00:00) to end hour (59:59)
        val endLimit = end.plusHours(1).minusSeconds(1)
        
        val filtered = records.filter { snapshot ->
            val ts = snapshot.timestamp
            // Include if timestamp is within the range [start, endLimit]
            // start is truncated to hour (e.g., 03:00:00), endLimit is end hour + 59:59
            !ts.isBefore(start) && !ts.isAfter(endLimit)
        }.sortedBy { it.timestamp } // Ensure chronological order
        
        // Log for debugging
        println("buildWindow: start=$start, end=$end, total records=${records.size}, filtered=${filtered.size}")
        if (filtered.isNotEmpty()) {
            println("buildWindow: first=${filtered.first().timestamp}, last=${filtered.last().timestamp}")
            // Check for gaps
            if (filtered.size > 1) {
                val timeDiffs = filtered.zipWithNext().map { (first, second) ->
                    Duration.between(first.timestamp, second.timestamp).toMinutes()
                }
                val avgDiff = timeDiffs.average()
                println("buildWindow: Average time difference: ${avgDiff.toInt()} minutes")
                val largeGaps = timeDiffs.filter { it > 10 }
                if (largeGaps.isNotEmpty()) {
                    println("buildWindow: Warning: Found ${largeGaps.size} gaps > 10 minutes")
                }
            }
        }
        
        return filtered
    }

    private fun LocalDateTime.truncatedToHour(): LocalDateTime =
        this.truncatedTo(ChronoUnit.HOURS)

    private fun updateDashboard(transform: DashboardUiState.() -> DashboardUiState) {
        _uiState.update { state ->
            state.copy(dashboard = state.dashboard.transform())
        }
    }

    private fun updateSettings(transform: SettingsState.() -> SettingsState) {
        _uiState.update { state ->
            state.copy(settings = state.settings.transform())
        }
    }

    private fun updateBms(transform: BmsState.() -> BmsState) {
        _uiState.update { state ->
            state.copy(bms = state.bms.transform())
        }
    }
    
    private fun updateBmsFromSnapshot(snapshot: EnergySnapshot) {
        val battery = snapshot.battery
        val currentBms = _uiState.value.bms
        
        // Update BMS state from snapshot's battery data
        updateBms {
            copy(
                socPercent = battery.socPercent ?: battery.percentage,
                sohPercent = battery.sohPercent ?: currentBms.sohPercent, // Keep existing if not available
                voltageV = battery.voltageVdc,
                currentA = battery.currentA ?: currentBms.currentA, // Use from Firebase or keep existing
                temperatureC = battery.temperatureC ?: currentBms.temperatureC, // Use from Firebase or keep existing
                maxChargeCurrentA = battery.maxChargeCurrentA?.toFloat() ?: currentBms.maxChargeCurrentA,
                maxDischargeCurrentA = battery.maxDischargeCurrentA?.toFloat() ?: currentBms.maxDischargeCurrentA,
                cutOffVoltageV = currentBms.cutOffVoltageV, // Keep existing setting
                // Keep cells from current state (not available in Firebase data)
                cells = currentBms.cells
            )
        }
    }
}

