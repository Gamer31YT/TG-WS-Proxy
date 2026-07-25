package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.proxy.DcPreset
import com.example.proxy.ProxyConfig
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonOrange
import com.example.ui.theme.NeonRed
import com.example.ui.theme.PurpleDark
import com.example.ui.theme.PurpleLightContainer
import com.example.ui.theme.PurplePrimary
import com.example.ui.theme.TelegramBlue

@Composable
fun DcServersScreen(
    presets: List<DcPreset>,
    config: ProxyConfig,
    isTestingPings: Boolean,
    onSelectPreset: (String) -> Unit,
    onTestPings: () -> Unit,
    onAddCustomPreset: (name: String, wsUrl: String, location: String) -> Unit,
    onDeletePreset: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header Bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = "Центры данных Telegram",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Выберите WebSocket сервер для проксирования",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Ping Test Button
                Button(
                    onClick = onTestPings,
                    enabled = !isTestingPings,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("ping_test_button")
                ) {
                    if (isTestingPings) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.NetworkCheck,
                            contentDescription = "Тест пинга",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Пинг", fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Presets List
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(presets, key = { it.id }) { preset ->
                    val isSelected = (config.selectedDcId == preset.id) && config.customWsUrl.isBlank()

                    DcPresetCard(
                        preset = preset,
                        isSelected = isSelected,
                        onSelect = { onSelectPreset(preset.id) },
                        onDelete = { onDeletePreset(preset.id) }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }

        // Add Custom Server FAB
        FloatingActionButton(
            onClick = { showAddDialog = true },
            containerColor = TelegramBlue,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("add_custom_server_fab")
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Add Custom WebSocket")
        }

        if (showAddDialog) {
            AddCustomPresetDialog(
                onDismiss = { showAddDialog = false },
                onAdd = { name, url, location ->
                    onAddCustomPreset(name, url, location)
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
fun DcPresetCard(
    preset: DcPreset,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    val isCustom = preset.id.startsWith("custom_")

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) PurpleLightContainer else MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) PurplePrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                shape = RoundedCornerShape(24.dp)
            )
            .clickable { onSelect() }
            .testTag("dc_preset_${preset.id}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(18.dp)
        ) {
            Text(
                text = preset.flag,
                fontSize = 28.sp
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = preset.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isSelected) PurpleDark else MaterialTheme.colorScheme.onSurface
                    )
                    if (isSelected) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Selected",
                            tint = PurplePrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Text(
                    text = preset.location,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isSelected) PurpleDark.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = preset.wsUrl,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSelected) PurplePrimary else MaterialTheme.colorScheme.primary
                )
            }

            // Ping badge
            preset.pingMs?.let { ping ->
                val pingColor = when {
                    ping < 100 -> NeonGreen
                    ping < 250 -> NeonOrange
                    else -> NeonRed
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(pingColor.copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "${ping} ms",
                        color = pingColor,
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp
                    )
                }
            }

            if (isCustom) {
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Custom Preset",
                        tint = NeonRed.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AddCustomPresetDialog(
    onDismiss: () -> Unit,
    onAdd: (name: String, url: String, location: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("wss://") }
    var location by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Добавить свой WS Сервер", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Название сервера") },
                    placeholder = { Text("например, Мой Рабочий WS Прокси") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("URL адрес WebSocket (WSS)") },
                    placeholder = { Text("wss://my-proxy.workers.dev/apiws") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Локация / Заметка (Опционально)") },
                    placeholder = { Text("Европа CDN") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && url.startsWith("ws")) {
                        onAdd(name, url, location)
                    }
                },
                enabled = name.isNotBlank() && url.length > 8
            ) {
                Text("Добавить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}
