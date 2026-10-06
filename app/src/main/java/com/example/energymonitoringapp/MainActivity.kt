package com.example.energymonitoringapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.energymonitoringapp.composables.ChartsScreen
import com.example.energymonitoringapp.composables.CustomGaugeScreen
import com.example.energymonitoringapp.composables.NotificationsScreen
import com.example.energymonitoringapp.composables.SensorsScreen
import com.example.energymonitoringapp.ui.theme.EnergyMonitoringAppTheme

enum class Pr5Tab(val title: String, val icon: ImageVector) {
    SENSORS("Сенсори", Icons.Default.Sensors),
    NOTIFICATIONS("Сповіщення", Icons.Default.Notifications),
    CHARTS("Графіки", Icons.Default.ShowChart),
    GAUGE("Спідометр", Icons.Default.Speed)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EnergyMonitoringAppTheme {
                MainAppContent()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContent() {
    var currentTab by remember { mutableStateOf(Pr5Tab.SENSORS) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("ПР5: Енергомоніторинг (Вар. 15)") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        bottomBar = {
            NavigationBar {
                Pr5Tab.values().forEach { tab ->
                    NavigationBarItem(
                        selected = currentTab == tab,
                        onClick = { currentTab = tab },
                        icon = { Icon(tab.icon, contentDescription = tab.title) },
                        label = { Text(tab.title) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                Pr5Tab.SENSORS -> SensorsScreen()
                Pr5Tab.NOTIFICATIONS -> NotificationsScreen()
                Pr5Tab.CHARTS -> ChartsScreen()
                Pr5Tab.GAUGE -> CustomGaugeScreen()
            }
        }
    }
}