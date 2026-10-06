package com.example.energymonitoringapp.composables

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

data class PowerTimePoint(
    val timeLabel: String,
    val powerKw: Float
)

data class ZoneEnergyUsage(
    val zoneName: String,
    val totalKwh: Float,
    val color: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChartsScreen() {
    var selectedInterval by remember { mutableStateOf("24 Години (Доба)") }
    var scaleMultiplier by remember { mutableFloatStateOf(1.0f) }
    var selectedLinePointIndex by remember { mutableStateOf<Int?>(4) }
    var selectedBarIndex by remember { mutableStateOf<Int?>(1) }

    val rawData24h = remember {
        listOf(
            PowerTimePoint("00:00", 112f),
            PowerTimePoint("03:00", 94f),
            PowerTimePoint("06:00", 145f),
            PowerTimePoint("09:00", 210f),
            PowerTimePoint("12:00", 188f),
            PowerTimePoint("15:00", 195f),
            PowerTimePoint("18:00", 248f),
            PowerTimePoint("21:00", 225f),
            PowerTimePoint("23:59", 160f)
        )
    }

    val rawData8h = remember {
        listOf(
            PowerTimePoint("08:00", 180f),
            PowerTimePoint("09:00", 210f),
            PowerTimePoint("10:00", 225f),
            PowerTimePoint("11:00", 215f),
            PowerTimePoint("12:00", 188f),
            PowerTimePoint("13:00", 192f),
            PowerTimePoint("14:00", 204f),
            PowerTimePoint("15:00", 195f),
            PowerTimePoint("16:00", 220f)
        )
    }

    val rawData1h = remember {
        listOf(
            PowerTimePoint("14:00", 195f),
            PowerTimePoint("14:10", 198f),
            PowerTimePoint("14:20", 202f),
            PowerTimePoint("14:30", 206f),
            PowerTimePoint("14:40", 212f),
            PowerTimePoint("14:50", 209f),
            PowerTimePoint("15:00", 204f)
        )
    }

    val activeLinePoints = when (selectedInterval) {
        "1 Година" -> rawData1h
        "8 Годин (Зміна)" -> rawData8h
        else -> rawData24h
    }

    val zonesData = remember {
        listOf(
            ZoneEnergyUsage("Промзона Т-1", 1420f, Color(0xFF1976D2)),
            ZoneEnergyUsage("Житломасив", 980f, Color(0xFF388E3C)),
            ZoneEnergyUsage("Офісний парк", 640f, Color(0xFFF57C00)),
            ZoneEnergyUsage("Транспорт/Тр.", 510f, Color(0xFF7B1FA2)),
            ZoneEnergyUsage("СЕС Залишок", 390f, Color(0xFF0097A7))
        )
    }

    val totalKwSum = activeLinePoints.sumOf { it.powerKw.toDouble() }
    val averagePower = if (activeLinePoints.isNotEmpty()) totalKwSum / activeLinePoints.size else 0.0
    val peakPoint = activeLinePoints.maxByOrNull { it.powerKw }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "ПЗ № 19: Інтерактивні графіки та діаграми",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Одночасне відображення лінійного графіка динаміки та стовпчикової діаграми зон",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf("1 Година", "8 Годин (Зміна)", "24 Години (Доба)").forEach { interval ->
                FilterChip(
                    selected = selectedInterval == interval,
                    onClick = {
                        selectedInterval = interval
                        selectedLinePointIndex = null
                    },
                    label = { Text(interval, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Масштаб осі Y: ${(scaleMultiplier * 100).toInt()}%",
                style = MaterialTheme.typography.labelMedium
            )
            Row {
                IconButton(onClick = { scaleMultiplier = (scaleMultiplier - 0.1f).coerceAtLeast(0.8f) }) {
                    Icon(Icons.Default.ZoomOut, contentDescription = null)
                }
                IconButton(onClick = { scaleMultiplier = (scaleMultiplier + 0.1f).coerceAtMost(1.5f) }) {
                    Icon(Icons.Default.ZoomIn, contentDescription = null)
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "1. Лінійний графік навантаження (кВт)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    selectedLinePointIndex?.let { idx ->
                        if (idx in activeLinePoints.indices) {
                            val pt = activeLinePoints[idx]
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "${pt.timeLabel}: ${pt.powerKw} кВт",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(activeLinePoints) {
                                detectTapGestures { offset ->
                                    val count = activeLinePoints.size
                                    if (count > 1) {
                                        val stepX = (size.width - 40f) / (count - 1)
                                        val clickedIdx = ((offset.x - 20f) / stepX).roundToInt()
                                        if (clickedIdx in activeLinePoints.indices) {
                                            selectedLinePointIndex = clickedIdx
                                        }
                                    }
                                }
                            }
                    ) {
                        val w = size.width
                        val h = size.height
                        val padX = 20f
                        val padY = 20f
                        val graphW = w - padX * 2
                        val graphH = h - padY * 2

                        val maxVal = (activeLinePoints.maxOfOrNull { it.powerKw } ?: 250f) * scaleMultiplier
                        val minVal = 0f

                        val gridColor = Color.LightGray.copy(alpha = 0.5f)
                        for (i in 0..4) {
                            val y = padY + graphH * i / 4f
                            drawLine(gridColor, Offset(padX, y), Offset(w - padX, y), strokeWidth = 1f)
                        }

                        if (activeLinePoints.size > 1) {
                            val stepX = graphW / (activeLinePoints.size - 1)
                            val path = Path()

                            activeLinePoints.forEachIndexed { i, pt ->
                                val x = padX + i * stepX
                                val normY = (pt.powerKw - minVal) / (maxVal - minVal)
                                val y = (padY + graphH) - normY * graphH

                                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                            }

                            drawPath(
                                path = path,
                                color = Color(0xFF1976D2),
                                style = Stroke(width = 3.5f, cap = StrokeCap.Round)
                            )

                            activeLinePoints.forEachIndexed { i, pt ->
                                val x = padX + i * stepX
                                val normY = (pt.powerKw - minVal) / (maxVal - minVal)
                                val y = (padY + graphH) - normY * graphH

                                val isSel = i == selectedLinePointIndex
                                drawCircle(
                                    color = if (isSel) Color(0xFFD32F2F) else Color(0xFF1976D2),
                                    radius = if (isSel) 7f else 4.5f,
                                    center = Offset(x, y)
                                )
                                drawCircle(
                                    color = Color.White,
                                    radius = if (isSel) 3.5f else 2f,
                                    center = Offset(x, y)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "2. Стовпчикова діаграма за зонами (кВт·год)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    selectedBarIndex?.let { idx ->
                        if (idx in zonesData.indices) {
                            val z = zonesData[idx]
                            Surface(
                                color = z.color.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "${z.zoneName}: ${z.totalKwh.toInt()} кВт·год",
                                    color = z.color,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp)
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(zonesData) {
                                detectTapGestures { offset ->
                                    val count = zonesData.size
                                    val barSlot = size.width / count
                                    val clickedIdx = (offset.x / barSlot).toInt()
                                    if (clickedIdx in zonesData.indices) {
                                        selectedBarIndex = clickedIdx
                                    }
                                }
                            }
                    ) {
                        val w = size.width
                        val h = size.height
                        val maxBar = zonesData.maxOf { it.totalKwh } * 1.15f
                        val slotW = w / zonesData.size
                        val barW = slotW * 0.55f

                        zonesData.forEachIndexed { i, z ->
                            val barH = (z.totalKwh / maxBar) * (h - 20f)
                            val x = i * slotW + (slotW - barW) / 2f
                            val y = h - barH - 10f

                            val isSel = i == selectedBarIndex
                            drawRoundRect(
                                color = if (isSel) z.color else z.color.copy(alpha = 0.75f),
                                topLeft = Offset(x, y),
                                size = Size(barW, barH),
                                cornerRadius = CornerRadius(6f, 6f)
                            )

                            if (isSel) {
                                drawRoundRect(
                                    color = Color.Black,
                                    topLeft = Offset(x, y),
                                    size = Size(barW, barH),
                                    style = Stroke(width = 2f),
                                    cornerRadius = CornerRadius(6f, 6f)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "Зведена статистика за обраний інтервал:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Середнє навантаження:", style = MaterialTheme.typography.bodySmall)
                        Text("${"%.1f".format(averagePower)} кВт", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                    }

                    Column {
                        Text("Пікове значення:", style = MaterialTheme.typography.bodySmall)
                        Text("${peakPoint?.powerKw ?: 0f} кВт (${peakPoint?.timeLabel ?: "-"})", fontWeight = FontWeight.Bold, color = Color(0xFFD32F2F), style = MaterialTheme.typography.bodyLarge)
                    }

                    Column {
                        Text("Загальна генерація:", style = MaterialTheme.typography.bodySmall)
                        Text("${zonesData.sumOf { it.totalKwh.toDouble() }.toInt()} кВт·год", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}
