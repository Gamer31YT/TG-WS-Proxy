package com.example.proxy

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class LogLevel {
    INFO, SUCCESS, WARN, ERROR
}

data class LogEntry(
    val id: Long = System.currentTimeMillis() + (0..999).random(),
    val timestampMs: Long = System.currentTimeMillis(),
    val level: LogLevel = LogLevel.INFO,
    val message: String,
    val details: String? = null
) {
    val formattedTime: String
        get() = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date(timestampMs))
}

data class ProxyStats(
    val isRunning: Boolean = false,
    val bytesUploaded: Long = 0L,
    val bytesDownloaded: Long = 0L,
    val uploadSpeedBps: Long = 0L,
    val downloadSpeedBps: Long = 0L,
    val activeConnections: Int = 0,
    val totalConnectionsHandled: Long = 0L,
    val latencyMs: Long? = null,
    val startTimeMs: Long? = null,
    val activeTargetUrl: String = ""
)
