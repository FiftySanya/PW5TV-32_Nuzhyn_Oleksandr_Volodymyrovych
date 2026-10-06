package com.example.energymonitoringapp.composables

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.*

data class EnergyAlertNotification(
    val id: Int,
    val title: String,
    val message: String,
    val severity: String,
    val timestamp: String,
    val channelId: String,
    var isAcknowledged: Boolean = false
) {
    val color: Color
        get() = when (severity) {
            "CRITICAL" -> Color(0xFFD32F2F)
            "WARNING" -> Color(0xFFF57C00)
            else -> Color(0xFF1976D2)
        }
}

class NotificationsViewModel : ViewModel() {
    private val _notificationsList = MutableStateFlow<List<EnergyAlertNotification>>(emptyList())
    val notificationsList: StateFlow<List<EnergyAlertNotification>> = _notificationsList.asStateFlow()

    private val _actionLogs = MutableStateFlow<List<String>>(
        listOf("Ініціалізація диспетчерської служби сповіщень. Канали зареєстровано.")
    )
    val actionLogs: StateFlow<List<String>> = _actionLogs.asStateFlow()

    fun addNotification(alert: EnergyAlertNotification) {
        _notificationsList.value = listOf(alert) + _notificationsList.value
        logAction("Створено сповіщення [#${alert.id}]: ${alert.title}")
    }

    fun acknowledgeAlert(id: Int, actionName: String) {
        _notificationsList.value = _notificationsList.value.map {
            if (it.id == id) it.copy(isAcknowledged = true) else it
        }
        logAction("Дія диспетчера [$actionName] для сповіщення #$id")
    }

    fun dismissNotification(id: Int) {
        _notificationsList.value = _notificationsList.value.filter { it.id != id }
        logAction("Сповіщення #$id закрито та архівовано.")
    }

    fun clearAll() {
        _notificationsList.value = emptyList()
        logAction("Усі активні сповіщення скинуто.")
    }

    private fun logAction(msg: String) {
        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        _actionLogs.value = listOf("[$time] $msg") + _actionLogs.value.take(25)
    }
}

@Composable
fun NotificationsScreen(
    viewModel: NotificationsViewModel = viewModel()
) {
    val context = LocalContext.current
    val alerts by viewModel.notificationsList.collectAsState()
    val logs by viewModel.actionLogs.collectAsState()

    LaunchedEffect(Unit) {
        createNotificationChannels(context)
    }

    fun triggerSystemNotification(
        id: Int,
        title: String,
        body: String,
        channelId: String,
        severity: String
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val pendingIntent = PendingIntent.getActivity(
            context,
            id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(if (severity == "CRITICAL") NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .addAction(android.R.drawable.ic_menu_agenda, "Підтвердити", pendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Відхилити", pendingIntent)

        notificationManager.notify(id, builder.build())

        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        viewModel.addNotification(
            EnergyAlertNotification(
                id = id,
                title = title,
                message = body,
                severity = severity,
                timestamp = time,
                channelId = channelId
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "ПЗ № 18: Інтерактивні сповіщення енергосистеми",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Канали NotificationChannel, кнопки швидкої дії (Action Buttons) та синхронізація з UI",
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
                    text = "Імітація надзвичайних подій енергомережі:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val alertId = (100..999).random()
                            triggerSystemNotification(
                                id = alertId,
                                title = "АВАРІЯ: Перевантаження 110 кВ",
                                body = "Підстанція ПС-110 «Північна»: струм 320А перевищив граничний ліміт!",
                                channelId = "critical_energy_channel",
                                severity = "CRITICAL"
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Критична аварія", style = MaterialTheme.typography.labelMedium)
                    }

                    Button(
                        onClick = {
                            val alertId = (100..999).random()
                            triggerSystemNotification(
                                id = alertId,
                                title = "УВАГА: Падіння напруги",
                                body = "Фідер №4: зниження напруги до 198 В у житловому масиві.",
                                channelId = "warning_energy_channel",
                                severity = "WARNING"
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF57C00)),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Попередження", style = MaterialTheme.typography.labelMedium)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val alertId = (100..999).random()
                            triggerSystemNotification(
                                id = alertId,
                                title = "ІНФО: Автоматичне скидання",
                                body = "Спрацювала система АЧР-1: баланс частоти 50.0 Гц відновлено.",
                                channelId = "info_energy_channel",
                                severity = "INFO"
                            )
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Інфо-подія", style = MaterialTheme.typography.labelMedium)
                    }

                    OutlinedButton(
                        onClick = {
                            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                            notificationManager.cancelAll()
                            viewModel.clearAll()
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.ClearAll, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Очистити все", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Активні інтерактивні сповіщення (${alerts.size}):",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(6.dp))

        if (alerts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Активних надзвичайних сповіщень немає. Натисніть кнопку вище для генерації.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(alerts, key = { it.id }) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .background(item.color, RoundedCornerShape(5.dp))
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(item.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                }
                                Text(item.timestamp, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(item.message, style = MaterialTheme.typography.bodySmall)

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (item.isAcknowledged) {
                                    Surface(
                                        color = Color(0xFF2E7D32).copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            "Підтверджено диспетчером",
                                            color = Color(0xFF2E7D32),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                } else {
                                    Button(
                                        onClick = { viewModel.acknowledgeAlert(item.id, "Скидання навантаження") },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text("Підтвердити", style = MaterialTheme.typography.labelSmall)
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                OutlinedButton(
                                    onClick = {
                                        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                                        notificationManager.cancel(item.id)
                                        viewModel.dismissNotification(item.id)
                                    },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text("Закрити", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Журнал синхронізації та дій диспетчера:",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(4.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(logs) { log ->
                    Text(
                        text = log,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun createNotificationChannels(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val criticalChannel = NotificationChannel(
            "critical_energy_channel",
            "Критичні аварії енергосистеми",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Сповіщення про перевищення струму, напруги та відключення ліній"
            enableVibration(true)
        }

        val warningChannel = NotificationChannel(
            "warning_energy_channel",
            "Попередження про навантаження",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Сповіщення про коливання параметрів мережі"
        }

        val infoChannel = NotificationChannel(
            "info_energy_channel",
            "Інформаційні повідомлення",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Повідомлення про роботу автоматики та графіки"
        }

        notificationManager.createNotificationChannel(criticalChannel)
        notificationManager.createNotificationChannel(warningChannel)
        notificationManager.createNotificationChannel(infoChannel)
    }
}
