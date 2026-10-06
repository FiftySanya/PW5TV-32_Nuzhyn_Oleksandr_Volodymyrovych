package com.example.energymonitoringapp.composables

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

data class EnergySensorItem(
    val id: String,
    val name: String,
    val type: String,
    val currentValue: Double,
    val unit: String,
    val warningThreshold: Double,
    val criticalThreshold: Double,
    val icon: ImageVector,
    val isEnabled: Boolean = true
) {
    val status: String
        get() = when {
            currentValue >= criticalThreshold -> "Критично"
            currentValue >= warningThreshold -> "Увага"
            else -> "Норма"
        }

    val statusColor: Color
        get() = when (status) {
            "Критично" -> Color(0xFFD32F2F)
            "Увага" -> Color(0xFFF57C00)
            else -> Color(0xFF2E7D32)
        }
}

class SensorsViewModel : ViewModel() {
    private val _sensorsList = MutableStateFlow<List<EnergySensorItem>>(
        listOf(
            EnergySensorItem("S1", "Температура трансформатора Т-1", "Температура", 68.4, "°C", 75.0, 90.0, Icons.Default.Thermostat),
            EnergySensorItem("S2", "Струм фази A (Фідер №3)", "Струм", 242.0, "А", 260.0, 300.0, Icons.Default.Bolt),
            EnergySensorItem("S3", "Лінійна напруга 110 кВ", "Напруга", 112.4, "кВ", 115.0, 120.0, Icons.Default.ElectricMeter),
            EnergySensorItem("S4", "Вібрація ротора генератора", "Акселерометр", 1.45, "м/с²", 2.2, 3.5, Icons.Default.Vibration),
            EnergySensorItem("S5", "Сонячна інсоляція СЕС", "Освітленість", 840.0, "лк", 900.0, 1100.0, Icons.Default.WbSunny),
            EnergySensorItem("S6", "Температура оливи вимикача", "Температура", 52.1, "°C", 65.0, 80.0, Icons.Default.DeviceThermostat)
        )
    )
    val sensorsList: StateFlow<List<EnergySensorItem>> = _sensorsList.asStateFlow()

    private val _filterType = MutableStateFlow("Всі")
    val filterType: StateFlow<String> = _filterType.asStateFlow()

    private val _sortByValueDesc = MutableStateFlow(false)
    val sortByValueDesc: StateFlow<Boolean> = _sortByValueDesc.asStateFlow()

    private val _customThresholdMultiplier = MutableStateFlow(1.0f)
    val customThresholdMultiplier: StateFlow<Float> = _customThresholdMultiplier.asStateFlow()

    init {
        viewModelScope.launch {
            while (true) {
                delay(1200)
                updateSensorTelemetry()
            }
        }
    }

    private fun updateSensorTelemetry() {
        val current = _sensorsList.value
        _sensorsList.value = current.map { item ->
            val delta = when (item.type) {
                "Температура" -> Random.nextDouble(-0.8, 1.2)
                "Струм" -> Random.nextDouble(-4.0, 5.0)
                "Напруга" -> Random.nextDouble(-0.3, 0.4)
                "Акселерометр" -> Random.nextDouble(-0.15, 0.2)
                "Освітленість" -> Random.nextDouble(-20.0, 25.0)
                else -> Random.nextDouble(-1.0, 1.0)
            }
            val newValue = Math.round((item.currentValue + delta).coerceAtLeast(0.0) * 100.0) / 100.0
            item.copy(currentValue = newValue)
        }
    }

    fun setFilterType(type: String) {
        _filterType.value = type
    }

    fun toggleSort() {
        _sortByValueDesc.value = !_sortByValueDesc.value
    }

    fun setThresholdMultiplier(multiplier: Float) {
        _customThresholdMultiplier.value = multiplier
    }

    fun toggleSensorEnabled(id: String) {
        _sensorsList.value = _sensorsList.value.map {
            if (it.id == id) it.copy(isEnabled = !it.isEnabled) else it
        }
    }

    fun simulateAlert() {
        _sensorsList.value = _sensorsList.value.map { item ->
            if (item.id == "S1") item.copy(currentValue = 94.5)
            else if (item.id == "S2") item.copy(currentValue = 310.0)
            else item
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SensorsScreen(
    viewModel: SensorsViewModel = viewModel()
) {
    val context = LocalContext.current
    val sensors by viewModel.sensorsList.collectAsState()
    val filterType by viewModel.filterType.collectAsState()
    val sortByValueDesc by viewModel.sortByValueDesc.collectAsState()
    val thresholdMultiplier by viewModel.customThresholdMultiplier.collectAsState()

    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val lightSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_LIGHT)
        val accelSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        if (lightSensor != null) {
            sensorManager.registerListener(listener, lightSensor, SensorManager.SENSOR_DELAY_UI)
        }
        if (accelSensor != null) {
            sensorManager.registerListener(listener, accelSensor, SensorManager.SENSOR_DELAY_UI)
        }

        onDispose {
            sensorManager?.unregisterListener(listener)
        }
    }

    val categories = listOf("Всі", "Температура", "Струм", "Напруга", "Акселерометр", "Освітленість")

    val displayedSensors = remember(sensors, filterType, sortByValueDesc, thresholdMultiplier) {
        val filtered = if (filterType == "Всі") sensors else sensors.filter { it.type == filterType }
        val adjusted = filtered.map {
            it.copy(
                warningThreshold = it.warningThreshold * thresholdMultiplier,
                criticalThreshold = it.criticalThreshold * thresholdMultiplier
            )
        }
        if (sortByValueDesc) adjusted.sortedByDescending { it.currentValue } else adjusted
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "ПЗ № 17: Моніторинг сенсорів енергосистеми",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Інтерактивна панель багатосенсорного збору телеметрії та контролю порогів",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "Порогові коефіцієнти аварійної сигналізації: ${(thresholdMultiplier * 100).toInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Slider(
                    value = thresholdMultiplier,
                    onValueChange = { viewModel.setThresholdMultiplier(it) },
                    valueRange = 0.7f..1.3f,
                    steps = 5
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { viewModel.toggleSort() },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = if (sortByValueDesc) Icons.Default.ArrowDownward else Icons.Default.Sort,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (sortByValueDesc) "Сортування: Значення" else "Сортування: Стандартне")
                    }

                    Button(
                        onClick = { viewModel.simulateAlert() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Тест аварії")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        ScrollableTabRow(
            selectedTabIndex = categories.indexOf(filterType),
            edgePadding = 0.dp
        ) {
            categories.forEachIndexed { index, cat ->
                Tab(
                    selected = filterType == cat,
                    onClick = { viewModel.setFilterType(cat) },
                    text = { Text(cat) }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(displayedSensors, key = { it.id }) { item ->
                SensorCard(item = item, onToggle = { viewModel.toggleSensorEnabled(item.id) })
            }
        }
    }
}

@Composable
fun SensorCard(
    item: EnergySensorItem,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(item.statusColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(item.icon, contentDescription = null, tint = item.statusColor)
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Пороги: Попер. >= ${"%.1f".format(item.warningThreshold)} | Авар. >= ${"%.1f".format(item.criticalThreshold)} ${item.unit}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${item.currentValue} ${item.unit}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = item.statusColor
                )

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = item.statusColor.copy(alpha = 0.2f),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(
                        text = item.status,
                        color = item.statusColor,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}
