package com.example.asm3

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.ElectricalServices
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Power
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.asm3.model.BatteryStoragePoint
import com.example.asm3.model.BmsState
import com.example.asm3.model.CellStatus
import com.example.asm3.model.DashboardUiState
import com.example.asm3.model.EnergySnapshot
import com.example.asm3.model.SettingsState
import com.example.asm3.model.SolarSection
import com.example.asm3.model.SolarStringStatus
import com.example.asm3.model.StatSummary
import com.example.asm3.model.StatsRange
import com.example.asm3.model.StatsState
import com.example.asm3.model.ProductionPoint
import com.example.asm3.ui.theme.SolarBlue
import com.example.asm3.ui.theme.SolarGreen
import com.example.asm3.ui.theme.SolarOrange
import com.example.asm3.ui.theme.SolarRed
import com.example.asm3.ui.theme.SolarYellow
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.max
import kotlin.math.min

@Composable
fun EnergyFlowScreen(
    state: DashboardUiState,
    onSelectStart: (LocalDateTime) -> Unit,
    onSelectEnd: (LocalDateTime) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snapshot = state.currentSnapshot
    val timestampFormatter = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss") }
    val hourFormatter = remember { DateTimeFormatter.ofPattern("dd MMM HH:mm") }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            RangePickerCard(
                slots = state.availableSlots,
                selectedStart = state.selectedStart,
                selectedEnd = state.selectedEnd,
                hourFormatter = hourFormatter,
                onSelectStart = onSelectStart,
                onSelectEnd = onSelectEnd
            )
        }
        when {
            state.isLoading -> {
                item { LoadingCard() }
            }

            snapshot == null -> {
                item { EmptyStateCard() }
            }

            else -> {
                item {
                    SystemInfoCard(
                        snapshot = snapshot,
                        formattedTime = timestampFormatter.format(snapshot.timestamp),
                        onRefresh = onRefresh
                    )
                }
                item { FlowDiagramCard(snapshot) }
                item { QuickChargeCard(snapshot) }
            }
        }
        state.errorMessage?.let { message ->
            item { ErrorMessage(message) }
        }
    }
}

