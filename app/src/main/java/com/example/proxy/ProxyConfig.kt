package com.example.proxy

data class DcPreset(
    val id: String,
    val name: String,
    val dcNumber: Int,
    val location: String,
    val wsUrl: String,
    val flag: String,
    var pingMs: Long? = null
) {
    companion object {
        val OFFICIAL_PRESETS = listOf(
            DcPreset("auto", "Telegram Web (Основной)", 0, "Глобальная CDN сеть", "wss://web.telegram.org/apiws", "⚡"),
            DcPreset("dc1", "DC1 (Pluto)", 1, "Майами, США", "wss://pluto.web.telegram.org/apiws", "🇺🇸"),
            DcPreset("dc2", "DC2 (Venus)", 2, "Амстердам, Нидерланды", "wss://venus.telegram.org/apiws", "🇳🇱"),
            DcPreset("dc3", "DC3 (Aurora)", 3, "Майами, США", "wss://aurora.web.telegram.org/apiws", "🇺🇸"),
            DcPreset("dc4", "DC4 (Vestal)", 4, "Амстердам, Нидерланды", "wss://vestal.web.telegram.org/apiws", "🇳🇱"),
            DcPreset("dc5", "DC5 (Flora)", 5, "Сингапур", "wss://flora.web.telegram.org/apiws", "🇸🇬")
        )
    }
}

data class ProxyConfig(
    val localPort: Int = 1080,
    val listenAddress: String = "127.0.0.1",
    val selectedDcId: String = "auto",
    val customWsUrl: String = "",
    val customUserAgent: String = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36",
    val customHostHeader: String = "",
    val enableSocksAuth: Boolean = false,
    val socksUser: String = "",
    val socksPass: String = "",
    val dohProvider: String = "Default", // Default, Cloudflare, Google, Quad9
    val autoStartOnBoot: Boolean = false
) {
    fun getEffectiveWsUrl(requestedDc: Int? = null): String {
        if (customWsUrl.isNotBlank()) return customWsUrl
        if (requestedDc != null && requestedDc in 1..5) {
            DcPreset.OFFICIAL_PRESETS.find { it.dcNumber == requestedDc }?.let { return it.wsUrl }
        }
        DcPreset.OFFICIAL_PRESETS.find { it.id == selectedDcId }?.let { return it.wsUrl }
        return "wss://web.telegram.org/apiws"
    }
}
