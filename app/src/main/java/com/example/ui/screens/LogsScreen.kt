package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.proxy.LogEntry
import com.example.proxy.LogLevel
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonGreenTerminal
import com.example.ui.theme.NeonOrange
import com.example.ui.theme.NeonRed
import com.example.ui.theme.PurpleLightContainer
import com.example.ui.theme.PurplePrimary
import com.example.ui.theme.TelegramBlue

@Composable
fun LogsScreen(
    logs: List<LogEntry>,
    searchQuery: String,
    selectedLevel: LogLevel?,
    onSearchChange: (String) -> Unit,
    onLevelSelect: (LogLevel?) -> Unit,
    onClearLogs: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Логи Прокси",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.weight(1f))

            // Copy Logs
            IconButton(
                onClick = {
                    val allText = logs.joinToString("\n") { "[${it.formattedTime}] [${it.level}] ${it.message} ${it.details ?: ""}" }
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("TG WS Proxy Logs", allText))
                    Toast.makeText(context, "Логи скопированы в буфер обмена", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.testTag("copy_logs_button")
            ) {
                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Скопировать логи", tint = PurplePrimary)
            }

            // Clear Logs
            IconButton(
                onClick = onClearLogs,
                modifier = Modifier.testTag("clear_logs_button")
            ) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = "Очистить логи", tint = NeonRed)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Search Input
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("Фильтр логов...") },
            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(imageVector = Icons.Default.Clear, contentDescription = "Очистить поиск")
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().testTag("logs_search_input")
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Level Filter Chips
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            FilterChip(
                selected = selectedLevel == null,
                onClick = { onLevelSelect(null) },
                label = { Text("ВСЕ (${logs.size})") }
            )
            FilterChip(
                selected = selectedLevel == LogLevel.SUCCESS,
                onClick = { onLevelSelect(LogLevel.SUCCESS) },
                label = { Text("УСПЕХ") },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = NeonGreen.copy(alpha = 0.2f))
            )
            FilterChip(
                selected = selectedLevel == LogLevel.INFO,
                onClick = { onLevelSelect(LogLevel.INFO) },
                label = { Text("ИНФО") },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = TelegramBlue.copy(alpha = 0.2f))
            )
            FilterChip(
                selected = selectedLevel == LogLevel.ERROR,
                onClick = { onLevelSelect(LogLevel.ERROR) },
                label = { Text("ОШИБКИ") },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = NeonRed.copy(alpha = 0.2f))
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Logs Terminal Box
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(24.dp))
                .background(Color(0xFF1C1B1F), RoundedCornerShape(24.dp))
                .padding(16.dp)
        ) {
            if (logs.isEmpty()) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        text = "Логи пока отсутствуют.\nЗапустите прокси сервер для отслеживания соединений.",
                        color = Color(0xFFE6E1E5).copy(alpha = 0.7f),
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(logs, key = { it.id }) { log ->
                        LogItemRow(logEntry = log)
                    }
                }
            }
        }
    }
}

@Composable
fun LogItemRow(logEntry: LogEntry) {
    val levelColor = when (logEntry.level) {
        LogLevel.SUCCESS -> NeonGreen
        LogLevel.INFO -> TelegramBlue
        LogLevel.WARN -> NeonOrange
        LogLevel.ERROR -> NeonRed
    }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = logEntry.formattedTime,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "[${logEntry.level}]",
                color = levelColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = logEntry.message,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.weight(1f)
            )
        }

        logEntry.details?.let { details ->
            Text(
                text = "└─ $details",
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(start = 70.dp)
            )
        }
    }
}
