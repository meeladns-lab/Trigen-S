package com.example.core.probe

import com.example.core.ipsource.GermanyNetherlandsFilter
import com.example.core.model.AttemptRecord
import com.example.core.model.FailureClass
import com.example.core.model.ProbeResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.ConnectException
import java.net.InetSocketAddress
import java.net.NoRouteToHostException
import java.net.Socket
import java.net.SocketException
import java.net.SocketTimeoutException
import java.security.cert.CertificateExpiredException
import java.security.cert.X509Certificate
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SNIHostName
import javax.net.ssl.SSLHandshakeException
import javax.net.ssl.SSLParameters
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory
import kotlin.coroutines.coroutineContext
import kotlin.math.abs

class ProbeEngine(
    private val timeoutBudget: TimeoutBudget,
    private val sniHosts: List<String> = listOf(
        "cdnjs.cloudflare.com",
        "time.cloudflare.com",
        "cloudflare-dns.com",
        "developers.cloudflare.com",
        "speed.cloudflare.com",
        "www.cloudflare.com"
    ),
    private val onlyGermanyAndNetherlands: Boolean = true
) {

    companion object {
        private val cachedSslSocketFactory: SSLSocketFactory by lazy {
            SSLSocketFactory.getDefault() as SSLSocketFactory
        }
        private val cachedHostnameVerifier by lazy {
            HttpsURLConnection.getDefaultHostnameVerifier()
        }
    }

    /**
     * Probes candidate IP:port with up to 3 attempts.
     * Evaluates health and enforces Germany/Netherlands datacenter restriction if enabled.
     * Employs early dead-endpoint termination to prevent thread starvation and UI freezes.
     */
    suspend fun probeCandidate(ip: String, port: Int, isNeighbor: Boolean = false): ProbeResult = withContext(Dispatchers.IO) {
        val cleanIp = NetUtils.stripBrackets(ip)

        val attempts = mutableListOf<AttemptRecord>()
        val totalAttempts = 3

        for (i in 1..totalAttempts) {
            if (!coroutineContext.isActive) {
                attempts.add(
                    AttemptRecord(
                        attemptNumber = i,
                        failureClass = FailureClass.CANCELLED,
                        errorMessage = "Cancelled"
                    )
                )
                break
            }

            val attempt = executeSingleAttempt(ip, port, i)
            attempts.add(attempt)

            if (onlyGermanyAndNetherlands && attempt.failureClass == FailureClass.NON_DE_NL_DATACENTER) {
                break
            }

            // Early exit optimization:
            // If first 2 attempts both failed without success, terminate to save scan time
            if (!attempt.isSuccess && i >= 2 && attempts.none { it.isSuccess }) {
                break
            }
        }

        val nonDeNl = attempts.any { it.failureClass == FailureClass.NON_DE_NL_DATACENTER }
        val successfulAttempts = if (nonDeNl) emptyList() else attempts.filter { it.isSuccess }
        val successCount = successfulAttempts.size

        val bestSuccess = successfulAttempts.firstOrNull()
        val detectedColo = bestSuccess?.colo ?: attempts.firstOrNull { it.colo.isNotEmpty() }?.colo ?: ""
        val isAllowedColo = !onlyGermanyAndNetherlands || GermanyNetherlandsFilter.isAllowedColo(detectedColo)
        val isHealthy = !nonDeNl && isAllowedColo && successCount >= 1

        val latencies = successfulAttempts.map { it.totalTimeMs.toDouble() }
        val minLatency = if (latencies.isNotEmpty()) latencies.minOrNull() ?: 0.0 else 9999.0
        val maxLatency = if (latencies.isNotEmpty()) latencies.maxOrNull() ?: 0.0 else 9999.0
        val avgLatency = if (latencies.isNotEmpty()) latencies.average() else 9999.0

        // Jitter: Mean Absolute Deviation (MAD) from average
        val jitter = if (latencies.isNotEmpty()) {
            latencies.map { abs(it - avgLatency) }.average()
        } else {
            0.0
        }

        val packetLossPercent = ((totalAttempts - successCount).toDouble() / totalAttempts) * 100.0

        val colo = if (detectedColo.isNotEmpty()) detectedColo else "CF"
        val clientEgressIp = bestSuccess?.clientEgressIp ?: ""
        val warp = bestSuccess?.warp ?: ""
        val tlsVersion = bestSuccess?.tlsVersion ?: ""

        val primaryFailure = when {
            nonDeNl || (onlyGermanyAndNetherlands && !isAllowedColo && successCount > 0) -> FailureClass.NON_DE_NL_DATACENTER
            isHealthy -> FailureClass.NONE
            else -> {
                // Find most descriptive failure
                attempts.map { it.failureClass }
                    .firstOrNull { it != FailureClass.NONE && it != FailureClass.CANCELLED }
                    ?: attempts.lastOrNull()?.failureClass
                    ?: FailureClass.TCP_TIMEOUT
            }
        }

        // Composite score: avg_latency * (1 + loss * 0.02) + jitter * 0.5
        val safeAvg = if (avgLatency.isNaN()) 9999.0 else avgLatency
        val safeJitter = if (jitter.isNaN()) 0.0 else jitter
        val lossPenalty = if (packetLossPercent > 0.0) 500.0 * (packetLossPercent / 10.0) else 0.0
        val rawScore = if (isHealthy) {
            (safeAvg * (1.0 + (packetLossPercent * 0.02))) + (safeJitter * 0.5) + lossPenalty
        } else {
            999999.0 + safeAvg
        }
        val score = if (rawScore.isNaN() || rawScore.isInfinite()) 999999.0 else rawScore

        ProbeResult(
            ip = ip,
            port = port,
            isIpv6 = NetUtils.isIpv6(ip),
            attempts = attempts,
            successCount = if (isHealthy) successCount else 0,
            totalAttempts = totalAttempts,
            minLatencyMs = minLatency,
            avgLatencyMs = avgLatency,
            maxLatencyMs = maxLatency,
            jitterMs = jitter,
            packetLossPercent = packetLossPercent,
            colo = colo,
            clientEgressIp = clientEgressIp,
            warp = warp,
            tlsVersion = tlsVersion,
            primaryFailure = primaryFailure,
            score = score,
            isHealthy = isHealthy,
            isNeighbor = isNeighbor
        )
    }

    /**
     * Executes single multi-phase probe attempt adhering to timeout split:
     * Dial <= T/4, TLS <= T/2, HTTP >= T/4
     */
    private fun executeSingleAttempt(ip: String, port: Int, attemptNum: Int): AttemptRecord {
        val cleanIp = NetUtils.stripBrackets(ip)
        var rawSocket: Socket? = null
        var sslSocket: SSLSocket? = null
        val startTime = System.currentTimeMillis()
        var tcpTime: Long = -1
        var tlsTime: Long = -1
        var httpTime: Long = -1

        try {
            // STAGE A: TCP Handshake
            val tcpStart = System.currentTimeMillis()
            rawSocket = Socket()
            rawSocket.soTimeout = timeoutBudget.httpTimeoutMs.toInt()
            rawSocket.connect(
                InetSocketAddress(cleanIp, port),
                timeoutBudget.dialTimeoutMs.toInt()
            )
            tcpTime = System.currentTimeMillis() - tcpStart

            // If non-TLS port (e.g., 80, 8080)
            if (port == 80 || port == 8080 || port == 8880 || port == 2052 || port == 2082 || port == 2086 || port == 2095) {
                val httpStart = System.currentTimeMillis()
                val traceResult = queryTraceHttp(rawSocket, "speed.cloudflare.com")
                httpTime = System.currentTimeMillis() - httpStart
                val totalTime = System.currentTimeMillis() - startTime
                return AttemptRecord(
                    attemptNumber = attemptNum,
                    tcpTimeMs = tcpTime,
                    tlsTimeMs = 0,
                    httpTimeMs = httpTime,
                    totalTimeMs = totalTime,
                    isSuccess = true,
                    failureClass = FailureClass.NONE,
                    colo = traceResult["colo"] ?: "",
                    clientEgressIp = traceResult["ip"] ?: "",
                    warp = traceResult["warp"] ?: "",
                    tlsVersion = "Plain HTTP"
                )
            }

            // STAGE B: TLS Handshake with explicit SNI
            var lastTlsException: Exception? = null
            var successfulSni: String? = null
            var negotiatedTls: String = ""
            var certValid = false

            for (sni in sniHosts) {
                val tlsStart = System.currentTimeMillis()
                try {
                    sslSocket = cachedSslSocketFactory.createSocket(
                        rawSocket,
                        sni,
                        port,
                        true
                    ) as SSLSocket

                    val sslParams = SSLParameters()
                    sslParams.serverNames = listOf(SNIHostName(sni))
                    sslSocket.sslParameters = sslParams
                    sslSocket.soTimeout = timeoutBudget.tlsTimeoutMs.toInt()
                    sslSocket.startHandshake()

                    tlsTime = System.currentTimeMillis() - tlsStart
                    negotiatedTls = sslSocket.session.protocol ?: "TLS"

                    // Verify certificate
                    val hostnameOk = cachedHostnameVerifier.verify(sni, sslSocket.session)
                    val peerCerts = try { sslSocket.session.peerCertificates } catch (_: Exception) { emptyArray() }
                    val leafCert = peerCerts.firstOrNull() as? X509Certificate
                    val subject = leafCert?.subjectX500Principal?.name ?: ""
                    val issuer = leafCert?.issuerX500Principal?.name ?: ""

                    val isCfEntity = subject.contains("Cloudflare", ignoreCase = true) ||
                            issuer.contains("Cloudflare", ignoreCase = true) ||
                            issuer.contains("Google Trust Services", ignoreCase = true) ||
                            issuer.contains("Let's Encrypt", ignoreCase = true)

                    if (!hostnameOk && !isCfEntity) {
                        return AttemptRecord(
                            attemptNumber = attemptNum,
                            tcpTimeMs = tcpTime,
                            tlsTimeMs = tlsTime,
                            failureClass = FailureClass.TLS_CERT_NAME_MISMATCH,
                            errorMessage = "Presented certificate does not match SNI ($sni) or Cloudflare CA."
                        )
                    }

                    leafCert?.checkValidity()
                    certValid = true
                    successfulSni = sni
                    break // Handshake succeeded
                } catch (e: CertificateExpiredException) {
                    return AttemptRecord(
                        attemptNumber = attemptNum,
                        tcpTimeMs = tcpTime,
                        tlsTimeMs = tlsTime,
                        failureClass = FailureClass.TLS_CERT_EXPIRED,
                        errorMessage = "Certificate expired: ${e.message}"
                    )
                } catch (e: Exception) {
                    lastTlsException = e
                    val msg = e.message?.lowercase() ?: ""
                    if (msg.contains("socket closed") || msg.contains("connection reset") || msg.contains("broken pipe")) {
                        break
                    }
                }
            }

            if (successfulSni == null || sslSocket == null) {
                return classifyTlsFailure(lastTlsException, tcpTime, attemptNum)
            }

            // STAGE C: HTTP /cdn-cgi/trace
            var traceResult = emptyMap<String, String>()
            try {
                val httpStart = System.currentTimeMillis()
                sslSocket.soTimeout = timeoutBudget.httpTimeoutMs.toInt()
                traceResult = queryTraceHttp(sslSocket, successfulSni)
                httpTime = System.currentTimeMillis() - httpStart
            } catch (_: Exception) {
                // In restricted networks (Iran DPI), HTTP GET on /cdn-cgi/trace may be reset or filtered
                // even when TCP & TLS are fully operational.
                httpTime = -1
            }

            val detectedColo = traceResult["colo"]?.trim()?.uppercase() ?: ""
            val clientIp = traceResult["ip"] ?: ""
            val warp = traceResult["warp"] ?: ""

            // Enforce Germany & Netherlands Datacenter filter if enabled
            if (onlyGermanyAndNetherlands && detectedColo.isNotEmpty() && !GermanyNetherlandsFilter.isAllowedColo(detectedColo)) {
                return AttemptRecord(
                    attemptNumber = attemptNum,
                    tcpTimeMs = tcpTime,
                    tlsTimeMs = tlsTime,
                    httpTimeMs = if (httpTime > 0) httpTime else 0,
                    totalTimeMs = System.currentTimeMillis() - startTime,
                    isSuccess = false,
                    failureClass = FailureClass.NON_DE_NL_DATACENTER,
                    errorMessage = "Datacenter ($detectedColo) is outside Germany and Netherlands",
                    colo = detectedColo
                )
            }

            val isVerifiedCf = traceResult["isCloudflare"] == "true" ||
                    detectedColo.isNotEmpty() ||
                    traceResult.containsKey("h") ||
                    certValid

            if (!isVerifiedCf) {
                return AttemptRecord(
                    attemptNumber = attemptNum,
                    tcpTimeMs = tcpTime,
                    tlsTimeMs = tlsTime,
                    httpTimeMs = if (httpTime > 0) httpTime else 0,
                    failureClass = FailureClass.NOT_CLOUDFLARE,
                    errorMessage = "Response did not contain valid Cloudflare headers or trace"
                )
            }

            val totalTime = System.currentTimeMillis() - startTime
            return AttemptRecord(
                attemptNumber = attemptNum,
                tcpTimeMs = tcpTime,
                tlsTimeMs = tlsTime,
                httpTimeMs = if (httpTime > 0) httpTime else 0,
                totalTimeMs = totalTime,
                isSuccess = true,
                failureClass = FailureClass.NONE,
                colo = if (detectedColo.isNotEmpty()) detectedColo else "CF",
                clientEgressIp = clientIp,
                warp = warp,
                tlsVersion = negotiatedTls
            )

        } catch (e: SocketTimeoutException) {
            return AttemptRecord(
                attemptNumber = attemptNum,
                tcpTimeMs = tcpTime,
                failureClass = if (tcpTime < 0) FailureClass.TCP_TIMEOUT else FailureClass.HTTP_TIMEOUT,
                errorMessage = "Timeout: ${e.message}"
            )
        } catch (e: ConnectException) {
            val msg = e.message?.lowercase() ?: ""
            val failure = if (msg.contains("refused")) FailureClass.TCP_REFUSED else FailureClass.TCP_TIMEOUT
            return AttemptRecord(
                attemptNumber = attemptNum,
                failureClass = failure,
                errorMessage = e.message ?: "Connection failed"
            )
        } catch (e: SocketException) {
            val msg = e.message?.lowercase() ?: ""
            val isRst = msg.contains("reset") || msg.contains("connection reset") || msg.contains("econnreset")
            return AttemptRecord(
                attemptNumber = attemptNum,
                failureClass = if (isRst) FailureClass.TCP_RESET else FailureClass.TCP_REFUSED,
                errorMessage = e.message ?: "Socket error"
            )
        } catch (e: NoRouteToHostException) {
            return AttemptRecord(
                attemptNumber = attemptNum,
                failureClass = FailureClass.TCP_TIMEOUT,
                errorMessage = "No route to host"
            )
        } catch (e: Exception) {
            return AttemptRecord(
                attemptNumber = attemptNum,
                failureClass = FailureClass.HTTP_STATUS_ERROR,
                errorMessage = e.message ?: "Unknown error"
            )
        } finally {
            try { sslSocket?.close() } catch (_: Exception) {}
            try { rawSocket?.close() } catch (_: Exception) {}
        }
    }

    private fun classifyTlsFailure(e: Exception?, tcpTime: Long, attemptNum: Int): AttemptRecord {
        val msg = e?.message?.lowercase() ?: ""
        val failureClass = when {
            e is SocketTimeoutException -> FailureClass.TLS_TIMEOUT
            msg.contains("unknown authority") || msg.contains("certpath") || msg.contains("trust") -> FailureClass.TLS_CERT_UNKNOWN_AUTHORITY
            msg.contains("reset") || msg.contains("econnreset") -> FailureClass.TCP_RESET
            e is SSLHandshakeException -> FailureClass.TLS_HANDSHAKE_FAILURE
            else -> FailureClass.TLS_HANDSHAKE_FAILURE
        }
        return AttemptRecord(
            attemptNumber = attemptNum,
            tcpTimeMs = tcpTime,
            failureClass = failureClass,
            errorMessage = e?.message ?: "TLS handshake failed"
        )
    }

    /**
     * Executes GET /cdn-cgi/trace directly over the socket stream.
     * Evaluates both HTTP response headers (Server, CF-RAY) and trace body key-value pairs.
     */
    private fun queryTraceHttp(socket: Socket, host: String): Map<String, String> {
        val out = OutputStreamWriter(socket.getOutputStream(), Charsets.US_ASCII)
        out.write("GET /cdn-cgi/trace HTTP/1.1\r\n")
        out.write("Host: $host\r\n")
        out.write("User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36\r\n")
        out.write("Accept: */*\r\n")
        out.write("Connection: close\r\n")
        out.write("\r\n")
        out.flush()

        val reader = BufferedReader(InputStreamReader(socket.getInputStream(), Charsets.US_ASCII))
        val statusLine = reader.readLine() ?: ""

        val result = mutableMapOf<String, String>()
        var isCloudflare = statusLine.contains("cloudflare", ignoreCase = true)
        var cfRayColo = ""

        // Parse response headers up to 40 lines
        var headerLines = 0
        while (headerLines < 40) {
            val line = try { reader.readLine() } catch (_: Exception) { null } ?: break
            headerLines++
            val trimmed = line.trim()
            if (trimmed.isEmpty()) break

            val colonIdx = trimmed.indexOf(':')
            if (colonIdx > 0) {
                val headerName = trimmed.substring(0, colonIdx).trim().lowercase()
                val headerVal = trimmed.substring(colonIdx + 1).trim()
                if (headerName == "server" && headerVal.contains("cloudflare", ignoreCase = true)) {
                    isCloudflare = true
                    result["server"] = headerVal
                } else if (headerName == "cf-ray") {
                    isCloudflare = true
                    result["cf-ray"] = headerVal
                    val lastHyphen = headerVal.lastIndexOf('-')
                    if (lastHyphen != -1 && lastHyphen < headerVal.length - 1) {
                        val potentialColo = headerVal.substring(lastHyphen + 1).trim().uppercase()
                        if (potentialColo.length in 3..4 && potentialColo.all { it.isLetter() }) {
                            cfRayColo = potentialColo
                        }
                    }
                }
            }
        }

        // Read body (key=value pairs or CF error notices) up to 30 lines
        var bodyLines = 0
        while (bodyLines < 30) {
            val line = try { reader.readLine() } catch (_: Exception) { null } ?: break
            bodyLines++
            val l = line.trim()
            if (l.isEmpty()) break
            if (l.contains("error code: 1034", ignoreCase = true) || l.contains("cloudflare", ignoreCase = true)) {
                isCloudflare = true
            }
            val eq = l.indexOf('=')
            if (eq > 0) {
                val key = l.substring(0, eq).trim()
                val value = l.substring(eq + 1).trim()
                result[key] = value
            }
            if (result.containsKey("colo") && result.containsKey("warp") && result.containsKey("ip")) {
                break
            }
        }

        if (!result.containsKey("colo") && cfRayColo.isNotEmpty()) {
            result["colo"] = cfRayColo
        }
        if (isCloudflare || result.containsKey("colo") || result.containsKey("h")) {
            result["isCloudflare"] = "true"
        }

        return result
    }
}
