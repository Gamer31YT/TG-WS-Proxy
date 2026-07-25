package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.proxy.ProxyConfig
import com.example.ui.theme.PurpleDark
import com.example.ui.theme.PurpleLightContainer
import com.example.ui.theme.PurplePrimary
import com.example.ui.theme.TelegramBlue
import com.example.ui.theme.TelegramBlueDark

@Composable
fun TelegramConnectCard(
    config: ProxyConfig,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val host = if (config.listenAddress == "0.0.0.0") "127.0.0.1" else config.listenAddress
    val port = config.localPort

    // MTProto & SOCKS5 proxy deep links for Telegram
    val mtprotoLink = "tg://proxy?server=$host&port=$port"
    val telegramMtprotoUrl = "https://t.me/proxy?server=$host&port=$port"
    val telegramSocksUrl = "https://t.me/socks?server=$host&port=$port"

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(24.dp))
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.linearGradient(listOf(TelegramBlue, TelegramBlueDark))
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Telegram MTProto",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Прокси Telegram MTProto",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Text(
                        text = "Быстрое подключение • $host:$port",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Action: Launch Telegram Proxy Setup
            Button(
                onClick = {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(mtprotoLink))
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(telegramMtprotoUrl))
                            context.startActivity(intent)
                        } catch (e2: Exception) {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(telegramSocksUrl))
                                context.startActivity(intent)
                            } catch (e3: Exception) {
                                Toast.makeText(context, "Приложение Telegram не найдено", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TelegramBlue
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("apply_telegram_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Подключить MTProto в Telegram",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Copy Link Button
                OutlinedButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Telegram MTProto Proxy Link", telegramMtprotoUrl)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Ссылка скопирована в буфер обмена!", Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Скопировать", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Скопировать", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                // Share Button
                OutlinedButton(
                    onClick = {
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, "Подключайся к Telegram через рабочий MTProto Прокси: $telegramMtprotoUrl")
                            type = "text/plain"
                        }
                        val shareIntent = Intent.createChooser(sendIntent, "Поделиться прокси")
                        context.startActivity(shareIntent)
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = "Поделиться", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Поделиться", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
