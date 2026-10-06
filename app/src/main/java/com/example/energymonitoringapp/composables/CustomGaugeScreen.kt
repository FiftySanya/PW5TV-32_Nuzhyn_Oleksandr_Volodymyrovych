package com.example.energymonitoringapp.composables

import android.graphics.Paint
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

enum class GaugeThemePalette(val label: String, val normalColor: Color, val warnColor: Color, val alertColor: Color) {
    CLASSIC("Класична", Color(0xFF2E7D32), Color(0xFFF57C00), Color(0xFFD32F2F)),
    CYBER_NEON("Кібер-Неон", Color(0xFF00E676), Color(0xFFFFD600), Color(0xFFFF1744)),
    EMERALD_ECO("Еко-Смарагд", Color(0xFF00B0FF), Color(0xFF00E5FF), Color(0xFFE040FB))
}

@Composable
fun EnergyLoadSpeedometerGauge(
    currentPowerKw: Float,
    maxPowerKw: Float = 240f,
    modifier: Modifier = Modifier,
    palette: GaugeThemePalette = GaugeThemePalette.CLASSIC,
    showScaleNumbers: Boolean = true
) {
    val animatedPower by animateFloatAsState(
        targetValue = currentPowerKw.coerceIn(0f, maxPowerKw),
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "GaugeNeedleAnimation"
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height * 0.65f)
            val radius = size.width * 0.42f

            val startAngle = 140f
            val totalSweep = 260f

            drawArc(
                color = Color.LightGray.copy(alpha = 0.35f),
                startAngle = startAngle,
                sweepAngle = totalSweep,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = 24f, cap = StrokeCap.Round)
            )

            val sweep1 = totalSweep * 0.5f
            drawArc(
                color = palette.normalColor,
                startAngle = startAngle,
                sweepAngle = sweep1,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = 24f, cap = StrokeCap.Round)
            )

            val sweep2 = totalSweep * 0.28f
            drawArc(
                color = palette.warnColor,
                startAngle = startAngle + sweep1,
                sweepAngle = sweep2,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = 24f)
            )

            val sweep3 = totalSweep * 0.22f
            drawArc(
                color = palette.alertColor,
                startAngle = startAngle + sweep1 + sweep2,
                sweepAngle = sweep3,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = 24f, cap = StrokeCap.Round)
            )

            val totalTicks = 12
            for (i in 0..totalTicks) {
                val fraction = i.toFloat() / totalTicks
                val angleDeg = startAngle + fraction * totalSweep
                val angleRad = Math.toRadians(angleDeg.toDouble())

                val innerR = radius - 20f
                val outerR = radius + 14f

                val startTick = Offset(
                    (center.x + innerR * cos(angleRad)).toFloat(),
                    (center.y + innerR * sin(angleRad)).toFloat()
                )
                val endTick = Offset(
                    (center.x + outerR * cos(angleRad)).toFloat(),
                    (center.y + outerR * sin(angleRad)).toFloat()
                )

                drawLine(
                    color = Color.Gray,
                    start = startTick,
                    end = endTick,
                    strokeWidth = if (i % 3 == 0) 4f else 2f
                )

                if (showScaleNumbers && i % 3 == 0) {
                    val labelVal = (fraction * maxPowerKw).toInt().toString()
                    val textR = radius - 44f
                    val tx = (center.x + textR * cos(angleRad)).toFloat()
                    val ty = (center.y + textR * sin(angleRad)).toFloat() + 10f

                    drawContext.canvas.nativeCanvas.drawText(
                        labelVal,
                        tx,
                        ty,
                        Paint().apply {
                            color = android.graphics.Color.DKGRAY
                            textSize = 28f
                            textAlign = Paint.Align.CENTER
                            isFakeBoldText = true
                        }
                    )
                }
            }

            val needleFraction = (animatedPower / maxPowerKw).coerceIn(0f, 1f)
            val needleAngleDeg = startAngle + needleFraction * totalSweep
            val needleAngleRad = Math.toRadians(needleAngleDeg.toDouble())

            val needleLen = radius * 0.78f
            val needleEnd = Offset(
                (center.x + needleLen * cos(needleAngleRad)).toFloat(),
                (center.y + needleLen * sin(needleAngleRad)).toFloat()
            )

            drawLine(
                color = Color(0xFFD32F2F),
                start = center,
                end = needleEnd,
                strokeWidth = 6f,
                cap = StrokeCap.Round
            )

            drawCircle(color = Color.DarkGray, radius = 18f, center = center)
            drawCircle(color = Color.White, radius = 7f, center = center)
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.85f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "${"%.1f".format(animatedPower)} кВт",
                    color = Color(0xFF00E676),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }
            Text(
                text = "Навантаження підстанції",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun CustomGaugeScreen() {
    var powerKw by remember { mutableFloatStateOf(142.5f) }
    var selectedPalette by remember { mutableStateOf(GaugeThemePalette.CLASSIC) }
    var showNumbers by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "ПЗ № 20: Кастомний компонент Jetpack Compose",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Спідометр активної потужності енергомережі на Canvas з анімованою стрілкою",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                EnergyLoadSpeedometerGauge(
                    currentPowerKw = powerKw,
                    maxPowerKw = 240f,
                    palette = selectedPalette,
                    showScaleNumbers = showNumbers,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Керування навантаженням та параметрами:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Потужність генератора: ${powerKw.toInt()} кВт",
                    style = MaterialTheme.typography.bodyMedium
                )
                Slider(
                    value = powerKw,
                    onValueChange = { powerKw = it },
                    valueRange = 0f..240f
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { powerKw = 75f },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                    ) {
                        Text("Норма (75 кВт)", style = MaterialTheme.typography.labelSmall)
                    }

                    Button(
                        onClick = { powerKw = 155f },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF57C00)),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                    ) {
                        Text("Увага (155 кВт)", style = MaterialTheme.typography.labelSmall)
                    }

                    Button(
                        onClick = { powerKw = 220f },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                    ) {
                        Text("Аварія (220 кВт)", style = MaterialTheme.typography.labelSmall)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Колірна тема шкали індикатора:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GaugeThemePalette.values().forEach { palette ->
                        FilterChip(
                            selected = selectedPalette == palette,
                            onClick = { selectedPalette = palette },
                            label = { Text(palette.label, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = showNumbers,
                        onCheckedChange = { showNumbers = it }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Відображати числові мітки на шкалі (0..240)", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
