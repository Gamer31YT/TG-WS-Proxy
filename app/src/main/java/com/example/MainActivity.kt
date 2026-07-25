package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.compose.material3.MaterialTheme
import com.example.ui.ProxyViewModel
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DcServersScreen
import com.example.ui.screens.HelpScreen
import com.example.ui.screens.LogsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.PurpleDark
import com.example.ui.theme.PurpleLightContainer
import com.example.ui.theme.PurplePrimary
import com.example.ui.theme.TgWsProxyTheme

enum class NavigationTab(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    DASHBOARD("Главная", Icons.Filled.Home, Icons.Outlined.Home, "nav_dashboard"),
    SERVERS("Серверы", Icons.Filled.Dns, Icons.Outlined.Dns, "nav_servers"),
    SETTINGS("Настройки", Icons.Filled.Settings, Icons.Outlined.Settings, "nav_settings"),
    LOGS("Логи", Icons.Filled.List, Icons.Outlined.List, "nav_logs"),
    GUIDE("Справка", Icons.Filled.HelpOutline, Icons.Outlined.HelpOutline, "nav_guide")
}

class MainActivity : ComponentActivity() {

    private val viewModel: ProxyViewModel by viewModels()

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        checkPermissions()

        setContent {
            TgWsProxyTheme {
                MainAppScreen(viewModel = viewModel)
            }
        }
    }

    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

@Composable
fun MainAppScreen(viewModel: ProxyViewModel) {
    var selectedTab by remember { mutableStateOf(NavigationTab.DASHBOARD) }

    val config by viewModel.proxyConfig.collectAsState()
    val stats by viewModel.proxyStats.collectAsState()
    val speedHistory by viewModel.speedHistory.collectAsState()
    val presets by viewModel.presets.collectAsState()
    val isTestingPings by viewModel.isTestingPings.collectAsState()
    val filteredLogs by viewModel.filteredLogs.collectAsState()
    val logFilter by viewModel.logFilter.collectAsState()
    val logLevelFilter by viewModel.logLevelFilter.collectAsState()

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                NavigationTab.entries.forEach { tab ->
                    val isSelected = selectedTab == tab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { selectedTab = tab },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = tab.title
                            )
                        },
                        label = { Text(tab.title) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = PurpleLightContainer
                        ),
                        modifier = Modifier.testTag(tab.testTag)
                    )
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        val modifier = Modifier.padding(innerPadding)

        when (selectedTab) {
            NavigationTab.DASHBOARD -> DashboardScreen(
                config = config,
                stats = stats,
                speedHistory = speedHistory,
                onToggleProxy = { viewModel.toggleProxy() },
                modifier = modifier
            )

            NavigationTab.SERVERS -> DcServersScreen(
                presets = presets,
                config = config,
                isTestingPings = isTestingPings,
                onSelectPreset = { viewModel.selectPreset(it) },
                onTestPings = { viewModel.testAllPings() },
                onAddCustomPreset = { name, url, loc -> viewModel.addCustomPreset(name, url, loc) },
                onDeletePreset = { viewModel.deletePreset(it) },
                modifier = modifier
            )

            NavigationTab.SETTINGS -> SettingsScreen(
                config = config,
                onSaveConfig = { viewModel.updateConfig(it) },
                modifier = modifier
            )

            NavigationTab.LOGS -> LogsScreen(
                logs = filteredLogs,
                searchQuery = logFilter,
                selectedLevel = logLevelFilter,
                onSearchChange = { viewModel.setLogFilter(it) },
                onLevelSelect = { viewModel.setLogLevelFilter(it) },
                onClearLogs = { viewModel.clearLogs() },
                modifier = modifier
            )

            NavigationTab.GUIDE -> HelpScreen(modifier = modifier)
        }
    }
}
