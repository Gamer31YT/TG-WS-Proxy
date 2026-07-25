package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.proxy.ProxyConfig

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("tg_ws_proxy_prefs", Context.MODE_PRIVATE)

    fun saveConfig(config: ProxyConfig) {
        prefs.edit().apply {
            putInt("localPort", config.localPort)
            putString("listenAddress", config.listenAddress)
            putString("selectedDcId", config.selectedDcId)
            putString("customWsUrl", config.customWsUrl)
            putString("customUserAgent", config.customUserAgent)
            putString("customHostHeader", config.customHostHeader)
            putBoolean("enableSocksAuth", config.enableSocksAuth)
            putString("socksUser", config.socksUser)
            putString("socksPass", config.socksPass)
            putString("dohProvider", config.dohProvider)
            putBoolean("autoStartOnBoot", config.autoStartOnBoot)
            apply()
        }
    }

    fun loadConfig(): ProxyConfig {
        return ProxyConfig(
            localPort = prefs.getInt("localPort", 1080),
            listenAddress = prefs.getString("listenAddress", "127.0.0.1") ?: "127.0.0.1",
            selectedDcId = prefs.getString("selectedDcId", "auto") ?: "auto",
            customWsUrl = prefs.getString("customWsUrl", "") ?: "",
            customUserAgent = prefs.getString("customUserAgent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36") ?: "",
            customHostHeader = prefs.getString("customHostHeader", "") ?: "",
            enableSocksAuth = prefs.getBoolean("enableSocksAuth", false),
            socksUser = prefs.getString("socksUser", "") ?: "",
            socksPass = prefs.getString("socksPass", "") ?: "",
            dohProvider = prefs.getString("dohProvider", "Default") ?: "Default",
            autoStartOnBoot = prefs.getBoolean("autoStartOnBoot", false)
        )
    }
}
