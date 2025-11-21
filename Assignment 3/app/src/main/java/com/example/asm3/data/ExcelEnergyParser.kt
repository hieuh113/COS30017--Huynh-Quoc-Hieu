package com.example.asm3.data

import android.content.Context
import android.net.Uri
import com.example.asm3.model.BatterySection
import com.example.asm3.model.ControlSection
import com.example.asm3.model.EnergyFlowSection
import com.example.asm3.model.EnergySnapshot
import com.example.asm3.model.EpsSection
import com.example.asm3.model.GridSection
import com.example.asm3.model.LoadSection
import com.example.asm3.model.SolarSection
import com.example.asm3.model.SolarStringStatus
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.DateUtil
import org.apache.poi.ss.usermodel.Row
import java.io.InputStream
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

class ExcelEnergyParser(
    private val context: Context
) {

    private val timestampFormats = listOf(
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
        DateTimeFormatter.ofPattern("yyyy/M/d HH:mm:ss"),
        DateTimeFormatter.ISO_LOCAL_DATE_TIME
    )

    fun loadSnapshots(uriString: String?): List<EnergySnapshot> {
        return try {
            val inputStream = openInputStream(uriString) ?: return emptyList()
            try {
                inputStream.use { stream ->
                    try {
                        HSSFWorkbook(stream).use { workbook ->
                            if (workbook.numberOfSheets == 0) return emptyList()
                            val sheet = workbook.getSheetAt(0)
                            if (sheet.physicalNumberOfRows == 0) return emptyList()
                            val headerRow = sheet.getRow(sheet.firstRowNum) ?: return emptyList()
                            val headerMap = mutableMapOf<String, Int>()
                            var cellIndex = headerRow.firstCellNum.toInt()
                            val lastIndex = headerRow.lastCellNum.toInt()
                            while (cellIndex < lastIndex) {
                                try {
                                    val cell = headerRow.getCell(cellIndex)
                                    if (cell != null) {
                                        val key = when (cell.cellType) {
                                            CellType.STRING -> cell.stringCellValue
                                            CellType.NUMERIC -> cell.numericCellValue.toString()
                                            else -> null
                                        }?.trim()?.lowercase()
                                        if (!key.isNullOrEmpty()) {
                                            headerMap[key] = cell.columnIndex
                                        }
                                    }
                                } catch (e: Exception) {
                                    // Skip invalid cell
                                }
                                cellIndex++
                            }

                            val records = mutableListOf<EnergySnapshot>()
                            for (rowIndex in sheet.firstRowNum + 1..sheet.lastRowNum) {
                                val row = sheet.getRow(rowIndex) ?: continue
                                try {
                                    row.toSnapshot(headerMap)?.let { records.add(it) }
                                } catch (e: Exception) {
                                    // Skip invalid rows
                                }
                            }
                            records.sortedBy { it.timestamp }
                        }
                    } catch (e: Exception) {
                        emptyList()
                    }
                }
            } catch (e: Exception) {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun openInputStream(uriString: String?): InputStream? {
        return if (uriString.isNullOrBlank()) {
            runCatching { context.assets.open("default_mock_data.xls") }.getOrNull()
        } else {
            val uri = Uri.parse(uriString)
            runCatching { context.contentResolver.openInputStream(uri) }.getOrNull()
        }
    }

    private fun Row.toSnapshot(headerMap: Map<String, Int>): EnergySnapshot? {
        return runCatching {
            val timestamp = resolveTimestamp(headerMap) ?: return null
            val status = getString(headerMap["status"]) ?: "Normal"

            val pv1Power = getDouble(headerMap["ppv1"]) ?: 0.0
            val pv2Power = getDouble(headerMap["ppv2"]) ?: 0.0
            val pv1Voltage = getDouble(headerMap["vpv1"]) ?: 0.0
            val pv2Voltage = getDouble(headerMap["vpv2"]) ?: 0.0

            val pCharge = getDouble(headerMap["pcharge"]) ?: 0.0
            val pDischarge = getDouble(headerMap["pdischarge"]) ?: 0.0
            val batteryVoltage = getDouble(headerMap["vbat"]) ?: 0.0
            val chargeRef = getDouble(headerMap["chargevoltref"]) ?: 55.0
            val dischargeCut = getDouble(headerMap["dischgcutvolt"]) ?: 42.0
            val batteryPercent = computeBatteryPercent(batteryVoltage, chargeRef, dischargeCut)
            val batteryPower = when {
                pDischarge > 0 -> pDischarge
                pCharge > 0 -> pCharge
                else -> 0.0
            }

            val gridPower = getDouble(headerMap["prec"]) ?: getDouble(headerMap["ptogrid"]) ?: 0.0
            val gridVoltage = getDouble(headerMap["vacr"]) ?: 0.0
            val gridFrequency = getDouble(headerMap["genfreq"]) ?: getDouble(headerMap["feps"]) ?: 0.0
            val genDryContact = when {
                gridPower > 10 -> "ON"
                else -> "OFF"
            }

            val loadPower = getDouble(headerMap["pload"]) ?: getDouble(headerMap["ptouser"]) ?: 0.0

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
                capacityAh = 1300
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
        }.getOrNull()
    }

    private fun Row.resolveTimestamp(headerMap: Map<String, Int>): LocalDateTime? {
        val serialIndex = headerMap["serial number"] ?: headerMap["date"]
        val timeIndex = headerMap["time"]

        val directDateTime = getLocalDateTime(serialIndex)
            ?: getLocalDateTime(timeIndex)
            ?: run {
                val dateText = getString(serialIndex)
                val timeText = getString(timeIndex)
                val combined = when {
                    dateText != null && timeText != null -> "${dateText.trim()} ${timeText.trim()}"
                    dateText != null -> dateText
                    timeText != null -> timeText
                    else -> null
                }
                combined?.let { parseTimestamp(it) }
            }

        if (directDateTime != null) return directDateTime

        val date = getLocalDate(serialIndex) ?: return null
        val time = getLocalTime(timeIndex) ?: LocalTime.MIDNIGHT
        return LocalDateTime.of(date, time)
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

    private fun Row.getDouble(index: Int?): Double? {
        return runCatching {
            val cell = index?.let { getCell(it) } ?: return null
            when (cell.cellType) {
                CellType.NUMERIC -> cell.numericCellValue
                CellType.STRING -> cell.stringCellValue.toDoubleOrNull()
                CellType.FORMULA -> when (cell.cachedFormulaResultType) {
                    CellType.NUMERIC -> cell.numericCellValue
                    CellType.STRING -> cell.stringCellValue.toDoubleOrNull()
                    else -> null
                }
                else -> null
            }
        }.getOrNull()
    }

    private fun Row.getString(index: Int?): String? {
        return runCatching {
            val cell = index?.let { getCell(it) } ?: return null
            when (cell.cellType) {
                CellType.STRING -> cell.stringCellValue
                CellType.NUMERIC -> {
                    if (DateUtil.isCellDateFormatted(cell)) {
                        val date = cell.dateCellValue ?: return null
                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                            .format(LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault()))
                    } else {
                        cell.numericCellValue.toString()
                    }
                }
                else -> null
            }
        }.getOrNull()
    }

    private fun Row.getLocalDate(index: Int?): LocalDate? {
        val cell = index?.let { getCell(it) } ?: return null
        return when (cell.cellType) {
            CellType.NUMERIC -> if (DateUtil.isCellDateFormatted(cell)) {
                cell.dateCellValue?.toInstant()?.let {
                    LocalDateTime.ofInstant(it, ZoneId.systemDefault()).toLocalDate()
                }
            } else null

            CellType.STRING -> runCatching {
                LocalDate.parse(cell.stringCellValue.trim())
            }.getOrNull()

            else -> null
        }
    }

    private fun Row.getLocalTime(index: Int?): LocalTime? {
        val cell = index?.let { getCell(it) } ?: return null
        return when (cell.cellType) {
            CellType.NUMERIC -> if (DateUtil.isCellDateFormatted(cell)) {
                cell.dateCellValue?.toInstant()?.let {
                    LocalDateTime.ofInstant(it, ZoneId.systemDefault()).toLocalTime()
                }
            } else {
                val seconds = ((cell.numericCellValue * 24 * 3600).roundToInt()).mod(24 * 3600)
                LocalTime.MIDNIGHT.plusSeconds(seconds.toLong())
            }

            CellType.STRING -> runCatching {
                LocalTime.parse(cell.stringCellValue.trim())
            }.getOrNull()

            else -> null
        }
    }

    private fun Row.getLocalDateTime(index: Int?): LocalDateTime? {
        val cell = index?.let { getCell(it) } ?: return null
        return when (cell.cellType) {
            CellType.NUMERIC -> if (DateUtil.isCellDateFormatted(cell)) {
                cell.dateCellValue?.toInstant()?.let {
                    LocalDateTime.ofInstant(it, ZoneId.systemDefault())
                }
            } else null

            CellType.STRING -> parseTimestamp(cell.stringCellValue)

            else -> null
        }
    }

    private fun parseTimestamp(text: String?): LocalDateTime? {
        val value = text?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        for (formatter in timestampFormats) {
            runCatching { LocalDateTime.parse(value, formatter) }
                .onSuccess { return it }
        }
        return runCatching {
            val instant = Instant.parse(value)
            LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
        }.getOrNull()
    }
}

