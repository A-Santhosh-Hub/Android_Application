package com.sanden.data.transfer

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import android.util.Base64
import android.util.Log
import com.sanden.data.db.SyncMessageDao
import com.sanden.data.db.SyncMessageEntity
import com.sanden.data.db.VaultFileDao
import com.sanden.data.scanner.CategoryDetector
import com.sanden.data.storage.SafeFileOperations
import com.sanden.domain.repository.VaultRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder
import java.security.MessageDigest
import java.util.concurrent.CopyOnWriteArrayList
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Embedded LAN HTTP + WebSocket Continuity Hub inspired by LocalSend and KDE Connect.
 *
 * Serves the Liquid Glass Web Companion UI on port 53317, streams multi-GB uploads
 * directly into the active SanDen Vault, streams vault downloads to the laptop,
 * and pushes real-time chat & clipboard updates over WebSocket.
 */
@Singleton
class LanTransferHub @Inject constructor(
    @ApplicationContext private val context: Context,
    private val syncMessageDao: SyncMessageDao,
    private val vaultFileDao: VaultFileDao,
    private val vaultRepository: VaultRepository
) {

    companion object {
        private const val TAG = "LanTransferHub"
        const val DEFAULT_PORT = 54321
        private const val SERVICE_TYPE = "_sanden._tcp."
        private const val WS_GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11"
        private const val BUFFER_SIZE = 8192
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var serverSocket: ServerSocket? = null
    private var acceptJob: Job? = null
    private var nsdManager: NsdManager? = null
    private var registrationListener: NsdManager.RegistrationListener? = null

    private val wsClients = CopyOnWriteArrayList<OutputStream>()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _serverUrl = MutableStateFlow("")
    val serverUrl: StateFlow<String> = _serverUrl.asStateFlow()

    private val _connectedClients = MutableStateFlow(0)
    val connectedClients: StateFlow<Int> = _connectedClients.asStateFlow()

    fun start(port: Int = DEFAULT_PORT) {
        if (_isRunning.value) return

        acceptJob = scope.launch {
            try {
                val ss = try {
                    ServerSocket(port)
                } catch (e: Exception) {
                    Log.w(TAG, "Port $port busy, picking free OS port", e)
                    ServerSocket(0)
                }
                serverSocket = ss
                val actualPort = ss.localPort
                val ip = getLocalIpAddress() ?: "127.0.0.1"
                _serverUrl.value = "http://$ip:$actualPort"
                _isRunning.value = true
                registerNsdService(actualPort)
                Log.i(TAG, "SanDen Continuity Hub started at ${_serverUrl.value}")

                while (!ss.isClosed) {
                    val client = ss.accept()
                    launch { handleClient(client) }
                }
            } catch (e: Exception) {
                if (_isRunning.value) {
                    Log.e(TAG, "Server error", e)
                }
            } finally {
                _isRunning.value = false
                _connectedClients.value = 0
            }
        }
    }

    fun stop() {
        _isRunning.value = false
        unregisterNsdService()
        wsClients.forEach { runCatching { it.close() } }
        wsClients.clear()
        _connectedClients.value = 0
        runCatching { serverSocket?.close() }
        serverSocket = null
        acceptJob?.cancel()
        acceptJob = null
        _serverUrl.value = ""
        Log.i(TAG, "SanDen Continuity Hub stopped")
    }

    suspend fun sendPhoneMessage(text: String, type: String = "TEXT") = withContext(Dispatchers.IO) {
        val entity = SyncMessageEntity(
            sender = "PHONE",
            type = type,
            textContent = text,
            timestamp = System.currentTimeMillis()
        )
        val id = syncMessageDao.insert(entity)
        broadcastWebSocket(entity.copy(id = id).toJson().toString())
    }

    suspend fun sendPhoneFile(filePath: String, fileName: String, fileSize: Long, mimeType: String) =
        withContext(Dispatchers.IO) {
            val entity = SyncMessageEntity(
                sender = "PHONE",
                type = "FILE",
                fileName = fileName,
                filePath = filePath,
                fileSize = fileSize,
                mimeType = mimeType,
                timestamp = System.currentTimeMillis()
            )
            val id = syncMessageDao.insert(entity)
            broadcastWebSocket(entity.copy(id = id).toJson().toString())
        }

    // ── HTTP / WebSocket Request Handling ────────────────────────────────

    private suspend fun handleClient(socket: Socket) {
        try {
            val input = BufferedInputStream(socket.getInputStream(), BUFFER_SIZE)
            val output = BufferedOutputStream(socket.getOutputStream(), BUFFER_SIZE)

            val requestLine = readLine(input) ?: return
            val parts = requestLine.split(" ")
            if (parts.size < 2) return
            val method = parts[0].uppercase()
            val rawTarget = parts[1]
            val path = rawTarget.substringBefore("?")
            val query = rawTarget.substringAfter("?", "")

            val headers = mutableMapOf<String, String>()
            while (true) {
                val line = readLine(input) ?: break
                if (line.isEmpty()) break
                val colon = line.indexOf(':')
                if (colon > 0) {
                    headers[line.substring(0, colon).trim().lowercase()] =
                        line.substring(colon + 1).trim()
                }
            }

            if (path == "/ws" && headers["upgrade"]?.equals("websocket", ignoreCase = true) == true) {
                handleWebSocket(socket, input, output, headers)
                return
            }

            when {
                method == "GET" && path == "/" -> serveCompanionHtml(output)
                method == "GET" && path == "/api/info" -> serveInfo(output)
                method == "GET" && path == "/api/messages" -> serveMessages(output)
                method == "POST" && path == "/api/messages" -> handlePostMessage(input, output, headers)
                method == "POST" && path == "/api/upload" -> handleUpload(input, output, headers)
                method == "GET" && path == "/api/vault/files" -> serveVaultFiles(output)
                method == "GET" && path == "/api/vault/download" -> serveVaultDownload(output, query)
                method == "GET" && path == "/api/messages/download" -> serveMessageDownload(output, query)
                else -> sendJson(output, 404, "Not Found", """{"error":"Not found"}""")
            }
        } catch (e: Exception) {
            Log.d(TAG, "Client disconnected: ${e.message}")
        } finally {
            runCatching { socket.close() }
        }
    }

    private fun serveCompanionHtml(output: OutputStream) {
        val bytes = context.assets.open("companion.html").use { it.readBytes() }
        writeResponse(output, 200, "OK", "text/html; charset=utf-8", bytes)
    }

    private suspend fun serveInfo(output: OutputStream) {
        val json = JSONObject().apply {
            put("deviceName", "${Build.MANUFACTURER} ${Build.MODEL}")
            put("url", _serverUrl.value)
            put("vaultMode", vaultRepository.getStorageMode().name)
        }.toString()
        sendJson(output, 200, "OK", json)
    }

    private suspend fun serveMessages(output: OutputStream) {
        val list = syncMessageDao.getRecent(100).reversed()
        val arr = JSONArray()
        for (m in list) arr.put(m.toJson())
        sendJson(output, 200, "OK", arr.toString())
    }

    private suspend fun handlePostMessage(
        input: InputStream,
        output: OutputStream,
        headers: Map<String, String>
    ) {
        val length = headers["content-length"]?.toIntOrNull() ?: 0
        if (length <= 0 || length > 2_000_000) {
            sendJson(output, 400, "Bad Request", """{"error":"Invalid body"}""")
            return
        }
        val bodyBytes = readExactBytes(input, length)
        val obj = JSONObject(String(bodyBytes, Charsets.UTF_8))
        val sender = obj.optString("sender", "LAPTOP")
        val type = obj.optString("type", "TEXT")
        val text = obj.optString("textContent", "")

        val entity = SyncMessageEntity(
            sender = sender,
            type = type,
            textContent = text,
            timestamp = System.currentTimeMillis()
        )
        val id = syncMessageDao.insert(entity)
        val saved = entity.copy(id = id)

        if (sender == "LAPTOP" && type == "CLIPBOARD" && text.isNotEmpty()) {
            withContext(Dispatchers.Main) {
                runCatching {
                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    cm.setPrimaryClip(ClipData.newPlainText("SanDen Sync", text))
                }
            }
        }

        broadcastWebSocket(saved.toJson().toString())
        sendJson(output, 200, "OK", saved.toJson().toString())
    }

    private suspend fun handleUpload(
        input: InputStream,
        output: OutputStream,
        headers: Map<String, String>
    ) {
        val contentLength = headers["content-length"]?.toLongOrNull() ?: -1L
        val rawName = headers["x-file-name"] ?: "laptop_${System.currentTimeMillis()}"
        val fileName = runCatching { URLDecoder.decode(rawName, "UTF-8") }.getOrDefault(rawName)
            .substringAfterLast('/')
            .substringAfterLast('\\')
            .ifBlank { "laptop_${System.currentTimeMillis()}" }

        vaultRepository.ensureVaultReady()
        val vaultDir = File(vaultRepository.getActiveVaultPath()).also { it.mkdirs() }
        val destFile = SafeFileOperations.resolveNameCollision(File(vaultDir, fileName))

        if (contentLength > 0) {
            SafeFileOperations.ensureSufficientSpace(vaultDir, contentLength)
                .onFailure {
                    sendJson(output, 507, "Insufficient Storage", """{"error":"Low storage"}""")
                    return
                }
        }

        var copied = 0L
        destFile.outputStream().buffered().use { fileOut ->
            val buf = ByteArray(BUFFER_SIZE)
            if (contentLength >= 0) {
                var remaining = contentLength
                while (remaining > 0) {
                    val toRead = minOf(buf.size.toLong(), remaining).toInt()
                    val n = input.read(buf, 0, toRead)
                    if (n == -1) break
                    fileOut.write(buf, 0, n)
                    copied += n
                    remaining -= n
                }
            } else {
                while (true) {
                    val n = input.read(buf)
                    if (n == -1) break
                    fileOut.write(buf, 0, n)
                    copied += n
                }
            }
            fileOut.flush()
        }

        vaultRepository.scanVault()
        val mime = CategoryDetector.detectMimeType(destFile.name) ?: "application/octet-stream"
        val entity = SyncMessageEntity(
            sender = "LAPTOP",
            type = "FILE",
            fileName = destFile.name,
            filePath = destFile.absolutePath,
            fileSize = destFile.length(),
            mimeType = mime,
            timestamp = System.currentTimeMillis()
        )
        val id = syncMessageDao.insert(entity)
        val saved = entity.copy(id = id)
        broadcastWebSocket(saved.toJson().toString())
        sendJson(output, 200, "OK", saved.toJson().toString())
    }

    private suspend fun serveVaultFiles(output: OutputStream) {
        val files = vaultFileDao.getAll()
        val arr = JSONArray()
        for (f in files) {
            arr.put(JSONObject().apply {
                put("id", f.id)
                put("name", f.name)
                put("size", f.size)
                put("category", f.category.name)
                put("isDirectory", f.isDirectory)
                put("lastModified", f.lastModified)
            })
        }
        sendJson(output, 200, "OK", arr.toString())
    }

    private suspend fun serveVaultDownload(output: OutputStream, query: String) {
        val idParam = query.split("&")
            .firstOrNull { it.startsWith("id=") }
            ?.substringAfter("id=")
            ?.toLongOrNull()
        val entity = idParam?.let { vaultFileDao.getById(it) }
        val file = entity?.let { File(it.path) }

        if (entity == null || file == null || !file.exists() || file.isDirectory) {
            sendJson(output, 404, "Not Found", """{"error":"File not found"}""")
            return
        }

        val mime = entity.mimeType ?: "application/octet-stream"
        val header = buildString {
            append("HTTP/1.1 200 OK\r\n")
            append("Content-Type: $mime\r\n")
            append("Content-Length: ${file.length()}\r\n")
            append("Content-Disposition: attachment; filename=\"${file.name.replace("\"", "_")}\"\r\n")
            append("Connection: close\r\n\r\n")
        }
        output.write(header.toByteArray(Charsets.UTF_8))
        file.inputStream().buffered().use { input ->
            val buf = ByteArray(BUFFER_SIZE)
            while (true) {
                val n = input.read(buf)
                if (n == -1) break
                output.write(buf, 0, n)
            }
        }
        output.flush()
    }

    private suspend fun serveMessageDownload(output: OutputStream, query: String) {
        val idParam = query.split("&")
            .firstOrNull { it.startsWith("id=") }
            ?.substringAfter("id=")
            ?.toLongOrNull()
        val msg = idParam?.let { syncMessageDao.getById(it) }
        val file = msg?.filePath?.takeIf { it.isNotBlank() }?.let { File(it) }

        if (msg == null || file == null || !file.exists() || file.isDirectory) {
            sendJson(output, 404, "Not Found", """{"error":"File not found"}""")
            return
        }

        val mime = msg.mimeType.ifBlank { "application/octet-stream" }
        val header = buildString {
            append("HTTP/1.1 200 OK\r\n")
            append("Content-Type: $mime\r\n")
            append("Content-Length: ${file.length()}\r\n")
            append("Content-Disposition: attachment; filename=\"${file.name.replace("\"", "_")}\"\r\n")
            append("Connection: close\r\n\r\n")
        }
        output.write(header.toByteArray(Charsets.UTF_8))
        file.inputStream().buffered().use { input ->
            val buf = ByteArray(BUFFER_SIZE)
            while (true) {
                val n = input.read(buf)
                if (n == -1) break
                output.write(buf, 0, n)
            }
        }
        output.flush()
    }

    // ── RFC 6455 WebSocket Support ───────────────────────────────────────

    private fun handleWebSocket(
        socket: Socket,
        input: InputStream,
        output: OutputStream,
        headers: Map<String, String>
    ) {
        val key = headers["sec-websocket-key"] ?: return
        val accept = Base64.encodeToString(
            MessageDigest.getInstance("SHA-1").digest((key + WS_GUID).toByteArray(Charsets.UTF_8)),
            Base64.NO_WRAP
        )
        val handshake = buildString {
            append("HTTP/1.1 101 Switching Protocols\r\n")
            append("Upgrade: websocket\r\n")
            append("Connection: Upgrade\r\n")
            append("Sec-WebSocket-Accept: $accept\r\n\r\n")
        }
        output.write(handshake.toByteArray(Charsets.UTF_8))
        output.flush()

        wsClients.add(output)
        _connectedClients.value = wsClients.size
        socket.soTimeout = 0

        try {
            // Keep reading frames until client disconnects
            while (!socket.isClosed && _isRunning.value) {
                val b0 = input.read()
                if (b0 == -1) break
                val opcode = b0 and 0x0F
                if (opcode == 0x8) break // Close frame

                val b1 = input.read()
                if (b1 == -1) break
                val masked = (b1 and 0x80) != 0
                var len = (b1 and 0x7F).toLong()
                if (len == 126L) {
                    val hi = input.read()
                    val lo = input.read()
                    if (hi == -1 || lo == -1) break
                    len = ((hi shl 8) or lo).toLong()
                } else if (len == 127L) {
                    var l = 0L
                    for (i in 0 until 8) {
                        val v = input.read()
                        if (v == -1) return
                        l = (l shl 8) or v.toLong()
                    }
                    len = l
                }
                if (masked) {
                    val mask = ByteArray(4)
                    if (readFully(input, mask) < 4) break
                }
                // Drain payload (control / ping)
                var skip = len
                val tmp = ByteArray(1024)
                while (skip > 0) {
                    val r = input.read(tmp, 0, minOf(tmp.size.toLong(), skip).toInt())
                    if (r == -1) return
                    skip -= r
                }
            }
        } finally {
            wsClients.remove(output)
            _connectedClients.value = wsClients.size
        }
    }

    private fun broadcastWebSocket(text: String) {
        val payload = text.toByteArray(Charsets.UTF_8)
        val frame = buildWsTextFrame(payload)
        for (clientOut in wsClients) {
            try {
                synchronized(clientOut) {
                    clientOut.write(frame)
                    clientOut.flush()
                }
            } catch (e: Exception) {
                wsClients.remove(clientOut)
                _connectedClients.value = wsClients.size
            }
        }
    }

    private fun buildWsTextFrame(payload: ByteArray): ByteArray {
        val len = payload.size
        val header = when {
            len <= 125 -> byteArrayOf(0x81.toByte(), len.toByte())
            len <= 65535 -> byteArrayOf(
                0x81.toByte(),
                126,
                ((len ushr 8) and 0xFF).toByte(),
                (len and 0xFF).toByte()
            )
            else -> byteArrayOf(
                0x81.toByte(), 127,
                0, 0, 0, 0,
                ((len ushr 24) and 0xFF).toByte(),
                ((len ushr 16) and 0xFF).toByte(),
                ((len ushr 8) and 0xFF).toByte(),
                (len and 0xFF).toByte()
            )
        }
        return header + payload
    }

    // ── NSD / mDNS Discovery ─────────────────────────────────────────────

    private fun registerNsdService(port: Int) {
        try {
            val mgr = context.getSystemService(Context.NSD_SERVICE) as? NsdManager ?: return
            nsdManager = mgr
            val info = NsdServiceInfo().apply {
                serviceName = "SanDen-${Build.MODEL}".take(32)
                serviceType = SERVICE_TYPE
                setPort(port)
            }
            val listener = object : NsdManager.RegistrationListener {
                override fun onServiceRegistered(serviceInfo: NsdServiceInfo) {
                    Log.i(TAG, "mDNS registered: ${serviceInfo.serviceName}")
                }
                override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}
                override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) {}
                override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}
            }
            registrationListener = listener
            mgr.registerService(info, NsdManager.PROTOCOL_DNS_SD, listener)
        } catch (e: Exception) {
            Log.w(TAG, "mDNS registration skipped: ${e.message}")
        }
    }

    private fun unregisterNsdService() {
        try {
            registrationListener?.let { nsdManager?.unregisterService(it) }
        } catch (_: Exception) {
        } finally {
            registrationListener = null
        }
    }

    // ── Utilities ────────────────────────────────────────────────────────

    private fun getLocalIpAddress(): String? {
        return try {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return null
            for (intf in interfaces) {
                if (!intf.isUp || intf.isLoopback) continue
                for (addr in intf.inetAddresses) {
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        val ip = addr.hostAddress ?: continue
                        if (!ip.startsWith("169.254.")) return ip
                    }
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    private fun sendJson(output: OutputStream, code: Int, status: String, json: String) {
        writeResponse(output, code, status, "application/json; charset=utf-8", json.toByteArray(Charsets.UTF_8))
    }

    private fun writeResponse(output: OutputStream, code: Int, status: String, contentType: String, body: ByteArray) {
        val header = buildString {
            append("HTTP/1.1 $code $status\r\n")
            append("Content-Type: $contentType\r\n")
            append("Content-Length: ${body.size}\r\n")
            append("Access-Control-Allow-Origin: *\r\n")
            append("Connection: close\r\n\r\n")
        }
        output.write(header.toByteArray(Charsets.UTF_8))
        output.write(body)
        output.flush()
    }

    private fun readLine(input: InputStream): String? {
        val sb = StringBuilder()
        while (true) {
            val c = input.read()
            if (c == -1) return if (sb.isEmpty()) null else sb.toString()
            if (c == '\n'.code) break
            if (c != '\r'.code) sb.append(c.toChar())
        }
        return sb.toString()
    }

    private fun readExactBytes(input: InputStream, length: Int): ByteArray {
        val buf = ByteArray(length)
        readFully(input, buf)
        return buf
    }

    private fun readFully(input: InputStream, buf: ByteArray): Int {
        var total = 0
        while (total < buf.size) {
            val r = input.read(buf, total, buf.size - total)
            if (r == -1) break
            total += r
        }
        return total
    }

    private fun SyncMessageEntity.toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("sender", sender)
        put("type", type)
        put("textContent", textContent)
        put("fileName", fileName)
        put("fileSize", fileSize)
        put("mimeType", mimeType)
        put("timestamp", timestamp)
    }
}
