package com.example.core.probe

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.util.Random
import javax.net.ssl.SNIHostName
import javax.net.ssl.SSLParameters
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

data class SpeedTestResult(
    val throughputMbps: Double?, // Download speed in MB/s
    val uploadSpeedMbps: Double?, // Upload speed in MB/s
    val ttfbMs: Long?,
    val bytesRead: Long = 0,
    val bytesUploaded: Long = 0,
    val durationMs: Long = 0,
    val uploadDurationMs: Long = 0
)

class ThroughputTester(
    private val timeoutSec: Int = 8
) {
    private val sslSocketFactory: SSLSocketFactory by lazy {
        SSLSocketFactory.getDefault() as SSLSocketFactory
    }

    /**
     * Executes both download and upload throughput tests pinned directly to candidate IP:port.
     * Engineered specifically to bypass Iranian DPI blocking and rate-limiting using whitelisted SNIs.
     */
    suspend fun testDualSpeed(
        ip: String,
        port: Int = 443,
        payloadBytes: Int = 524288 // 512 KB
    ): SpeedTestResult = withContext(Dispatchers.IO) {
        val dlResult = testDownloadSpeed(ip, port, payloadBytes)
        val ulResult = testUploadSpeed(ip, port, (payloadBytes / 2).coerceAtLeast(262144)) // 256 KB upload payload

        SpeedTestResult(
            throughputMbps = dlResult.throughputMbps,
            uploadSpeedMbps = ulResult.uploadSpeedMbps,
            ttfbMs = dlResult.ttfbMs ?: ulResult.ttfbMs,
            bytesRead = dlResult.bytesRead,
            bytesUploaded = ulResult.bytesUploaded,
            durationMs = dlResult.durationMs,
            uploadDurationMs = ulResult.uploadDurationMs
        )
    }

    /**
     * Executes download speed test pinned directly to candidate IP.
     * Uses whitelisted CDN static assets (cdnjs / Cloudflare) to ensure 100% DPI pass-through in Iran.
     */
    suspend fun testDownloadSpeed(
        ip: String,
        port: Int = 443,
        targetBytes: Int = 524288
    ): SpeedTestResult = withContext(Dispatchers.IO) {
        val cleanIp = NetUtils.stripBrackets(ip)

        // Multiple CDN endpoints hosted on Cloudflare edge with clean SNIs
        val downloadEndpoints = listOf(
            Pair("cdnjs.cloudflare.com", "/ajax/libs/three.js/r128/three.min.js"), // ~600 KB
            Pair("cdnjs.cloudflare.com", "/ajax/libs/pdf.js/3.11.174/pdf.min.js"), // ~280 KB
            Pair("speed.cloudflare.com", "/__down?bytes=$targetBytes")
        )

        for ((host, path) in downloadEndpoints) {
            var rawSocket: Socket? = null
            var sslSocket: SSLSocket? = null
            try {
                val startCallTime = System.currentTimeMillis()
                rawSocket = Socket()
                rawSocket.soTimeout = timeoutSec * 1000
                rawSocket.connect(InetSocketAddress(cleanIp, port), timeoutSec * 1000)

                sslSocket = sslSocketFactory.createSocket(rawSocket, host, port, true) as SSLSocket
                val sslParams = SSLParameters()
                sslParams.serverNames = listOf(SNIHostName(host))
                sslSocket.sslParameters = sslParams
                sslSocket.soTimeout = timeoutSec * 1000
                sslSocket.startHandshake()

                val out: OutputStream = sslSocket.outputStream
                val req = "GET $path HTTP/1.1\r\nHost: $host\r\nUser-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64)\r\nAccept: */*\r\nConnection: close\r\n\r\n"
                out.write(req.toByteArray(Charsets.US_ASCII))
                out.flush()

                val input: InputStream = sslSocket.inputStream
                val headerBuffer = ByteArray(4096)
                var headerEnd = false
                var totalBytes = 0L
                var ttfb: Long? = null

                // Read headers to determine TTFB
                val headerStart = System.currentTimeMillis()
                var hLen = 0
                val sb = StringBuilder()
                while (!headerEnd && (input.read(headerBuffer).also { hLen = it }) != -1) {
                    val chunk = String(headerBuffer, 0, hLen, Charsets.US_ASCII)
                    sb.append(chunk)
                    val idx = sb.indexOf("\r\n\r\n")
                    if (idx != -1) {
                        headerEnd = true
                        ttfb = System.currentTimeMillis() - startCallTime
                        val bodyPartBytes = hLen - (idx + 4)
                        if (bodyPartBytes > 0) {
                            totalBytes += bodyPartBytes
                        }
                        break
                    }
                }

                if (!headerEnd) {
                    sslSocket.close()
                    continue
                }

                // Stream the body chunks
                val streamStartTime = System.currentTimeMillis()
                val buffer = ByteArray(8192)
                var read: Int
                while (input.read(buffer).also { read = it } != -1) {
                    totalBytes += read
                    if (totalBytes >= targetBytes) break
                }

                val downloadDurationMs = (System.currentTimeMillis() - streamStartTime).coerceAtLeast(1)
                val seconds = downloadDurationMs / 1000.0

                // Megabytes per second (MB/s)
                val mbps = (totalBytes.toDouble() / (1024.0 * 1024.0)) / seconds

                if (totalBytes > 1024 && mbps > 0.001) {
                    return@withContext SpeedTestResult(
                        throughputMbps = mbps,
                        uploadSpeedMbps = null,
                        ttfbMs = ttfb ?: (System.currentTimeMillis() - startCallTime),
                        bytesRead = totalBytes,
                        bytesUploaded = 0,
                        durationMs = downloadDurationMs,
                        uploadDurationMs = 0
                    )
                }
            } catch (_: Exception) {
                // Try next endpoint
            } finally {
                try { sslSocket?.close() } catch (_: Exception) {}
                try { rawSocket?.close() } catch (_: Exception) {}
            }
        }

        SpeedTestResult(null, null, null, 0, 0, 0, 0)
    }

    /**
     * Executes upload speed test pinned directly to candidate IP.
     * Streams binary payload upstream via TLS POST to measure real upload throughput in Iran.
     */
    suspend fun testUploadSpeed(
        ip: String,
        port: Int = 443,
        uploadBytes: Int = 262144 // 256 KB default
    ): SpeedTestResult = withContext(Dispatchers.IO) {
        val cleanIp = NetUtils.stripBrackets(ip)
        val uploadHosts = listOf("cloudflare-dns.com", "cdnjs.cloudflare.com", "speed.cloudflare.com")

        for (host in uploadHosts) {
            var rawSocket: Socket? = null
            var sslSocket: SSLSocket? = null
            try {
                rawSocket = Socket()
                rawSocket.soTimeout = timeoutSec * 1000
                rawSocket.connect(InetSocketAddress(cleanIp, port), timeoutSec * 1000)

                sslSocket = sslSocketFactory.createSocket(rawSocket, host, port, true) as SSLSocket
                val sslParams = SSLParameters()
                sslParams.serverNames = listOf(SNIHostName(host))
                sslSocket.sslParameters = sslParams
                sslSocket.soTimeout = timeoutSec * 1000
                sslSocket.startHandshake()

                val out: OutputStream = sslSocket.outputStream
                val header = "POST /dns-query HTTP/1.1\r\nHost: $host\r\nContent-Type: application/octet-stream\r\nContent-Length: $uploadBytes\r\nConnection: close\r\n\r\n"
                out.write(header.toByteArray(Charsets.US_ASCII))

                val payloadChunk = ByteArray(8192)
                Random().nextBytes(payloadChunk)

                val uploadStartTime = System.currentTimeMillis()
                var written = 0
                while (written < uploadBytes) {
                    val toWrite = (uploadBytes - written).coerceAtMost(payloadChunk.size)
                    out.write(payloadChunk, 0, toWrite)
                    written += toWrite
                }
                out.flush()

                val uploadDurationMs = (System.currentTimeMillis() - uploadStartTime).coerceAtLeast(1)
                val seconds = uploadDurationMs / 1000.0
                val mbps = (written.toDouble() / (1024.0 * 1024.0)) / seconds

                // Read status line or acknowledgment from server
                val input: InputStream = sslSocket.inputStream
                val respBuf = ByteArray(512)
                val readResp = try { input.read(respBuf) } catch (_: Exception) { 0 }

                if (written > 0 && mbps > 0.001) {
                    return@withContext SpeedTestResult(
                        throughputMbps = null,
                        uploadSpeedMbps = mbps,
                        ttfbMs = uploadDurationMs,
                        bytesRead = 0,
                        bytesUploaded = written.toLong(),
                        durationMs = 0,
                        uploadDurationMs = uploadDurationMs
                    )
                }
            } catch (_: Exception) {
                // Try next host
            } finally {
                try { sslSocket?.close() } catch (_: Exception) {}
                try { rawSocket?.close() } catch (_: Exception) {}
            }
        }

        SpeedTestResult(null, null, null, 0, 0, 0, 0)
    }
}

