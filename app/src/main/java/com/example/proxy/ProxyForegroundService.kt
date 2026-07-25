package com.example.proxy

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ProxyForegroundService : Service() {

    inner class LocalBinder : Binder() {
        fun getService(): ProxyForegroundService = this@ProxyForegroundService
    }

    private val binder = LocalBinder()
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var statsCollectorJob: Job? = null

    var proxyServer: TgWsProxyServer? = null
        private set

    private val _logFlow = MutableSharedFlow<LogEntry>(extraBufferCapacity = 500)
    val logFlow: SharedFlow<LogEntry> = _logFlow.asSharedFlow()

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START

        when (action) {
            ACTION_START -> {
                startForegroundWithNotification("TG WS Proxy Starting...", "Initializing SOCKS5 WebSocket Engine")
                if (proxyServer == null) {
                    val defaultConfig = ProxyConfig()
                    initProxyServer(defaultConfig)
                }
                proxyServer?.start()
            }
            ACTION_STOP -> {
                proxyServer?.stop()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_STICKY
    }

    fun initProxyServer(config: ProxyConfig) {
        if (proxyServer == null) {
            proxyServer = TgWsProxyServer(config) { logEntry ->
                _logFlow.tryEmit(logEntry)
            }
            observeStats()
        } else {
            proxyServer?.updateConfig(config)
        }
    }

    private fun observeStats() {
        statsCollectorJob?.cancel()
        statsCollectorJob = scope.launch {
            proxyServer?.stats?.collectLatest { stats ->
                if (stats.isRunning) {
                    val upFormatted = formatSpeed(stats.uploadSpeedBps)
                    val downFormatted = formatSpeed(stats.downloadSpeedBps)
                    val title = "TG WS Proxy: Listening on 127.0.0.1"
                    val content = "↓ $downFormatted | ↑ $upFormatted | Active Conns: ${stats.activeConnections}"
                    updateNotification(title, content)
                }
            }
        }
    }

    private fun startForegroundWithNotification(title: String, content: String) {
        val notification = buildNotification(title, content)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                    } else {
                        0
                    }
                )
            } catch (e: Exception) {
                startForeground(NOTIFICATION_ID, notification)
            }
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotification(title: String, content: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, buildNotification(title, content))
    }

    private fun buildNotification(title: String, content: String): Notification {
        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val mainIntent = Intent(this, MainActivity::class.java)
        val pMainIntent = PendingIntent.getActivity(this, 0, mainIntent, pendingIntentFlags)

        val stopIntent = Intent(this, ProxyForegroundService::class.java).apply {
            action = ACTION_STOP
        }
        val pStopIntent = PendingIntent.getService(this, 1, stopIntent, pendingIntentFlags)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pMainIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop Proxy", pStopIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "TG WS Proxy Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows TG WS Proxy running status and connection speeds"
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun formatSpeed(bps: Long): String {
        return when {
            bps >= 1024 * 1024 -> String.format("%.1f MB/s", bps / (1024.0 * 1024.0))
            bps >= 1024 -> String.format("%.1f KB/s", bps / 1024.0)
            else -> "$bps B/s"
        }
    }

    override fun onDestroy() {

        proxyServer?.stop()
        statsCollectorJob?.cancel()
        super.onDestroy()
    }

    companion object {
        const val CHANNEL_ID = "tg_ws_proxy_service_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "com.example.proxy.ACTION_START"
        const val ACTION_STOP = "com.example.proxy.ACTION_STOP"
    }
}
