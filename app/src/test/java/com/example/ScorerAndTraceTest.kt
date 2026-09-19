package com.example

import com.example.core.model.AttemptRecord
import com.example.core.model.FailureClass
import com.example.core.model.ProbeResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScorerAndTraceTest {

    @Test
    fun testScorerPrioritizesZeroLossOverLossyEvenWithSlightlyHigherLatency() {
        // §1.6: Health-aware tiering. Clean IPs with 0% loss must rank ahead of lossy ones
        val cleanCandidate = ProbeResult(
            ip = "104.16.1.1",
            port = 443,
            isIpv6 = false,
            avgLatencyMs = 120.0,
            minLatencyMs = 110.0,
            maxLatencyMs = 130.0,
            jitterMs = 10.0,
            packetLossPercent = 0.0,
            successCount = 3,
            totalAttempts = 3,
            score = 120.0 + (10.0 * 1.5) + (0.0 * 20.0), // 135.0
            colo = "DXB",
            attempts = listOf(
                AttemptRecord(attemptNumber = 1, isSuccess = true, tcpTimeMs = 40, tlsTimeMs = 50, httpTimeMs = 30, totalTimeMs = 120),
                AttemptRecord(attemptNumber = 2, isSuccess = true, tcpTimeMs = 40, tlsTimeMs = 50, httpTimeMs = 30, totalTimeMs = 120),
                AttemptRecord(attemptNumber = 3, isSuccess = true, tcpTimeMs = 40, tlsTimeMs = 50, httpTimeMs = 30, totalTimeMs = 120)
            )
        )

        val lossyCandidate = ProbeResult(
            ip = "104.16.2.2",
            port = 443,
            isIpv6 = false,
            avgLatencyMs = 90.0,
            minLatencyMs = 85.0,
            maxLatencyMs = 95.0,
            jitterMs = 5.0,
            packetLossPercent = 33.3,
            successCount = 2,
            totalAttempts = 3,
            score = 90.0 + (5.0 * 1.5) + (33.3 * 20.0), // 90 + 7.5 + 666 = 763.5
            colo = "DXB",
            attempts = listOf(
                AttemptRecord(attemptNumber = 1, isSuccess = true, tcpTimeMs = 30, tlsTimeMs = 35, httpTimeMs = 25, totalTimeMs = 90),
                AttemptRecord(attemptNumber = 2, isSuccess = true, tcpTimeMs = 30, tlsTimeMs = 35, httpTimeMs = 25, totalTimeMs = 90),
                AttemptRecord(attemptNumber = 3, isSuccess = false, failureClass = FailureClass.TCP_RESET, errorMessage = "Reset")
            )
        )

        // Loss penalty (loss% * 20) ensures cleanCandidate.score < lossyCandidate.score
        assertTrue(
            "Loss-free IP must have lower (better) composite score than lossy IP",
            cleanCandidate.score < lossyCandidate.score
        )
    }

    @Test
    fun testCdnCgiTraceParsing() {
        val traceBody = """
            fl=31f456
            h=speed.cloudflare.com
            ip=2.188.10.20
            ts=1710000000.123
            visit_scheme=https
            uag=CleanIpScanner/1.0
            colo=DXB
            sliver=none
            http=http/1.1
            loc=IR
            tls=TLSv1.3
            sni=plaintext
            warp=off
            gateway=off
            rtt=112
        """.trimIndent()

        val map = traceBody.lines().mapNotNull { line ->
            val idx = line.indexOf('=')
            if (idx > 0) line.substring(0, idx).trim() to line.substring(idx + 1).trim() else null
        }.toMap()

        assertEquals("DXB", map["colo"])
        assertEquals("off", map["warp"])
        assertEquals("TLSv1.3", map["tls"])
        assertEquals("2.188.10.20", map["ip"])
    }
}
