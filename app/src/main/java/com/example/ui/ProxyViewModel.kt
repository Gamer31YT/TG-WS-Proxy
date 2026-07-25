package com.example.ui

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.PreferencesManager
import com.example.data.PresetEntity
import com.example.proxy.DcPreset
import com.example.proxy.LogEntry
import com.example.proxy.LogLevel
import com.example.proxy.ProxyConfig
import com.example.proxy.ProxyForegroundService
import com.example.proxy.ProxyStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.LinkedList

data class SpeedPoint(
    val timestampMs: Long,
    val downloadBps: Long,
    val uploadBps: Long
)

class ProxyViewModel(application: Application) : AndroidViewModel(application) {

    private val prefsManager = PreferencesManager(application)
    private val db = AppDatabase.getDatabase(application)
    private val presetDao = db.presetDao()

    private var service: ProxyForegroundService? = null
    private var isBound = false

    private val _proxyConfig = MutableStateFlow(prefsManager.loadConfig())
    val proxyConfig: StateFlow<ProxyConfig> = _proxyConfig.asStateFlow()

    private val _proxyStats = MutableStateFlow(ProxyStats())
    val proxyStats: StateFlow<ProxyStats> = _proxyStats.asStateFlow()

    private val _speedHistory = MutableStateFlow<List<SpeedPoint>>(emptyList())
    val speedHistory: StateFlow<List<SpeedPoint>> = _speedHistory.asStateFlow()

    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    private val _logFilter = MutableStateFlow("")
    val logFilter: StateFlow<String> = _logFilter.asStateFlow()

    private val _logLevelFilter = MutableStateFlow<LogLevel?>(null)
    val logLevelFilter: StateFlow<LogLevel?> = _logLevelFilter.asStateFlow()

    val filteredLogs: StateFlow<List<LogEntry>> = combine(_logs, _logFilter, _logLevelFilter) { list, query, level ->
        list.filter { entry ->
            val matchesQuery = query.isBlank() || entry.message.contains(query, ignoreCase = true) || entry.details?.contains(query, ignoreCase = true) == true
            val matchesLevel = level == null || entry.level == level
            matchesQuery && matchesLevel
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _presets = MutableStateFlow<List<DcPreset>>(DcPreset.OFFICIAL_PRESETS)
    val presets: StateFlow<List<DcPreset>> = _presets.asStateFlow()

    private val _isTestingPings = MutableStateFlow(false)
    val isTestingPings: StateFlow<Boolean> = _isTestingPings.asStateFlow()

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val localBinder = binder as? ProxyForegroundService.LocalBinder
            service = localBinder?.getService()
            isBound = true

            service?.initProxyServer(_proxyConfig.value)

            // Observe stats and logs from bound service
            viewModelScope.launch {
                service?.proxyServer?.stats?.collect { stats ->
                    _proxyStats.value = stats
                    updateSpeedHistory(stats.downloadSpeedBps, stats.uploadSpeedBps)
                }
            }

            viewModelScope.launch {
                service?.logFlow?.collect { logEntry ->
                    addLog(logEntry)
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            service = null
            isBound = false
        }
    }

    init {
        loadPresets()
        bindService()
    }

    fun bindService() {
        val intent = Intent(getApplication(), ProxyForegroundService::class.java)
        getApplication<Application>().startService(intent)
        getApplication<Application>().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    fun toggleProxy() {
        val currentRunning = _proxyStats.value.isRunning
        val context = getApplication<Application>()
        val intent = Intent(context, ProxyForegroundService::class.java)

        if (currentRunning) {
            intent.action = ProxyForegroundService.ACTION_STOP
            context.startService(intent)
            _proxyStats.update { it.copy(isRunning = false) }
        } else {
            intent.action = ProxyForegroundService.ACTION_START
            context.startService(intent)
            service?.proxyServer?.start()
        }
    }

    fun updateConfig(newConfig: ProxyConfig) {
        _proxyConfig.value = newConfig
        prefsManager.saveConfig(newConfig)
        service?.proxyServer?.updateConfig(newConfig)
    }

    fun selectPreset(presetId: String) {
        val newConfig = _proxyConfig.value.copy(selectedDcId = presetId, customWsUrl = "")
        updateConfig(newConfig)
    }

    fun setCustomWsUrl(url: String) {
        val newConfig = _proxyConfig.value.copy(customWsUrl = url, selectedDcId = "custom")
        updateConfig(newConfig)
    }

    fun addCustomPreset(name: String, wsUrl: String, location: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val newEntity = PresetEntity(
                id = "custom_${System.currentTimeMillis()}",
                name = name,
                dcNumber = 0,
                location = location.ifBlank { "Custom Server" },
                wsUrl = wsUrl,
                flag = "⚡",
                isCustom = true
            )
            presetDao.insertPreset(newEntity)
            loadPresets()
        }
    }

    fun deletePreset(presetId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val entities = presetDao.getAllPresets()
            entities.find { it.id == presetId && it.isCustom }?.let {
                presetDao.deletePreset(it)
                loadPresets()
            }
        }
    }

    fun testAllPings() {
        if (_isTestingPings.value) return
        viewModelScope.launch(Dispatchers.IO) {
            _isTestingPings.value = true
            val proxy = service?.proxyServer ?: com.example.proxy.TgWsProxyServer(_proxyConfig.value)
            val updatedPresets = _presets.value.map { preset ->
                val ping = try {
                    proxy.measurePing(preset.wsUrl)
                } catch (e: Exception) {
                    -1L
                }
                preset.copy(pingMs = if (ping > 0) ping else null)
            }
            _presets.value = updatedPresets
            _isTestingPings.value = false
        }
    }

    fun setLogFilter(query: String) {
        _logFilter.value = query
    }

    fun setLogLevelFilter(level: LogLevel?) {
        _logLevelFilter.value = level
    }

    fun clearLogs() {
        _logs.value = emptyList()
    }

    private fun addLog(entry: LogEntry) {
        _logs.update { current ->
            val list = LinkedList(current)
            list.addFirst(entry)
            if (list.size > 300) list.removeLast()
            list
        }
    }

    private fun updateSpeedHistory(downloadBps: Long, uploadBps: Long) {
        val now = System.currentTimeMillis()
        _speedHistory.update { current ->
            val list = current.toMutableList()
            list.add(SpeedPoint(now, downloadBps, uploadBps))
            if (list.size > 30) list.removeAt(0)
            list
        }
    }

    private fun loadPresets() {
        viewModelScope.launch(Dispatchers.IO) {
            presetDao.getAllPresetsFlow().collect { entities ->
                if (entities.isEmpty()) {
                    val defaultEntities = DcPreset.OFFICIAL_PRESETS.map {
                        PresetEntity.fromDcPreset(it, isCustom = false)
                    }
                    presetDao.insertPresets(defaultEntities)
                    _presets.value = DcPreset.OFFICIAL_PRESETS
                } else {
                    val merged = entities.map { entity ->
                        val official = DcPreset.OFFICIAL_PRESETS.find { it.id == entity.id }
                        if (official != null && !entity.isCustom) {
                            official
                        } else {
                            entity.toDcPreset()
                        }
                    }
                    _presets.value = merged
                }
            }
        }
    }

    override fun onCleared() {
        if (isBound) {
            try {
                getApplication<Application>().unbindService(serviceConnection)
            } catch (_: Exception) {}
            isBound = false
        }
        super.onCleared()
    }
}
