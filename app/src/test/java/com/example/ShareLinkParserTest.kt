package com.example

import com.example.core.linkparse.ShareLinkParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ShareLinkParserTest {

    @Test
    fun testParseVlessStandard() {
        val link = "vless://12345678-1234-1234-1234-123456789012@my-worker.workers.dev:443?type=ws&security=tls&sni=my-worker.workers.dev&path=%2Fspeed#MyWorker"
        val config = ShareLinkParser.parse(link)
        assertNotNull(config)
        assertEquals("vless", config!!.protocol)
        assertEquals("12345678-1234-1234-1234-123456789012", config.uuidOrPassword)
        assertEquals("my-worker.workers.dev", config.host)
        assertEquals(443, config.port)
        assertEquals("ws", config.transport)
        assertEquals("tls", config.security)
        assertEquals("/speed", config.path)
        assertEquals("MyWorker", config.remarks)

        // Test Credential Redaction (§8)
        val redacted = config.getRedactedString()
        assertTrue("Credentials must be redacted in display", redacted.contains("***@"))
        assertTrue("Host and params preserved", redacted.contains("my-worker.workers.dev:443"))

        // Test Rewrite with Clean IP
        val rewritten = ShareLinkParser.rewriteWithCleanIp(config, "104.16.1.1")
        assertTrue("Host replaced with clean IP", rewritten.contains("@104.16.1.1:443"))
        assertTrue("SNI preserved as worker host", rewritten.contains("sni=my-worker.workers.dev"))
    }

    @Test
    fun testParseVlessWithMissingQuestionMark() {
        // §8 Parser hardening: Handle a missing ? separator (terminals eat it on paste)
        val brokenLink = "vless://12345678-1234-1234-1234-123456789012@speed.cloudflare.com:443&type=ws&security=tls&sni=speed.cloudflare.com"
        val config = ShareLinkParser.parse(brokenLink)
        assertNotNull(config)
        assertEquals("vless", config!!.protocol)
        assertEquals(443, config.port)
    }

    @Test
    fun testHostTypoNormalization() {
        // §8 Parser hardening: Normalize common host typos (worers.dev -> workers.dev)
        val normalized = ShareLinkParser.normalizeHostTypos("sub.mytest.worers.dev")
        assertEquals("sub.mytest.workers.dev", normalized)
    }

    @Test
    fun testParseTrojan() {
        val trojanLink = "trojan://secretpassword@proxy.example.com:443?security=tls&sni=proxy.example.com#TestTrojan"
        val config = ShareLinkParser.parse(trojanLink)
        assertNotNull(config)
        assertEquals("trojan", config!!.protocol)
        assertEquals("secretpassword", config.uuidOrPassword)
        assertEquals("proxy.example.com", config.host)
        assertEquals("TestTrojan", config.remarks)
    }

    @Test
    fun testParseShadowsocksSip002() {
        val ssLink = "ss://YWVzLTEyOC1nY206cGFzc3dvcmQ@server.com:8388#MyServer"
        val config = ShareLinkParser.parse(ssLink)
        assertNotNull(config)
        assertEquals("ss", config!!.protocol)
        assertEquals("server.com", config.host)
        assertEquals(8388, config.port)
    }

    @Test
    fun testGenerateTop20VlessConfigs() {
        val baseVless = "vless://12345678-1234-1234-1234-123456789012@my-worker.workers.dev:443?type=ws&security=tls&sni=my-worker.workers.dev&path=%2Fspeed#BaseConfig"
        val mockResults = (1..25).map { i ->
            com.example.core.model.ProbeResult(
                ip = "104.16.1.$i",
                port = 443,
                isIpv6 = false,
                attempts = emptyList(),
                successCount = 4,
                totalAttempts = 4,
                minLatencyMs = 38.0 + i,
                avgLatencyMs = 40.0 + i,
                maxLatencyMs = 42.0 + i,
                jitterMs = 2.0,
                packetLossPercent = 0.0,
                colo = "FRA",
                score = 90.0 - i,
                isHealthy = true
            )
        }

        val generated = ShareLinkParser.generateTop20VlessConfigs(baseVless, mockResults, 20)
        assertEquals(20, generated.size)

        // Check first config
        val first = generated[0]
        assertTrue("Contains clean IP 104.16.1.1", first.contains("@104.16.1.1:443"))
        assertTrue("Preserves UUID", first.contains("12345678-1234-1234-1234-123456789012"))
        assertTrue("Preserves SNI", first.contains("sni=my-worker.workers.dev"))
        assertTrue("Contains rank label and latency", first.contains("CF-01 [104.16.1.1]"))

        // Check 20th config
        val twentieth = generated[19]
        assertTrue("Contains 20th clean IP 104.16.1.20", twentieth.contains("@104.16.1.20:443"))
        assertTrue("Contains rank 20", twentieth.contains("CF-20 [104.16.1.20]"))

        // All 20 configs have distinct IP addresses
        val distinctIps = generated.map { it.substringAfter("@").substringBefore(":") }.toSet()
        assertEquals(20, distinctIps.size)
    }
}
