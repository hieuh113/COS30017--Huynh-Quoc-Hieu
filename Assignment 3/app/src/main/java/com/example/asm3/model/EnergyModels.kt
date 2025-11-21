package com.example.asm3.model

import androidx.compose.runtime.Immutable
import java.time.LocalDateTime

@Immutable
data class SolarStringStatus(
    val name: String,
    val powerW: Int,
    val voltageV: Double
)

@Immutable
data class SolarSection(
    val pv1: SolarStringStatus?,
    val pv2: SolarStringStatus?
)

@Immutable
data class BatterySection(
    val powerW: Int,
    val percentage: Int,
    val voltageVdc: Double,
    val type: String,
    val capacityAh: Int,
    val socPercent: Int? = null, // State of Charge from Firebase
    val sohPercent: Int? = null, // State of Health from Firebase
    val temperatureC: Double? = null, // Battery temperature from Firebase
    val maxChargeCurrentA: Double? = null, // Max charge current from Firebase
    val maxDischargeCurrentA: Double? = null, // Max discharge current from Firebase
    val currentA: Double? = null // Battery current from Firebase
)

@Immutable
data class GridSection(
    val powerW: Int,
    val voltageVac: Double,
    val frequencyHz: Double,
    val genDryContact: String
)

@Immutable
data class LoadSection(
    val consumptionPowerW: Int
)

@Immutable
data class EpsSection(
    val mode: String,
    val description: String
)

@Immutable
data class ControlSection(
    val startQuickChargeEnabled: Boolean
)

@Immutable
data class EnergyFlowSection(
    val fromPvToInverter: Boolean,
    val fromBatteryToInverter: Boolean,
    val fromInverterToLoad: Boolean,
    val fromGridToInverter: Boolean
)

@Immutable
data class EnergySnapshot(
    val timestamp: LocalDateTime,
    val status: String,
    val solar: SolarSection,
    val battery: BatterySection,
    val grid: GridSection,
    val load: LoadSection,
    val eps: EpsSection,
    val controls: ControlSection,
    val energyFlow: EnergyFlowSection
)

@Immutable
data class DashboardUiState(
    val isLoading: Boolean = true,
    val currentSnapshot: EnergySnapshot? = null,
    val availableSlots: List<LocalDateTime> = emptyList(),
    val selectedStart: LocalDateTime? = null,
    val selectedEnd: LocalDateTime? = null,
    val errorMessage: String? = null
)

enum class StatsRange { Day, Week, Month }

@Immutable
data class ProductionPoint(
    val hourLabel: String,
    val productionKWh: Float,
    val consumptionKWh: Float
)

@Immutable
data class BatteryStoragePoint(
    val hourLabel: String,
    val levelPercent: Float
)

@Immutable
data class StatSummary(
    val totalProductionKWh: Double,
    val productionGrowthPercent: Double,
    val savingsUsd: Double,
    val savingsGrowthPercent: Double
)

@Immutable
data class StatsState(
    val summary: StatSummary,
    val productionSeries: Map<StatsRange, List<ProductionPoint>>,
    val batterySeries: Map<StatsRange, List<BatteryStoragePoint>>,
    val selectedRange: StatsRange,
    val gridImportKWh: Map<StatsRange, Double>
)

@Immutable
data class CellStatus(
    val index: Int,
    val voltage: Double,
    val temperatureC: Double,
    val hasWarning: Boolean
)

@Immutable
data class BmsState(
    val socPercent: Int,
    val sohPercent: Int,
    val voltageV: Double,
    val currentA: Double,
    val temperatureC: Double,
    val cells: List<CellStatus>,
    val maxChargeCurrentA: Float,
    val maxDischargeCurrentA: Float,
    val cutOffVoltageV: Float
)

@Immutable
data class SettingsState(
    val notificationsEnabled: Boolean,
    val batteryAlertsEnabled: Boolean,
    val darkMode: Boolean,
    val language: String,
    val updateFrequencySeconds: Int,
    val autoExportReports: Boolean,
    val excelFileName: String?,
    val excelFileUri: String?,
    val dataPreview: String
)

@Immutable
data class EnergyUiState(
    val dashboard: DashboardUiState = DashboardUiState(),
    val stats: StatsState,
    val bms: BmsState,
    val settings: SettingsState
)

