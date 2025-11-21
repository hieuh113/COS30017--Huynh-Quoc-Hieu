package com.example.asm3

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.asm3.model.EnergyUiState

enum class EnergyTab(
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Energy("Energy", Icons.Outlined.WbSunny),
    Stats("Stats", Icons.Outlined.ShowChart),
    Bms("BMS", Icons.Outlined.BatteryChargingFull),
    Settings("Settings", Icons.Outlined.Settings)
}

@Composable
fun EnergyApp(
    viewModel: EnergyViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by rememberSaveable { mutableStateOf(EnergyTab.Energy) }

    Scaffold(
        bottomBar = {
            EnergyBottomNavigation(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )
        }
    ) { padding ->
        EnergyAppContent(
            uiState = uiState,
            selectedTab = selectedTab,
            modifier = Modifier.padding(padding),
            viewModel = viewModel
        )
    }
}

@Composable
private fun EnergyAppContent(
    uiState: EnergyUiState,
    selectedTab: EnergyTab,
    modifier: Modifier = Modifier,
    viewModel: EnergyViewModel
) {
    when (selectedTab) {
        EnergyTab.Energy -> EnergyFlowScreen(
            state = uiState.dashboard,
            onSelectStart = viewModel::selectStartSlot,
            onSelectEnd = viewModel::selectEndSlot,
            onRefresh = viewModel::refreshDashboard,
            modifier = modifier
        )

        EnergyTab.Stats -> StatsScreen(
            state = uiState.stats,
            onRangeSelected = viewModel::selectStatsRange,
            modifier = modifier
        )

        EnergyTab.Bms -> BmsScreen(
            state = uiState.bms,
            onChargeCurrentChange = viewModel::updateChargeCurrent,
            onDischargeCurrentChange = viewModel::updateDischargeCurrent,
            onCutOffVoltageChange = viewModel::updateCutOffVoltage,
            modifier = modifier
        )

        EnergyTab.Settings -> SettingsScreen(
            state = uiState.settings,
            onToggleNotifications = viewModel::toggleNotifications,
            onToggleBatteryAlerts = viewModel::toggleBatteryAlerts,
            onToggleDarkMode = viewModel::toggleDarkMode,
            onToggleAutoExport = viewModel::toggleAutoExport,
            onUpdateFrequency = viewModel::updateFrequency,
            onExcelFileSelected = viewModel::onExcelFileSelected,
            modifier = modifier
        )
    }
}

@Composable
private fun EnergyBottomNavigation(
    selectedTab: EnergyTab,
    onTabSelected: (EnergyTab) -> Unit
) {
    NavigationBar {
        EnergyTab.values().forEach { tab ->
            NavigationBarItem(
                selected = tab == selectedTab,
                onClick = { onTabSelected(tab) },
                icon = { Icon(tab.icon, contentDescription = tab.label) },
                label = { Text(tab.label) }
            )
        }
    }
}

