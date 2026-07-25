package com.example.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Http
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Router
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.proxy.ProxyConfig
import com.example.ui.theme.PurplePrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    config: ProxyConfig,
    onSaveConfig: (ProxyConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    var localPortText by remember(config) { mutableStateOf(config.localPort.toString()) }
    var listenAddress by remember(config) { mutableStateOf(config.listenAddress) }
    var customUserAgent by remember(config) { mutableStateOf(config.customUserAgent) }
    var customHostHeader by remember(config) { mutableStateOf(config.customHostHeader) }
    var enableAuth by remember(config) { mutableStateOf(config.enableSocksAuth) }
    var socksUser by remember(config) { mutableStateOf(config.socksUser) }
    var socksPass by remember(config) { mutableStateOf(config.socksPass) }
    var dohProvider by remember(config) { mutableStateOf(config.dohProvider) }

    var dohExpanded by remember { mutableStateOf(false) }
    val dohOptions = listOf("Default", "Cloudflare", "Google", "Quad9")

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Настройки Прокси",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        // Local Server Section
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Router, contentDescription = null, tint = PurplePrimary)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Локальный Сокет Модуль",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedTextField(
                    value = localPortText,
                    onValueChange = { localPortText = it },
                    label = { Text("Локальный порт MTProto/SOCKS") },
                    placeholder = { Text("1080") },
                    modifier = Modifier.fillMaxWidth().testTag("local_port_input")
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Доступ в локальной сети (0.0.0.0)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (listenAddress == "0.0.0.0") "Слушает все сетевые интерфейсы" else "Только этот телефон (127.0.0.1)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Switch(
                        checked = listenAddress == "0.0.0.0",
                        onCheckedChange = { isLan ->
                            listenAddress = if (isLan) "0.0.0.0" else "127.0.0.1"
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = PurplePrimary)
                    )
                }
            }
        }

        // Custom HTTP Headers / Spoofing
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Http, contentDescription = null, tint = PurplePrimary)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "HTTP и WebSocket Заголовки",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedTextField(
                    value = customUserAgent,
                    onValueChange = { customUserAgent = it },
                    label = { Text("Заголовок User-Agent") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = customHostHeader,
                    onValueChange = { customHostHeader = it },
                    label = { Text("Заголовок Host / SNI (Опционально)") },
                    placeholder = { Text("web.telegram.org") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // DNS Over HTTPS
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Dns, contentDescription = null, tint = PurplePrimary)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Настройки DNS (DoH)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                ExposedDropdownMenuBox(
                    expanded = dohExpanded,
                    onExpandedChange = { dohExpanded = !dohExpanded }
                ) {
                    OutlinedTextField(
                        value = dohProvider,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Провайдер DNS") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dohExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = dohExpanded,
                        onDismissRequest = { dohExpanded = false }
                    ) {
                        dohOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    dohProvider = option
                                    dohExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // Save Settings Button
        Button(
            onClick = {
                val portInt = localPortText.toIntOrNull() ?: 1080
                val newConfig = config.copy(
                    localPort = portInt,
                    listenAddress = listenAddress,
                    customUserAgent = customUserAgent,
                    customHostHeader = customHostHeader,
                    enableSocksAuth = enableAuth,
                    socksUser = socksUser,
                    socksPass = socksPass,
                    dohProvider = dohProvider
                )
                onSaveConfig(newConfig)
            },
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("save_settings_button")
        ) {
            Text(
                text = "Сохранить настройки",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
