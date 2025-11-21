package com.example.asm3.data

import com.example.asm3.model.BatterySection
import com.example.asm3.model.ControlSection
import com.example.asm3.model.EnergyFlowSection
import com.example.asm3.model.EnergySnapshot
import com.example.asm3.model.EpsSection
import com.example.asm3.model.GridSection
import com.example.asm3.model.LoadSection
import com.example.asm3.model.SolarSection
import com.example.asm3.model.SolarStringStatus
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import okhttp3.OkHttpClient
import okhttp3.Request
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

class FirebaseEnergyParser {
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()
    private val gson = Gson()
    
    private val timestampFormats = listOf(
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
        DateTimeFormatter.ofPattern("yyyy/M/d HH:mm:ss"),
        DateTimeFormatter.ISO_LOCAL_DATE_TIME
    )
    
    private val baseUrl = "https://solar-energy-6452d-default-rtdb.firebaseio.com/"
    
    suspend fun loadSnapshots(path: String = "", limit: Int = 50000): List<EnergySnapshot> {
        return withContext(Dispatchers.IO) {
            try {
                val url = if (path.isBlank()) {
                    "${baseUrl}.json"
                } else {
                    "${baseUrl}${path}.json"
                }
                
                val request = Request.Builder()
                    .url(url)
                    .get()
                    .build()
                
                // Add timeout to prevent hanging
                val response = client.newCall(request).execute()
                
                if (!response.isSuccessful) {
                    return@withContext emptyList()
                }
                
                val responseBody = response.body?.string() ?: return@withContext emptyList()
                
                // Parse JSON response in chunks to avoid memory issues
                val jsonElement = gson.fromJson(responseBody, JsonElement::class.java)
                
                val records = mutableListOf<EnergySnapshot>()
                var processedCount = 0
                
                when {
                    jsonElement.isJsonArray -> {
                        // If it's an array, parse each element - parse ALL records, no limit for data integrity
                        val jsonArray = jsonElement.asJsonArray
                        val size = jsonArray.size()
                        var skippedCount = 0
                        for (i in 0 until size) {
                            val element = jsonArray.get(i)
                            if (element.isJsonObject) {
                                val snapshot = element.asJsonObject.toSnapshot()
                                if (snapshot != null) {
                                    records.add(snapshot)
                                    processedCount++
                                } else {
                                    skippedCount++
                                }
                            }
                            // Yield periodically to avoid blocking
                            if (i % 100 == 0) {
                                yield()
                            }
                        }
                        if (skippedCount > 0) {
                            println("FirebaseEnergyParser: Skipped $skippedCount invalid records out of $size total")
                        }
                    }
                    jsonElement.isJsonObject -> {
                        // If it's an object, check if it contains an array
                        val jsonObject = jsonElement.asJsonObject
                        // Try to find array in the object
                        var foundArray = false
                        jsonObject.entrySet().forEach { entry ->
                            if (processedCount >= limit) return@forEach
                            
                            if (entry.value.isJsonArray) {
                                foundArray = true
                                val jsonArray = entry.value.asJsonArray
                                val size = minOf(jsonArray.size(), limit - processedCount)
                                for (i in 0 until size) {
                                    val element = jsonArray.get(i)
                                    if (element.isJsonObject) {
                                        element.asJsonObject.toSnapshot()?.let { 
                                            records.add(it)
                                            processedCount++
                                        }
                                    }
                                    // Yield periodically
                                    if (i % 100 == 0) {
                                        yield()
                                    }
                                }
                            } else if (entry.value.isJsonObject) {
                                // If it's a nested object (Firebase Realtime DB format with keys)
                                // Try to parse it as a snapshot
                                if (processedCount < limit) {
                                    entry.value.asJsonObject.toSnapshot()?.let { 
                                        records.add(it)
                                        processedCount++
                                    }
                                }
                            }
                        }
                        // If no array found and object has Time/Serial number fields, try to parse it as a single snapshot
                        if (!foundArray && (jsonObject.has("Time") || jsonObject.has("Serial number"))) {
                            jsonObject.toSnapshot()?.let { records.add(it) }
                        } else if (!foundArray && jsonObject.size() > 0 && processedCount < limit) {
                            // If object has child objects, parse each as a snapshot
                            jsonObject.entrySet().forEach { entry ->
                                if (processedCount >= limit) return@forEach
                                if (entry.value.isJsonObject) {
                                    entry.value.asJsonObject.toSnapshot()?.let { 
                                        records.add(it)
                                        processedCount++
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Sort by timestamp and remove duplicates (keep first occurrence)
                // Important: Sort first, then remove duplicates to ensure chronological order
                val sortedRecords = records
                    .sortedBy { it.timestamp }
                    .distinctBy { it.timestamp } // Remove exact duplicate timestamps
                    .sortedBy { it.timestamp } // Sort again after distinct to ensure order
                
                // Log for debugging
                if (sortedRecords.isNotEmpty()) {
                    println("FirebaseEnergyParser: Parsed ${records.size} records, ${sortedRecords.size} unique timestamps")
                    println("First timestamp: ${sortedRecords.first().timestamp}")
                    println("Last timestamp: ${sortedRecords.last().timestamp}")
                    
                    // Check for gaps (records should be ~6 minutes apart)
                    if (sortedRecords.size > 1) {
                        val timeDiffs = sortedRecords.zipWithNext().map { (first, second) ->
                            java.time.Duration.between(first.timestamp, second.timestamp).toMinutes()
                        }
                        val avgDiff = timeDiffs.average()
                        println("Average time difference: ${avgDiff.toInt()} minutes")
                        val largeGaps = timeDiffs.filter { it > 10 }
                        if (largeGaps.isNotEmpty()) {
                            println("Warning: Found ${largeGaps.size} gaps > 10 minutes")
                        }
                    }
                }
                
                sortedRecords
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }
    
    private fun JsonObject.toSnapshot(): EnergySnapshot? {
        return try {
            val timestamp = resolveTimestamp() ?: return null
            val status = getString("Status") ?: getString("status") ?: "Normal"
            
            val pv1Power = getDouble("ppv1") ?: 0.0
            val pv2Power = getDouble("ppv2") ?: 0.0
            val pv1Voltage = getDouble("vpv1") ?: 0.0
            val pv2Voltage = getDouble("vpv2") ?: 0.0
            
            val pCharge = getDouble("pCharge") ?: getDouble("pcharge") ?: 0.0
            val pDischarge = getDouble("pDisCharge") ?: getDouble("pdischarge") ?: getDouble("pDischarge") ?: 0.0
            val batteryVoltage = getDouble("vBat") ?: getDouble("vbat") ?: getDouble("Vbat_Inv") ?: 0.0
            val chargeRef = getDouble("chargeVoltRef") ?: getDouble("chargevoltref") ?: 55.0
            val dischargeCut = getDouble("dischgCutVolt") ?: getDouble("dischgcutvolt") ?: 42.0
            
            // Get SOC and SOH from Firebase (may be in format "29%" or just number)
            val socPercent = getPercent("SOC")
            val sohPercent = getPercent("SOH")
            
            // Get additional BMS data from Firebase
            val batteryTemp = getDouble("tBat") ?: getDouble("tbat") ?: 0.0
            val maxChgCurr = getDouble("maxChgCurr") ?: getDouble("maxchgcurr") ?: 0.0
            val maxDischgCurr = getDouble("maxDischgCurr") ?: getDouble("maxdischgcurr") ?: 0.0
            val batCurrent = getDouble("BatCurrent") ?: getDouble("batcurrent") ?: 0.0
            
            // Use SOC from Firebase if available, otherwise compute from voltage
            val batteryPercent = socPercent ?: computeBatteryPercent(batteryVoltage, chargeRef, dischargeCut)
            val batteryPower = when {
                pDischarge > 0 -> pDischarge
                pCharge > 0 -> pCharge
                else -> 0.0
            }
            
            val gridPower = getDouble("prec") ?: getDouble("pToGrid") ?: getDouble("ptogrid") ?: 0.0
            val gridVoltage = getDouble("vacr") ?: 0.0
            val gridFrequency = getDouble("genFreq") ?: getDouble("genfreq") ?: getDouble("fac") ?: getDouble("feps") ?: 0.0
            val genDryContact = when {
                gridPower > 10 -> "ON"
                else -> "OFF"
            }
            
            val loadPower = getDouble("pLoad") ?: getDouble("pload") ?: getDouble("pToUser") ?: getDouble("ptouser") ?: 0.0
            
            val solarSection = SolarSection(
                pv1 = if (pv1Power > 0.0 || pv1Voltage > 0.0) {
                    SolarStringStatus("PV1", pv1Power.roundToInt(), pv1Voltage)
                } else null,
                pv2 = if (pv2Power > 0.0 || pv2Voltage > 0.0) {
                    SolarStringStatus("PV2", pv2Power.roundToInt(), pv2Voltage)
                } else null
            )
            
            val batterySection = BatterySection(
                powerW = batteryPower.roundToInt(),
                percentage = batteryPercent,
                voltageVdc = batteryVoltage,
                type = "Lead-acid",
                capacityAh = 1300,
                socPercent = socPercent,
                sohPercent = sohPercent,
                temperatureC = if (batteryTemp > 0) batteryTemp else null,
                maxChargeCurrentA = if (maxChgCurr > 0) maxChgCurr else null,
                maxDischargeCurrentA = if (maxDischgCurr > 0) maxDischgCurr else null,
                currentA = if (batCurrent != 0.0) batCurrent else null
            )
            
            val gridSection = GridSection(
                powerW = gridPower.roundToInt(),
                voltageVac = gridVoltage,
                frequencyHz = gridFrequency,
                genDryContact = genDryContact
            )
            
            val loadSection = LoadSection(
                consumptionPowerW = loadPower.roundToInt()
            )
            
            val epsSection = EpsSection(
                mode = if (status.contains("stand", ignoreCase = true)) "StandBy" else "Online",
                description = "Backup Power (EPS)"
            )
            
            val controls = ControlSection(startQuickChargeEnabled = true)
            
            val flow = EnergyFlowSection(
                fromPvToInverter = (pv1Power + pv2Power) > 10,
                fromBatteryToInverter = pDischarge > 10,
                fromInverterToLoad = loadPower > 10,
                fromGridToInverter = gridPower > 10
            )
            
            EnergySnapshot(
                timestamp = timestamp,
                status = status,
                solar = solarSection,
                battery = batterySection,
                grid = gridSection,
                load = loadSection,
                eps = epsSection,
                controls = controls,
                energyFlow = flow
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    private fun JsonObject.resolveTimestamp(): LocalDateTime? {
        val timeValue = getString("Time") ?: getString("time") ?: getString("Serial number")
        return timeValue?.let { parseTimestamp(it) }
    }
    
    private fun computeBatteryPercent(
        voltage: Double,
        chargeRef: Double,
        dischargeCut: Double
    ): Int {
        if (chargeRef <= dischargeCut) return 0
        val percent = ((voltage - dischargeCut) / (chargeRef - dischargeCut)) * 100
        return percent.roundToInt().coerceIn(0, 100)
    }
    
    private fun JsonObject.getDouble(key: String): Double? {
        return try {
            if (!has(key)) return null
            val element = get(key)
            when {
                element.isJsonNull -> null
                element.isJsonPrimitive -> {
                    val primitive = element.asJsonPrimitive
                    when {
                        primitive.isNumber -> primitive.asDouble
                        primitive.isString -> primitive.asString.toDoubleOrNull()
                        else -> null
                    }
                }
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    private fun JsonObject.getString(key: String): String? {
        return try {
            if (!has(key)) return null
            val element = get(key)
            when {
                element.isJsonNull -> null
                element.isJsonPrimitive -> element.asJsonPrimitive.asString
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    private fun JsonObject.getPercent(key: String): Int? {
        return try {
            val value = getString(key) ?: return null
            // Extract number from string like "29%" or "29"
            val numberStr = value.replace("%", "").trim()
            numberStr.toIntOrNull()
        } catch (e: Exception) {
            null
        }
    }
    
    private fun parseTimestamp(text: String?): LocalDateTime? {
        val value = text?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        
        // Try each format
        for (formatter in timestampFormats) {
            try {
                return LocalDateTime.parse(value, formatter)
            } catch (e: Exception) {
                // Try next format
            }
        }
        
        // If all formats fail, log for debugging
        println("FirebaseEnergyParser: Failed to parse timestamp: '$value'")
        return null
    }
}

