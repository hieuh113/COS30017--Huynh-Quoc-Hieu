package com.example.asm3.data

import com.example.asm3.model.BatterySection
import com.example.asm3.model.BatteryStoragePoint
import com.example.asm3.model.BmsState
import com.example.asm3.model.CellStatus
import com.example.asm3.model.ControlSection
import com.example.asm3.model.EnergyFlowSection
import com.example.asm3.model.EnergySnapshot
import com.example.asm3.model.EpsSection
import com.example.asm3.model.GridSection
import com.example.asm3.model.LoadSection
import com.example.asm3.model.SettingsState
import com.example.asm3.model.SolarSection
import com.example.asm3.model.SolarStringStatus
import com.example.asm3.model.StatSummary
import com.example.asm3.model.StatsRange
import com.example.asm3.model.StatsState
import com.example.asm3.model.ProductionPoint
import java.time.LocalDateTime
import kotlin.random.Random

class MockEnergyRepository {

    fun createStatsState(): StatsState {
        val productionDay = mutableListOf<ProductionPoint>()
        val batteryDay = mutableListOf<BatteryStoragePoint>()
        val hours = listOf("00:00","04:00","08:00","12:00","16:00","20:00")
        var batteryLevel = 65f
        hours.forEachIndexed { index, hour ->
            val prod = if (index < 2) 0f else (index * 1.2f)
            val cons = prod * 0.8f + index * 0.5f
            productionDay.add(
                ProductionPoint(hour, prod, cons)
            )
            batteryDay.add(
                BatteryStoragePoint(hour, batteryLevel)
            )
            batteryLevel += Random.nextFloat() * 2 - 1
        }

        val productionWeek = (1..7).map {
            ProductionPoint("D$it", 12f + it, 10f + it / 2f)
        }
        val productionMonth = (1..4).map {
            ProductionPoint("W$it", 80f + it * 5, 60f + it * 3)
        }

        val batteryWeek = (1..7).map {
            BatteryStoragePoint("D$it", 60f + Random.nextFloat() * 20)
        }
        val batteryMonth = (1..4).map {
            BatteryStoragePoint("W$it", 70f + Random.nextFloat() * 10)
        }

        val summary = StatSummary(
            totalProductionKWh = 4520.0,
            productionGrowthPercent = 15.2,
            savingsUsd = 1580.0,
            savingsGrowthPercent = 8.4
        )

        val gridImport = mapOf(
            StatsRange.Day to 6.4,
            StatsRange.Week to 42.0,
            StatsRange.Month to 165.0
        )

        return StatsState(
            summary = summary,
            productionSeries = mapOf(
                StatsRange.Day to productionDay,
                StatsRange.Week to productionWeek,
                StatsRange.Month to productionMonth
            ),
            batterySeries = mapOf(
                StatsRange.Day to batteryDay,
                StatsRange.Week to batteryWeek,
                StatsRange.Month to batteryMonth
            ),
            selectedRange = StatsRange.Day,
            gridImportKWh = gridImport
        )
    }

    fun createBmsState(): BmsState {
        val cells = (1..16).map { index ->
            val voltage = 3.4 + Random.nextDouble(from = -0.03, until = 0.04)
            val temperature = 27 + Random.nextDouble(from = -1.5, until = 1.8)
            CellStatus(
                index = index,
                voltage = (voltage * 100).toInt() / 100.0,
                temperatureC = (temperature * 10).toInt() / 10.0,
                hasWarning = index == 4
            )
        }

        return BmsState(
            socPercent = 75,
            sohPercent = 98,
            voltageV = 51.2,
            currentA = 12.5,
            temperatureC = 28.0,
            cells = cells,
            maxChargeCurrentA = 50f,
            maxDischargeCurrentA = 60f,
            cutOffVoltageV = 44f
        )
    }

    fun createSettingsState(): SettingsState {
        return SettingsState(
            notificationsEnabled = true,
            batteryAlertsEnabled = true,
            darkMode = false,
            language = "English",
            updateFrequencySeconds = 5,
            autoExportReports = false,
            excelFileName = "Firebase Realtime Database",
            excelFileUri = null,
            dataPreview = "https://solar-energy-6452d-default-rtdb.firebaseio.com/"
        )
    }

    fun generateMockSnapshots(): List<EnergySnapshot> {
        val baseTime = LocalDateTime.now().minusDays(1).withHour(0).withMinute(0).withSecond(0)
        val snapshots = mutableListOf<EnergySnapshot>()
        
        // Generate 24 hours of mock data
        for (hour in 0..23) {
            val timestamp = baseTime.plusHours(hour.toLong()).plusMinutes(Random.nextInt(0, 60).toLong())
            val pv1Power = if (hour in 6..18) Random.nextInt(50, 200) else Random.nextInt(0, 20)
            val pv2Power = if (hour in 6..18) Random.nextInt(40, 180) else Random.nextInt(0, 15)
            val pv1Voltage = 350.0 + Random.nextDouble(-20.0, 30.0)
            val pv2Voltage = 350.0 + Random.nextDouble(-25.0, 28.0)
            
            val batteryVoltage = 44.0 + Random.nextDouble(-2.0, 6.0)
            val batteryPercent = ((batteryVoltage - 42.0) / (55.0 - 42.0) * 100).toInt().coerceIn(0, 100)
            val batteryPower = if (hour in 10..16) Random.nextInt(100, 1200) else Random.nextInt(0, 500)
            
            val loadPower = Random.nextInt(800, 1500)
            val gridPower = if (loadPower > pv1Power + pv2Power + batteryPower) {
                loadPower - pv1Power - pv2Power - batteryPower
            } else 0
            
            snapshots.add(
                EnergySnapshot(
                    timestamp = timestamp,
                    status = "Normal",
                    solar = SolarSection(
                        pv1 = SolarStringStatus("PV1", pv1Power, pv1Voltage),
                        pv2 = SolarStringStatus("PV2", pv2Power, pv2Voltage)
                    ),
                    battery = BatterySection(
                        powerW = batteryPower,
                        percentage = batteryPercent,
                        voltageVdc = batteryVoltage,
                        type = "Lead-acid",
                        capacityAh = 1300
                    ),
                    grid = GridSection(
                        powerW = gridPower.toInt(),
                        voltageVac = 227.0 + Random.nextDouble(-5.0, 5.0),
                        frequencyHz = 49.9 + Random.nextDouble(-0.1, 0.1),
                        genDryContact = if (gridPower > 10) "ON" else "OFF"
                    ),
                    load = LoadSection(consumptionPowerW = loadPower),
                    eps = EpsSection(mode = "StandBy", description = "Backup Power (EPS)"),
                    controls = ControlSection(startQuickChargeEnabled = true),
                    energyFlow = EnergyFlowSection(
                        fromPvToInverter = (pv1Power + pv2Power) > 10,
                        fromBatteryToInverter = batteryPower > 100,
                        fromInverterToLoad = loadPower > 10,
                        fromGridToInverter = gridPower > 10
                    )
                )
            )
        }
        return snapshots.sortedBy { it.timestamp }
    }
}