@Composable
private fun RangePickerCard(
    slots: List<LocalDateTime>,
    selectedStart: LocalDateTime?,
    selectedEnd: LocalDateTime?,
    hourFormatter: DateTimeFormatter,
    onSelectStart: (LocalDateTime) -> Unit,
    onSelectEnd: (LocalDateTime) -> Unit
) {
    Card {
        Column(Modifier.padding(16.dp)) {
            Text("Select Time Range", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            if (slots.isEmpty()) {
                Text("No time data available.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                RangeDropdownRow(
                    label = "From",
                    value = selectedStart,
                    slots = slots,
                    formatter = hourFormatter,
                    onSelect = onSelectStart
                )
                Spacer(Modifier.height(12.dp))
                RangeDropdownRow(
                    label = "To",
                    value = selectedEnd,
                    slots = slots,
                    formatter = hourFormatter,
                    onSelect = onSelectEnd
                )
            }
        }
    }
}

@Composable
private fun RangeDropdownRow(
    label: String,
    value: LocalDateTime?,
    slots: List<LocalDateTime>,
    formatter: DateTimeFormatter,
    onSelect: (LocalDateTime) -> Unit
) {
    val sortedSlots = remember(slots) { slots.sorted() }
    var expanded by remember { mutableStateOf(false) }
    
    Column {
        Text(label, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { 
                        if (sortedSlots.isNotEmpty()) {
                            expanded = true
                        }
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = value?.let { formatter.format(it) } ?: "Not selected",
                        modifier = Modifier.weight(1f),
                        color = if (value == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                    )
                    Icon(
                        Icons.Outlined.ArrowDropDown,
                        contentDescription = "Select time",
                        tint = if (sortedSlots.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            
            DropdownMenu(
                expanded = expanded && sortedSlots.isNotEmpty(),
                onDismissRequest = { expanded = false },
                modifier = Modifier.fillMaxWidth(0.95f)
            ) {
                sortedSlots.forEach { slot ->
                    DropdownMenuItem(
                        text = { 
                            Text(
                                formatter.format(slot),
                                modifier = Modifier.fillMaxWidth()
                            ) 
                        },
                        onClick = {
                            onSelect(slot)
                            expanded = false
                        }
                    )
                }
            }
        }
        
        if (sortedSlots.isEmpty()) {
            Spacer(Modifier.height(4.dp))
            Text(
                "No time data available",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun LoadingCard() {
    Card {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}

@Composable
private fun EmptyStateCard() {
    Card {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("No data to display", fontWeight = FontWeight.SemiBold)
            Text(
                "Please select a valid Excel file in the settings section.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun SystemInfoCard(
    snapshot: EnergySnapshot,
    formattedTime: String,
    onRefresh: () -> Unit
) {
    Card {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("System Information", style = MaterialTheme.typography.titleMedium)
                    Text(formattedTime, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                StatusChip(snapshot.status)
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Outlined.Refresh, contentDescription = "Refresh")
                }
            }
            Spacer(Modifier.height(12.dp))
            SolarSummary(snapshot.solar)
        }
    }
}

@Composable
private fun StatusChip(status: String) {
    Surface(
        color = SolarGreen.copy(alpha = 0.15f),
        shape = RoundedCornerShape(20.dp)
    ) {
        Text(
            text = status,
            color = SolarGreen,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun SolarSummary(solar: SolarSection) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        SolarSummaryItem("PV1", solar.pv1)
        SolarSummaryItem("PV2", solar.pv2)
    }
}

@Composable
private fun SolarSummaryItem(label: String, data: SolarStringStatus?) {
    Column {
        Text(label, fontWeight = FontWeight.SemiBold)
        Text(
            text = data?.let { "${it.powerW} W" } ?: "--",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = data?.let { "${it.voltageV.toInt()} V" } ?: "--",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun FlowDiagramCard(snapshot: EnergySnapshot) {
    Card {
        Column(Modifier.padding(16.dp)) {
            Text("Energy Flow", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(16.dp))
            EnergyDiagram(snapshot)
            Spacer(Modifier.height(16.dp))
            BatteryDetails(snapshot)
            Spacer(Modifier.height(8.dp))
            GridDetails(snapshot)
        }
    }
}

@Composable
private fun EnergyDiagram(snapshot: EnergySnapshot) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Top: Solar
        FlowNode(
            icon = Icons.Outlined.WbSunny,
            label = "Solar",
            primary = "${(snapshot.solar.pv1?.powerW ?: 0) + (snapshot.solar.pv2?.powerW ?: 0)} W",
            secondary = listOfNotNull(
                snapshot.solar.pv1?.voltageV?.let { "PV1: ${it.toInt()} V" },
                snapshot.solar.pv2?.voltageV?.let { "PV2: ${it.toInt()} V" }
            ).joinToString(" • ")
        )
        FlowArrowVertical(active = snapshot.energyFlow.fromPvToInverter)
        
        // Middle: Battery, Inverter, Grid in a row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Battery on the left
            FlowNode(
                icon = Icons.Outlined.BatteryChargingFull,
                label = "Battery",
                primary = "${snapshot.battery.powerW} W",
                secondary = "${snapshot.battery.voltageVdc.toInt()} V • ${snapshot.battery.percentage}%"
            )
            
            // Arrow from Battery to Inverter
            FlowArrowHorizontal(
                forward = true,
                active = snapshot.energyFlow.fromBatteryToInverter
            )
            
            // Inverter in the center
            FlowNode(
                icon = Icons.Outlined.Power,
                label = "Inverter",
                primary = snapshot.eps.mode,
                secondary = snapshot.status
            )
            
            // Arrow from Grid to Inverter (left-pointing)
            FlowArrowHorizontal(
                forward = false,
                active = snapshot.energyFlow.fromGridToInverter
            )
            
            // Grid on the right
            FlowNode(
                icon = Icons.Outlined.ElectricalServices,
                label = "Grid",
                primary = "${snapshot.grid.powerW} W",
                secondary = "${snapshot.grid.voltageVac.toInt()} V"
            )
        }
        
        // Bottom: Load
        FlowArrowVertical(active = snapshot.energyFlow.fromInverterToLoad)
        FlowNode(
            icon = Icons.Outlined.Home,
            label = "Consumption",
            primary = "${snapshot.load.consumptionPowerW} W",
            secondary = "Load Power"
        )
    }
}

@Composable
private fun FlowArrowVertical(active: Boolean) {
    val color = if (active) SolarYellow else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(vertical = 4.dp)) {
        repeat(3) {
            Icon(
                imageVector = Icons.Outlined.ArrowDownward,
                contentDescription = null,
                tint = color
            )
        }
    }
}

@Composable
private fun FlowArrowHorizontal(forward: Boolean, active: Boolean) {
    val color = if (active) SolarYellow else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
    Row(
        modifier = Modifier.width(56.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) {
            Icon(
                imageVector = if (forward) Icons.AutoMirrored.Outlined.ArrowForward else Icons.Outlined.ArrowBack,
                contentDescription = null,
                tint = color
            )
        }
    }
}

@Composable
private fun FlowNode(
    icon: ImageVector,
    label: String,
    primary: String,
    secondary: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(4.dp))
        Text(label, fontWeight = FontWeight.SemiBold)
        Text(primary, fontSize = 14.sp)
        Text(secondary, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

@Composable
private fun BatteryDetails(snapshot: EnergySnapshot) {
    Column {
        Text("Lead-acid battery: ${snapshot.battery.capacityAh} Ah", fontWeight = FontWeight.SemiBold)
        Text(
            "${snapshot.battery.percentage}% • ${snapshot.battery.voltageVdc.toInt()} Vdc",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun GridDetails(snapshot: EnergySnapshot) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Power", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        Text(
                    "${snapshot.grid.powerW} W",
                    fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Voltage", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Text(
                    "${snapshot.grid.voltageVac.toInt()} V",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Frequency", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Text(
                    "${snapshot.grid.frequencyHz.toInt()} Hz",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun QuickChargeCard(snapshot: EnergySnapshot) {
    Card {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.BatteryChargingFull, contentDescription = null, tint = SolarGreen)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(snapshot.eps.mode, fontWeight = FontWeight.SemiBold)
                    Text(snapshot.eps.description, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(16.dp))
            Button(
                enabled = snapshot.controls.startQuickChargeEnabled,
                onClick = { /* trigger quick charge */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Start Quick Charge")
            }
        }
    }
}

@Composable
private fun ErrorMessage(message: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SolarRed.copy(alpha = 0.1f))
    ) {
        Text(
            text = message,
            color = SolarRed,
            modifier = Modifier.padding(16.dp),
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun StatsScreen(
    state: StatsState,
    onRangeSelected: (StatsRange) -> Unit,
    modifier: Modifier = Modifier
) {
    val productionPoints = state.productionSeries[state.selectedRange].orEmpty()
    val batteryPoints = state.batterySeries[state.selectedRange].orEmpty()
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            SummaryRow(state.summary)
        }
        item {
            RangeSelector(
                selectedRange = state.selectedRange,
                onRangeSelected = onRangeSelected
            )
        }
        item {
            ChartCard(title = "Production & Consumption") {
                ProductionChart(points = productionPoints)
            }
        }
        item {
            ChartCard(title = "Battery Capacity") {
                BatteryChart(points = batteryPoints)
            }
        }
        item {
            GridImportCard(value = state.gridImportKWh[state.selectedRange] ?: 0.0)
        }
    }
}

@Composable
private fun SummaryRow(summary: StatSummary) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        SummaryCard(
            label = "Total Production",
            value = "${summary.totalProductionKWh.toInt()} kWh",
            growth = summary.productionGrowthPercent,
            modifier = Modifier.weight(1f)
        )
        SummaryCard(
            label = "Savings",
            value = "$${summary.savingsUsd.toInt()}",
            growth = summary.savingsGrowthPercent,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SummaryCard(
    label: String,
    value: String,
    growth: Double,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("+${growth.toInt()}%", color = SolarGreen, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun RangeSelector(
    selectedRange: StatsRange,
    onRangeSelected: (StatsRange) -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(30.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        StatsRange.values().forEach { range ->
            val selected = range == selectedRange
            TextButton(
                onClick = { onRangeSelected(range) },
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(26.dp))
                    .background(
                        if (selected) MaterialTheme.colorScheme.background else Color.Transparent
                    )
            ) {
                Text(
                    text = when (range) {
                        StatsRange.Day -> "Day"
                        StatsRange.Week -> "Week"
                        StatsRange.Month -> "Month"
                    },
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ChartCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
private fun ProductionChart(points: List<ProductionPoint>) {
    val maxValue = max(
        1f,
        points.maxOfOrNull { max(it.productionKWh, it.consumptionKWh) } ?: 1f
    )
    Canvas(modifier = Modifier.fillMaxSize()) {
        if (points.isEmpty()) return@Canvas
        val stepX = size.width / (points.size - 1).coerceAtLeast(1)
        val prodPath = Path().apply {
            moveTo(0f, size.height)
            points.forEachIndexed { index, point ->
                val x = index * stepX
                val y = size.height - (point.productionKWh / maxValue) * size.height
                lineTo(x, y)
            }
            lineTo(size.width, size.height)
            close()
        }
        val consPath = Path().apply {
            moveTo(0f, size.height)
            points.forEachIndexed { index, point ->
                val x = index * stepX
                val y = size.height - (point.consumptionKWh / maxValue) * size.height
                lineTo(x, y)
            }
            lineTo(size.width, size.height)
            close()
        }
        drawPath(prodPath, color = SolarGreen.copy(alpha = 0.35f))
        drawPath(consPath, color = SolarBlue.copy(alpha = 0.35f))
        points.forEachIndexed { index, point ->
            val x = index * stepX
            val yProd = size.height - (point.productionKWh / maxValue) * size.height
            val yCons = size.height - (point.consumptionKWh / maxValue) * size.height
            drawCircle(SolarGreen, radius = 6f, center = Offset(x, yProd))
            drawCircle(SolarBlue, radius = 6f, center = Offset(x, yCons))
        }
    }
}

@Composable
private fun BatteryChart(points: List<BatteryStoragePoint>) {
    val maxValue = points.maxOfOrNull { it.levelPercent } ?: 100f
    val minValue = points.minOfOrNull { it.levelPercent } ?: 0f
    Canvas(modifier = Modifier.fillMaxSize()) {
        if (points.isEmpty()) return@Canvas
        val stepX = size.width / (points.size - 1).coerceAtLeast(1)
        val path = Path().apply {
            points.forEachIndexed { index, point ->
                val x = index * stepX
                val normalized = (point.levelPercent - minValue) / (maxValue - minValue)
                val y = size.height - normalized * size.height
                if (index == 0) moveTo(x, y) else lineTo(x, y)
            }
        }
        drawPath(
            path = path,
            color = SolarOrange,
            style = Stroke(width = 6f, cap = StrokeCap.Round)
        )
    }
}

@Composable
private fun GridImportCard(value: Double) {
    Card {
        Column(Modifier.padding(16.dp)) {
            Text("Grid Import", style = MaterialTheme.typography.titleMedium)
            Text("${value.toInt()} kWh", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("For selected period", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun BmsScreen(
    state: BmsState,
    onChargeCurrentChange: (Float) -> Unit,
    onDischargeCurrentChange: (Float) -> Unit,
    onCutOffVoltageChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            Card {
                Column(Modifier.padding(16.dp)) {
                    Text("BMS Battery System", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        MetricBlock("SOC", "${state.socPercent}%", Modifier.weight(1f))
                        MetricBlock("SOH", "${state.sohPercent}%", Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        MetricBlock("Voltage", "${state.voltageV.toInt()} V", Modifier.weight(1f))
                        MetricBlock("Current", "${state.currentA.toInt()} A", Modifier.weight(1f))
                        MetricBlock("Temp", "${state.temperatureC.toInt()}°C", Modifier.weight(1f))
                    }
                }
            }
        }
        item {
            Text("Cell Status", style = MaterialTheme.typography.titleMedium)
        }
        items(state.cells.chunked(2)) { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { cell ->
                    CellCard(cell, Modifier.weight(1f))
                }
                if (row.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
        item {
            Spacer(Modifier.height(8.dp))
            Text("Battery Protection Settings", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            SliderCard(
                title = "Max Charge Current",
                value = state.maxChargeCurrentA,
                suffix = "A",
                range = 10f..100f,
                onValueChange = onChargeCurrentChange
            )
            SliderCard(
                title = "Max Discharge Current",
                value = state.maxDischargeCurrentA,
                suffix = "A",
                range = 10f..120f,
                onValueChange = onDischargeCurrentChange
            )
            SliderCard(
                title = "Cut-off Voltage",
                value = state.cutOffVoltageV,
                suffix = "V",
                range = 40f..50f,
                onValueChange = onCutOffVoltageChange
            )
        }
    }
}

@Composable
private fun MetricBlock(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    OutlinedCard(modifier = modifier) {
        Column(Modifier.padding(12.dp)) {
            Text(title, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun CellCard(cell: CellStatus, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (cell.hasWarning) SolarOrange.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(Modifier.padding(12.dp)) {
            Text("Cell ${cell.index}", fontWeight = FontWeight.Medium)
            Text("V: ${cell.voltage.toInt()} V")
            Text("T: ${cell.temperatureC.toInt()}°C")
            if (cell.hasWarning) {
                Text(
                    "Temperature Warning",
                    color = SolarOrange,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun SliderCard(
    title: String,
    value: Float,
    suffix: String,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Card(modifier = Modifier.padding(vertical = 6.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text("${value.toInt()} $suffix", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = range,
                steps = 20
            )
        }
    }
}

@Composable
fun SettingsScreen(
    state: SettingsState,
    onToggleNotifications: (Boolean) -> Unit,
    onToggleBatteryAlerts: (Boolean) -> Unit,
    onToggleDarkMode: (Boolean) -> Unit,
    onToggleAutoExport: (Boolean) -> Unit,
    onUpdateFrequency: (Int) -> Unit,
    onExcelFileSelected: (Uri?, String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) {
            }
            val name = context.contentResolver.queryDisplayName(uri)
            onExcelFileSelected(uri, name)
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SettingsCard(title = "Notifications") {
                SettingSwitch(
                    title = "Enable Notifications",
                    subtitle = "Receive system status information",
                    checked = state.notificationsEnabled,
                    onCheckedChange = onToggleNotifications
                )
                SettingSwitch(
                    title = "Battery Alerts",
                    subtitle = "Notify when battery is low or error occurs",
                    checked = state.batteryAlertsEnabled,
                    onCheckedChange = onToggleBatteryAlerts
                )
            }
        }
        item {
            SettingsCard(title = "Display") {
                SettingSwitch(
                    title = "Dark Mode",
                    subtitle = "Night interface",
                    checked = state.darkMode,
                    onCheckedChange = onToggleDarkMode
                )
                SettingStaticRow("Language", state.language)
            }
        }
        item {
            SettingsCard(title = "System") {
                FrequencyDropdown(
                    current = state.updateFrequencySeconds,
                    onUpdateFrequency = onUpdateFrequency
                )
                SettingSwitch(
                    title = "Auto Export Reports",
                    subtitle = "Export monthly summary files",
                    checked = state.autoExportReports,
                    onCheckedChange = onToggleAutoExport
                )
            }
        }
        item {
            SettingsCard(title = "Firebase Data") {
                Text(
                    "Data is loaded from Firebase Realtime Database.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Text("Data source: ${state.excelFileName ?: state.dataPreview}")
                Spacer(Modifier.height(8.dp))
                Text(
                    "URL: https://solar-energy-6452d-default-rtdb.firebaseio.com/",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        item {
            SettingsCard(title = "Mock data (Excel)") {
                Text(
                    "Select an Excel file from the \"mock data\" folder to update simulation data.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Text("Current file: ${state.excelFileName ?: state.dataPreview}")
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = {
                    launcher.launch(arrayOf("application/vnd.ms-excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                }) {
                    Text("Select Excel File")
                }
            }
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card {
        Column(modifier = Modifier.padding(16.dp), content = {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            content()
        })
    }
}

@Composable
private fun SettingSwitch(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(subtitle, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedTrackColor = SolarGreen)
        )
    }
}

@Composable
private fun SettingStaticRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontWeight = FontWeight.SemiBold)
            Text(value, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun FrequencyDropdown(
    current: Int,
    onUpdateFrequency: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val options = listOf(5, 10, 30, 60)
    Column {
        Text("Update Frequency", fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        Box {
            OutlinedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = true }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("$current seconds", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("Data update interval", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(
                        imageVector = Icons.Outlined.ArrowDropDown,
                        contentDescription = null
                    )
                }
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text("$option seconds") },
                        onClick = {
                            expanded = false
                            onUpdateFrequency(option)
                        }
                    )
                }
            }
        }
    }
}

private fun ContentResolver.queryDisplayName(uri: Uri): String? {
    return runCatching {
        query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIndex != -1) cursor.getString(nameIndex) else null
        }
    }.getOrNull()
}

