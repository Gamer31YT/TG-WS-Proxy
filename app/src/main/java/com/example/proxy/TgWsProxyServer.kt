package com.example.proxy

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import okio.ByteString.Companion.toByteString
import java.io.InputStream
import java.io.OutputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

class TgWsProxyServer(
    private var config: ProxyConfig,
    private val onLog: (LogEntry) -> Unit = {}
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null
    private var statsJob: Job? = null

    private val _stats = MutableStateFlow(ProxyStats())
    val stats: StateFlow<ProxyStats> = _stats.asStateFlow()

    private val activeConnectionsCount = AtomicInteger(0)
    private val totalConnectionsCount = AtomicLong(0L)
    private val bytesUploadedTotal = AtomicLong(0L)
    private val bytesDownloadedTotal = AtomicLong(0L)

    private var lastUploadedBytes = 0L
    private var lastDownloadedBytes = 0L
    private var lastStatsTimeMs = System.currentTimeMillis()

    private val okHttpClient: OkHttpClient by lazy {
        val builder = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.MILLISECONDS) // WebSocket keepalive
            .writeTimeout(10, TimeUnit.SECONDS)
            .pingInterval(20, TimeUnit.SECONDS)

        when (config.dohProvider) {
            "Cloudflare" -> {
                builder.dns(object : Dns {
                    override fun lookup(hostname: String): List<InetAddress> {
                        return try {
                            InetAddress.getAllByName(hostname).toList()
                        } catch (e: Exception) {
                            Dns.SYSTEM.lookup(hostname)
                        }
                    }
                })
            }
            else -> {
                builder.dns(Dns.SYSTEM)
            }
        }
        builder.build()
    }

    fun updateConfig(newConfig: ProxyConfig) {
        val portChanged = config.localPort != newConfig.localPort || config.listenAddress != newConfig.listenAddress
        config = newConfig
        _stats.update { it.copy(activeTargetUrl = config.getEffectiveWsUrl()) }
        if (_stats.value.isRunning && portChanged) {
            stop()
            start()
        }
    }

    @Synchronized
    fun start(): Boolean {
        if (serverJob?.isActive == true) return true

        return try {
            val bindAddress = InetAddress.getByName(config.listenAddress)
            serverSocket = ServerSocket().apply {
                reuseAddress = true
                bind(InetSocketAddress(bindAddress, config.localPort))
            }

            val startTime = System.currentTimeMillis()
            _stats.update {
                ProxyStats(
                    isRunning = true,
                    startTimeMs = startTime,
                    activeTargetUrl = config.getEffectiveWsUrl()
                )
            }

            log(LogLevel.SUCCESS, "Server started on ${config.listenAddress}:${config.localPort}", "WS Target: ${config.getEffectiveWsUrl()}")

            serverJob = scope.launch {
                acceptLoop()
            }

            startStatsMonitoring()
            true
        } catch (e: Exception) {
            log(LogLevel.ERROR, "Failed to start proxy server: ${e.message}", e.stackTraceToString())
            stop()
            false
        }
    }

    @Synchronized
    fun stop() {
        serverJob?.cancel()
        serverJob = null
        statsJob?.cancel()
        statsJob = null

        try {
            serverSocket?.close()
        } catch (_: Exception) {}
        serverSocket = null

        _stats.update {
            ProxyStats(
                isRunning = false,
                activeConnections = 0,
                uploadSpeedBps = 0,
                downloadSpeedBps = 0
            )
        }

        log(LogLevel.INFO, "Proxy server stopped")
    }

    private suspend fun acceptLoop() {
        val server = serverSocket ?: return
        while (serverJob?.isActive == true && !server.isClosed) {
            try {
                val clientSocket = server.accept()
                clientSocket.tcpNoDelay = true
                clientSocket.soTimeout = 30000 // 30 sec timeout for initial handshake

                totalConnectionsCount.incrementAndGet()
                activeConnectionsCount.incrementAndGet()

                scope.launch {
                    handleClient(clientSocket)
                }
            } catch (e: Exception) {
                if (serverJob?.isActive == true && !server.isClosed) {
                    Log.e("TgWsProxy", "Error accepting client", e)
                }
            }
        }
    }

    private fun handleClient(clientSocket: Socket) {
        val clientIp = clientSocket.remoteSocketAddress.toString()
        try {
            val input = clientSocket.getInputStream()
            val output = clientSocket.getOutputStream()

            // Check if SOCKS5 or HTTP CONNECT
            val firstByte = input.read()
            if (firstByte == -1) {
                closeQuietly(clientSocket)
                activeConnectionsCount.decrementAndGet()
                return
            }

            if (firstByte == 0x05) {
                // SOCKS5 Protocol
                handleSocks5(clientSocket, input, output, clientIp)
            } else if (firstByte == 'C'.code || firstByte == 'G'.code || firstByte == 'P'.code) {
                // HTTP CONNECT Protocol
                handleHttpConnect(clientSocket, input, output, firstByte, clientIp)
            } else {
                // Native MTProto Obfuscated Protocol / Direct Client Stream
                handleDirectMtproto(clientSocket, input, output, firstByte, clientIp)
            }
        } catch (e: Exception) {
            log(LogLevel.ERROR, "Client handling error ($clientIp): ${e.message}")
            closeQuietly(clientSocket)
            activeConnectionsCount.decrementAndGet()
        }
    }

    private fun handleSocks5(socket: Socket, input: InputStream, output: OutputStream, clientIp: String) {
        // Method selection
        val numMethods = input.read()
        if (numMethods <= 0) {
            closeQuietly(socket)
            activeConnectionsCount.decrementAndGet()
            return
        }
        val methods = ByteArray(numMethods)
        input.readFully(methods)

        // Reply method (0x00 NO AUTH)
        output.write(byteArrayOf(0x05, 0x00))
        output.flush()

        // Read request header
        val ver = input.read()
        val cmd = input.read()
        val rsv = input.read()
        val atyp = input.read()

        if (ver != 0x05 || cmd != 0x01) { // Only CMD 0x01 CONNECT supported
            output.write(byteArrayOf(0x05, 0x07, 0x00, 0x01, 0, 0, 0, 0, 0, 0)) // Command not supported
            output.flush()
            closeQuietly(socket)
            activeConnectionsCount.decrementAndGet()
            return
        }

        var targetHost = ""
        var targetPort = 0
        var dcNumber: Int? = null

        when (atyp) {
            0x01 -> { // IPv4
                val ipBytes = ByteArray(4)
                input.readFully(ipBytes)
                targetHost = InetAddress.getByAddress(ipBytes).hostAddress ?: ""
                dcNumber = detectDcFromIp(ipBytes)
            }
            0x03 -> { // Domain Name
                val len = input.read()
                val domainBytes = ByteArray(len)
                input.readFully(domainBytes)
                targetHost = String(domainBytes, Charsets.UTF_8)
            }
            0x04 -> { // IPv6
                val ip6Bytes = ByteArray(16)
                input.readFully(ip6Bytes)
                targetHost = InetAddress.getByAddress(ip6Bytes).hostAddress ?: ""
            }
            else -> {
                closeQuietly(socket)
                activeConnectionsCount.decrementAndGet()
                return
            }
        }

        val portBytes = ByteArray(2)
        input.readFully(portBytes)
        targetPort = ((portBytes[0].toInt() and 0xFF) shl 8) or (portBytes[1].toInt() and 0xFF)

        // Send SOCKS5 Success response: [ver=5, rep=0, rsv=0, atyp=1, addr=0.0.0.0, port=0]
        output.write(byteArrayOf(0x05, 0x00, 0x00, 0x01, 0, 0, 0, 0, 0, 0))
        output.flush()

        log(LogLevel.INFO, "SOCKS5 CONNECT $targetHost:$targetPort", "Client: $clientIp ${dcNumber?.let { "(Detected DC$it)" } ?: ""}")

        val wsUrl = config.getEffectiveWsUrl(dcNumber)
        pipeTcpToWebSocket(socket, input, output, wsUrl, targetHost, targetPort)
    }

    private fun handleHttpConnect(
        socket: Socket,
        input: InputStream,
        output: OutputStream,
        firstByteChar: Int,
        clientIp: String
    ) {
        val lineBuilder = StringBuilder()
        lineBuilder.append(firstByteChar.toChar())

        var b = input.read()
        while (b != -1 && b != '\n'.code) {
            if (b != '\r'.code) lineBuilder.append(b.toChar())
            b = input.read()
        }

        val requestLine = lineBuilder.toString()
        val parts = requestLine.split(" ")
        var targetHost = ""
        var targetPort = 443

        if (parts.size >= 2) {
            val hostPort = parts[1].split(":")
            targetHost = hostPort[0]
            if (hostPort.size > 1) {
                targetPort = hostPort[1].toIntOrNull() ?: 443
            }
        }

        // Read remaining HTTP headers until empty line
        while (true) {
            val hLine = readLine(input)
            if (hLine.isBlank()) break
        }

        // Reply 200 Connection Established
        output.write("HTTP/1.1 200 Connection Established\r\n\r\n".toByteArray(Charsets.UTF_8))
        output.flush()

        log(LogLevel.INFO, "HTTP CONNECT $targetHost:$targetPort", "Client: $clientIp")

        val wsUrl = config.getEffectiveWsUrl(null)
        pipeTcpToWebSocket(socket, input, output, wsUrl, targetHost, targetPort)
    }

    private fun handleDirectMtproto(
        socket: Socket,
        input: InputStream,
        output: OutputStream,
        firstByte: Int,
        clientIp: String
    ) {
        log(LogLevel.INFO, "MTProto Obfuscated client stream connected", "Client: $clientIp (First byte: 0x${Integer.toHexString(firstByte)})")
        val wsUrl = config.getEffectiveWsUrl(null)
        pipeTcpToWebSocket(socket, input, output, wsUrl, "web.telegram.org", 443, initialByte = firstByte)
    }

    private fun pipeTcpToWebSocket(
        clientSocket: Socket,
        input: InputStream,
        output: OutputStream,
        wsUrl: String,
        targetHost: String,
        targetPort: Int,
        initialByte: Int? = null
    ) {
        val wsRequest = Request.Builder()
            .url(wsUrl)
            .apply {
                if (config.customUserAgent.isNotBlank()) {
                    header("User-Agent", config.customUserAgent)
                } else {
                    header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
                }
                if (config.customHostHeader.isNotBlank()) {
                    header("Host", config.customHostHeader)
                }
                header("Origin", "https://web.telegram.org")
                header("Sec-WebSocket-Protocol", "binary")
            }
            .build()

        var webSocketRef: WebSocket? = null
        val wsConnected = Object()
        var isWsOpen = false
        var wsError: Throwable? = null

        val listener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                synchronized(wsConnected) {
                    isWsOpen = true
                    webSocketRef = webSocket
                    wsConnected.notifyAll()
                }
                log(LogLevel.SUCCESS, "WebSocket connected to $wsUrl", "Target: $targetHost:$targetPort")
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                try {
                    val byteArray = bytes.toByteArray()
                    output.write(byteArray)
                    output.flush()
                    bytesDownloadedTotal.addAndGet(byteArray.size.toLong())
                } catch (e: Exception) {
                    closeQuietly(clientSocket)
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                synchronized(wsConnected) {
                    wsError = t
                    wsConnected.notifyAll()
                }
                log(LogLevel.WARN, "WebSocket connection failed (${t.message}), switching to Direct TCP relay")
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                closeQuietly(clientSocket)
            }
        }

        val ws = okHttpClient.newWebSocket(wsRequest, listener)

        // Wait for WS connection (up to 3.5 sec)
        synchronized(wsConnected) {
            if (!isWsOpen && wsError == null) {
                try {
                    wsConnected.wait(3500)
                } catch (_: Exception) {}
            }
        }

        if (isWsOpen && webSocketRef != null) {
            val activeWs = webSocketRef!!
            clientSocket.soTimeout = 0 // Remove timeout during active proxying

            // Send initial byte if present
            if (initialByte != null) {
                activeWs.send(byteArrayOf(initialByte.toByte()).toByteString())
                bytesUploadedTotal.addAndGet(1L)
            }

            val buffer = ByteArray(16384)
            try {
                while (true) {
                    val bytesRead = input.read(buffer)
                    if (bytesRead == -1) break

                    val byteString = buffer.toByteString(0, bytesRead)
                    val sent = activeWs.send(byteString)
                    if (!sent) break
                    bytesUploadedTotal.addAndGet(bytesRead.toLong())
                }
            } catch (_: Exception) {
            } finally {
                activeWs.close(1000, "Client closed connection")
                closeQuietly(clientSocket)
                activeConnectionsCount.decrementAndGet()
            }
        } else {
            // Direct TCP Socket Relay Fallback (for networks blocking WSS)
            pipeDirectTcpFallback(clientSocket, input, output, targetHost, targetPort, initialByte)
        }
    }

    private fun pipeDirectTcpFallback(
        clientSocket: Socket,
        input: InputStream,
        output: OutputStream,
        targetHost: String,
        targetPort: Int,
        initialByte: Int?
    ) {
        var targetSocket: Socket? = null
        try {
            val resolvedHost = if (targetHost.isBlank() || targetHost == "0.0.0.0") "149.154.167.50" else targetHost
            val resolvedPort = if (targetPort <= 0) 443 else targetPort

            targetSocket = Socket()
            targetSocket.connect(InetSocketAddress(resolvedHost, resolvedPort), 5000)
            targetSocket.tcpNoDelay = true
            targetSocket.soTimeout = 0

            val targetOut = targetSocket.getOutputStream()
            val targetIn = targetSocket.getInputStream()

            log(LogLevel.SUCCESS, "Direct TCP relay active -> $resolvedHost:$resolvedPort")

            // Send initial byte
            if (initialByte != null) {
                targetOut.write(initialByte)
                targetOut.flush()
                bytesUploadedTotal.addAndGet(1L)
            }

            // Target -> Client Thread
            val downThread = Thread {
                try {
                    val buf = ByteArray(16384)
                    while (true) {
                        val len = targetIn.read(buf)
                        if (len == -1) break
                        output.write(buf, 0, len)
                        output.flush()
                        bytesDownloadedTotal.addAndGet(len.toLong())
                    }
                } catch (_: Exception) {}
            }
            downThread.isDaemon = true
            downThread.start()

            // Client -> Target Loop
            val buf = ByteArray(16384)
            while (true) {
                val len = input.read(buf)
                if (len == -1) break
                targetOut.write(buf, 0, len)
                targetOut.flush()
                bytesUploadedTotal.addAndGet(len.toLong())
            }
        } catch (e: Exception) {
            log(LogLevel.ERROR, "Direct TCP Relay Error: ${e.message}")
        } finally {
            closeQuietly(targetSocket)
            closeQuietly(clientSocket)
            activeConnectionsCount.decrementAndGet()
        }
    }

    private fun detectDcFromIp(ipBytes: ByteArray): Int? {
        if (ipBytes.size != 4) return null
        val b0 = ipBytes[0].toInt() and 0xFF
        val b1 = ipBytes[1].toInt() and 0xFF
        val b2 = ipBytes[2].toInt() and 0xFF

        // Known Telegram IP ranges:
        // 149.154.160.0/20 (149.154.160.0 – 149.154.175.255)
        // 91.108.4.0/22, 91.108.8.0/22, 91.108.12.0/22, 91.108.16.0/22, 91.108.56.0/22
        if (b0 == 149 && b1 == 154) {
            return when (b2) {
                175 -> 1
                167 -> 2
                171 -> 3
                165 -> 4
                else -> 2
            }
        }
        if (b0 == 91 && b1 == 108) {
            return when (b2) {
                4, 5, 6, 7 -> 1
                8, 9, 10, 11 -> 2
                12, 13, 14, 15 -> 3
                16, 17, 18, 19 -> 4
                56, 57, 58, 59 -> 5
                else -> 2
            }
        }
        return null
    }

    private fun startStatsMonitoring() {
        statsJob = scope.launch {
            while (serverJob?.isActive == true) {
                delay(1000)
                val now = System.currentTimeMillis()
                val deltaSec = (now - lastStatsTimeMs) / 1000.0
                lastStatsTimeMs = now

                val currentUploaded = bytesUploadedTotal.get()
                val currentDownloaded = bytesDownloadedTotal.get()

                val uploadSpeed = if (deltaSec > 0) ((currentUploaded - lastUploadedBytes) / deltaSec).toLong() else 0L
                val downloadSpeed = if (deltaSec > 0) ((currentDownloaded - lastDownloadedBytes) / deltaSec).toLong() else 0L

                lastUploadedBytes = currentUploaded
                lastDownloadedBytes = currentDownloaded

                _stats.update {
                    it.copy(
                        bytesUploaded = currentUploaded,
                        bytesDownloaded = currentDownloaded,
                        uploadSpeedBps = uploadSpeed,
                        downloadSpeedBps = downloadSpeed,
                        activeConnections = activeConnectionsCount.get(),
                        totalConnectionsHandled = totalConnectionsCount.get()
                    )
                }
            }
        }
    }

    private fun readLine(input: InputStream): String {
        val sb = StringBuilder()
        var b = input.read()
        while (b != -1 && b != '\n'.code) {
            if (b != '\r'.code) sb.append(b.toChar())
            b = input.read()
        }
        return sb.toString()
    }

    private fun InputStream.readFully(buffer: ByteArray) {
        var bytesRead = 0
        while (bytesRead < buffer.size) {
            val count = read(buffer, bytesRead, buffer.size - bytesRead)
            if (count == -1) break
            bytesRead += count
        }
    }

    private fun closeQuietly(socket: Socket?) {
        try {
            socket?.close()
        } catch (_: Exception) {}
    }

    private fun log(level: LogLevel, message: String, details: String? = null) {
        onLog(LogEntry(level = level, message = message, details = details))
    }

    suspend fun measurePing(wsUrl: String): Long {
        val cleanUrl = wsUrl.trim()
        val uri = try { java.net.URI(cleanUrl) } catch (e: Exception) { null }
        val host = uri?.host ?: "web.telegram.org"
        val port = if (uri?.port != null && uri.port != -1) uri.port else 443

        var pingMs: Long = -1

        // Attempt 1: Fast TCP Socket Connect to Host & Port 443
        val tcpStart = System.currentTimeMillis()
        try {
            val socket = Socket()
            socket.connect(InetSocketAddress(host, port), 2000)
            pingMs = System.currentTimeMillis() - tcpStart
            socket.close()
        } catch (_: Exception) {
            pingMs = -1
        }

        // Attempt 2: Fallback to Known DC IP if Host DNS failed
        if (pingMs <= 0) {
            val fallbackIp = when {
                host.contains("pluto") -> "149.154.175.50"
                host.contains("venus") -> "149.154.167.50"
                host.contains("aurora") -> "149.154.175.100"
                host.contains("vestal") -> "149.154.167.91"
                host.contains("flora") -> "91.108.56.130"
                else -> "149.154.167.50"
            }
            val ipStart = System.currentTimeMillis()
            try {
                val socket = Socket()
                socket.connect(InetSocketAddress(fallbackIp, 443), 2000)
                pingMs = System.currentTimeMillis() - ipStart
                socket.close()
            } catch (_: Exception) {
                pingMs = -1
            }
        }

        return pingMs
    }
}
