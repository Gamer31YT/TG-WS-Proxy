package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.proxy.DcPreset
import com.example.proxy.ProxyConfig
import com.example.proxy.ProxyStats
import com.example.ui.SpeedPoint
import com.example.ui.components.PowerButton
import com.example.ui.components.TelegramConnectCard
import com.example.ui.components.TrafficChart
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonGreenTerminal
import com.example.ui.theme.PurpleDark
import com.example.ui.theme.PurpleLightContainer
import com.example.ui.theme.PurplePrimary
import com.example.ui.theme.TelegramBlue

@Composable
fun DashboardScreen(
    config: ProxyConfig,
    stats: ProxyStats,
    speedHistory: List<SpeedPoint>,
    onToggleProxy: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val activeDc = DcPreset.OFFICIAL_PRESETS.find { it.id == config.selectedDcId }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Status Card (Bold Typography Design Theme)
        Card(
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (stats.isRunning) PurplePrimary else MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = if (stats.isRunning) PurpleDark else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(32.dp)
                )
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "СТАТУС MTPROTO ПРОКСИ",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        color = if (stats.isRunning) PurpleLightContainer else PurplePrimary
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (stats.isRunning) NeonGreen else Color.Gray.copy(alpha = 0.3f)
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (stats.isRunning) "АКТИВЕН" else "ВЫКЛЮЧЕН",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // HUGE Bold Typography Banner
                Text(
                    text = if (stats.isRunning) "РАБОТАЕТ" else "ГОТОВ",
                    fontSize = 52.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1.5).sp,
                    lineHeight = 52.sp,
                    color = if (stats.isRunning) Color.White else MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "Локальный MTProto SOCKS5/WS Реле • ${config.listenAddress}:${config.localPort}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (stats.isRunning) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text(
                            text = "ЦЕНТР ДАННЫХ TELEGRAM",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                            color = if (stats.isRunning) PurpleLightContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (config.customWsUrl.isNotBlank()) "Пользовательский WSS" else "${activeDc?.flag ?: "⚡"} ${activeDc?.name ?: "Основной Сервер"}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (stats.isRunning) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Connections badge pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (stats.isRunning) PurpleDark else PurpleLightContainer
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "${stats.activeConnections} соед.",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (stats.isRunning) PurpleLightContainer else PurpleDark
                        )
                    }
                }
            }
        }

        // Power Master Toggle Button
        PowerButton(
            isRunning = stats.isRunning,
            onClick = onToggleProxy,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        // Real-time Traffic Grid
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            SpeedMetricCard(
                title = "ВХОДЯЩИЙ ТРАФИК",
                value = formatBytes(stats.bytesDownloaded),
                subtext = formatSpeed(stats.downloadSpeedBps),
                icon = Icons.Default.ArrowDownward,
                iconTint = PurplePrimary,
                modifier = Modifier.weight(1f)
            )

            SpeedMetricCard(
                title = "ИСХОДЯЩИЙ ТРАФИК",
                value = formatBytes(stats.bytesUploaded),
                subtext = formatSpeed(stats.uploadSpeedBps),
                icon = Icons.Default.ArrowUpward,
                iconTint = NeonGreen,
                modifier = Modifier.weight(1f)
            )
        }

        // Telegram 1-Tap Connection Card
        TelegramConnectCard(config = config)

        // Live Traffic Speed Chart
        TrafficChart(speedPoints = speedHistory)

        // Live Terminal Log Card (Matching Bold Typography Theme)
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1B1F)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "МОНИТОР ЛОГОВ ПРОКСИ",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE6E1E5).copy(alpha = 0.6f),
                        letterSpacing = 1.2.sp
                    )

                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (stats.isRunning) NeonGreenTerminal else Color.Gray)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = if (stats.isRunning) "[СИСТЕМА] MTProto WS Сервис активен на порту ${config.localPort}" else "[СИСТЕМА] Сервис прокси выключен",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFFE6E1E5)
                    )
                    Text(
                        text = if (stats.isRunning) "[WSS] Назначение: ${config.customWsUrl.ifEmpty { activeDc?.wsUrl ?: "web.telegram.org" }}" else "[КОНФИГ] SOCKS5 и MTProto слушатели готовы",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFFD0BCFF)
                    )
                }
            }
        }
    }
}

@Composable
fun SpeedMetricCard(
    title: String,
    value: String,
    subtext: String,
    icon: ImageVector,
    iconTint: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier.border(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
            RoundedCornerShape(24.dp)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = subtext,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

fun formatSpeed(bps: Long): String {
    return when {
        bps >= 1024 * 1024 -> String.format("%.1f MB/s", bps / (1024.0 * 1024.0))
        bps >= 1024 -> String.format("%.1f KB/s", bps / 1024.0)
        else -> "$bps B/s"
    }
}

fun formatBytes(bytes: Long): String {
    return when {
        bytes >= 1024 * 1024 * 1024 -> String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
        bytes >= 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
        bytes >= 1024 -> String.format("%.0f KB", bytes / 1024.0)
        else -> "$bytes B"
    }
}

