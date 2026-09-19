package com.example.core.linkparse

import android.util.Base64
import com.example.core.model.ShareLinkConfig
import com.example.core.probe.NetUtils
import org.json.JSONObject
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

object ShareLinkParser {

    /**
     * Parses vless://, trojan://, vmess://, or ss:// links with defensive error handling
     */
    fun parse(rawLink: String): ShareLinkConfig? {
        val trimmed = rawLink.trim()
        if (trimmed.isEmpty()) return null

        return when {
            trimmed.startsWith("vless://", ignoreCase = true) -> parseVless(trimmed)
            trimmed.startsWith("trojan://", ignoreCase = true) -> parseTrojan(trimmed)
            trimmed.startsWith("vmess://", ignoreCase = true) -> parseVmess(trimmed)
            trimmed.startsWith("ss://", ignoreCase = true) -> parseShadowsocks(trimmed)
            else -> null
        }
    }

    private fun parseVless(link: String): ShareLinkConfig? {
        try {
            val content = link.substringAfter("://")
            val remarks = if (content.contains("#")) {
                URLDecoder.decode(content.substringAfter("#"), StandardCharsets.UTF_8.name())
            } else ""

            val withoutRemarks = content.substringBefore("#")
            val uuid = withoutRemarks.substringBefore("@")
            val afterAt = withoutRemarks.substringAfter("@")

            // Handle missing '?'
            val hostPortPart: String
            val queryPart: String
            if (afterAt.contains("?")) {
                hostPortPart = afterAt.substringBefore("?")
                queryPart = afterAt.substringAfter("?")
            } else {
                // If missing '?' but contains '='
                val firstEq = afterAt.indexOf('=')
                if (firstEq != -1) {
                    val lastAmpBeforeEq = afterAt.lastIndexOf('&', firstEq)
                    val splitIdx = if (lastAmpBeforeEq != -1) lastAmpBeforeEq else afterAt.indexOf('/')
                    if (splitIdx != -1) {
                        hostPortPart = afterAt.substring(0, splitIdx)
                        queryPart = afterAt.substring(splitIdx + 1)
                    } else {
                        hostPortPart = afterAt.substring(0, firstEq).substringBeforeLast(':')
                        queryPart = afterAt.substring(hostPortPart.length)
                    }
                } else {
                    hostPortPart = afterAt
                    queryPart = ""
                }
            }

            val queryParams = parseQueryParams(queryPart)
            val hostPort = extractHostAndPort(hostPortPart, 443)

            val rawSni = queryParams["sni"] ?: queryParams["peer"] ?: hostPort.first
            val cleanSni = normalizeHostTypos(rawSni)
            val path = URLDecoder.decode(queryParams["path"] ?: "", StandardCharsets.UTF_8.name())

            return ShareLinkConfig(
                protocol = "vless",
                uuidOrPassword = uuid,
                host = normalizeHostTypos(hostPort.first),
                port = hostPort.second,
                sni = cleanSni,
                path = path,
                transport = queryParams["type"] ?: "tcp",
                security = queryParams["security"] ?: "tls",
                remarks = remarks,
                rawOriginal = link
            )
        } catch (_: Exception) {
            return null
        }
    }

    private fun parseTrojan(link: String): ShareLinkConfig? {
        try {
            val content = link.substringAfter("://")
            val remarks = if (content.contains("#")) {
                URLDecoder.decode(content.substringAfter("#"), StandardCharsets.UTF_8.name())
            } else ""

            val withoutRemarks = content.substringBefore("#")
            val password = withoutRemarks.substringBefore("@")
            val afterAt = withoutRemarks.substringAfter("@")

            val hostPortPart = afterAt.substringBefore("?")
            val queryPart = if (afterAt.contains("?")) afterAt.substringAfter("?") else ""
            val queryParams = parseQueryParams(queryPart)
            val hostPort = extractHostAndPort(hostPortPart, 443)

            val rawSni = queryParams["sni"] ?: queryParams["peer"] ?: hostPort.first
            val cleanSni = normalizeHostTypos(rawSni)
            val path = URLDecoder.decode(queryParams["path"] ?: "", StandardCharsets.UTF_8.name())

            return ShareLinkConfig(
                protocol = "trojan",
                uuidOrPassword = password,
                host = normalizeHostTypos(hostPort.first),
                port = hostPort.second,
                sni = cleanSni,
                path = path,
                transport = queryParams["type"] ?: "tcp",
                security = queryParams["security"] ?: "tls",
                remarks = remarks,
                rawOriginal = link
            )
        } catch (_: Exception) {
            return null
        }
    }

    private fun parseVmess(link: String): ShareLinkConfig? {
        try {
            val b64 = link.substringAfter("://").trim()
            val decodedStr = decodeBase64Safe(b64) ?: return null
            val json = JSONObject(decodedStr)

            val host = normalizeHostTypos(json.optString("add", ""))
            val port = json.optInt("port", 443)
            val id = json.optString("id", "")
            val sni = normalizeHostTypos(json.optString("sni", host))
            val path = json.optString("path", "")
            val net = json.optString("net", "tcp")
            val tls = json.optString("tls", "tls")
            val ps = json.optString("ps", "")

            return ShareLinkConfig(
                protocol = "vmess",
                uuidOrPassword = id,
                host = host,
                port = port,
                sni = if (sni.isNotEmpty()) sni else host,
                path = path,
                transport = net,
                security = if (tls.isNotEmpty()) tls else "tls",
                remarks = ps,
                rawOriginal = link
            )
        } catch (_: Exception) {
            return null
        }
    }

    private fun parseShadowsocks(link: String): ShareLinkConfig? {
        try {
            val content = link.substringAfter("://")
            val remarks = if (content.contains("#")) {
                URLDecoder.decode(content.substringAfter("#"), StandardCharsets.UTF_8.name())
            } else ""
            val withoutRemarks = content.substringBefore("#")

            if (withoutRemarks.contains("@")) {
                // SIP002 format: ss://method:password@hostname:port
                val encodedUserInfo = withoutRemarks.substringBefore("@")
                val hostPort = extractHostAndPort(withoutRemarks.substringAfter("@"), 8388)
                val userInfo = decodeBase64Safe(encodedUserInfo) ?: encodedUserInfo
                return ShareLinkConfig(
                    protocol = "ss",
                    uuidOrPassword = userInfo,
                    host = normalizeHostTypos(hostPort.first),
                    port = hostPort.second,
                    sni = hostPort.first,
                    remarks = remarks,
                    rawOriginal = link
                )
            } else {
                // Legacy base64 format
                val decoded = decodeBase64Safe(withoutRemarks) ?: return null
                val user = decoded.substringBefore("@")
                val hostPort = extractHostAndPort(decoded.substringAfter("@"), 8388)
                return ShareLinkConfig(
                    protocol = "ss",
                    uuidOrPassword = user,
                    host = normalizeHostTypos(hostPort.first),
                    port = hostPort.second,
                    sni = hostPort.first,
                    remarks = remarks,
                    rawOriginal = link
                )
            }
        } catch (_: Exception) {
            return null
        }
    }

    fun rewriteWithCleanIp(config: ShareLinkConfig, cleanIp: String): String {
        val bracketedIp = if (NetUtils.isIpv6(cleanIp)) "[${NetUtils.stripBrackets(cleanIp)}]" else cleanIp

        return when (config.protocol) {
            "vless" -> {
                val remark = if (config.remarks.isNotEmpty()) "${config.remarks}-CleanIP" else "CleanIP"
                "vless://${config.uuidOrPassword}@$bracketedIp:${config.port}?type=${config.transport}&security=${config.security}&sni=${config.sni}&path=${config.path}&host=${config.sni}#$remark"
            }
            "trojan" -> {
                val remark = if (config.remarks.isNotEmpty()) "${config.remarks}-CleanIP" else "CleanIP"
                "trojan://${config.uuidOrPassword}@$bracketedIp:${config.port}?type=${config.transport}&security=${config.security}&sni=${config.sni}&path=${config.path}#$remark"
            }
            "vmess" -> {
                val json = JSONObject()
                json.put("v", "2")
                json.put("ps", if (config.remarks.isNotEmpty()) "${config.remarks}-CleanIP" else "CleanIP")
                json.put("add", cleanIp)
                json.put("port", config.port)
                json.put("id", config.uuidOrPassword)
                json.put("net", config.transport)
                json.put("type", "none")
                json.put("host", config.sni)
                json.put("path", config.path)
                json.put("tls", config.security)
                json.put("sni", config.sni)
                val b64Encoded = try {
                    Base64.encodeToString(json.toString().toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
                } catch (_: Throwable) {
                    java.util.Base64.getEncoder().encodeToString(json.toString().toByteArray(Charsets.UTF_8))
                }
                "vmess://$b64Encoded"
            }
            "ss" -> {
                val remark = if (config.remarks.isNotEmpty()) "${config.remarks}-CleanIP" else "CleanIP"
                "ss://${config.uuidOrPassword}@$bracketedIp:${config.port}#$remark"
            }
            else -> config.rawOriginal
        }
    }

    fun generateSingBoxOutbound(config: ShareLinkConfig, cleanIp: String): String {
        val json = JSONObject()
        json.put("type", config.protocol)
        json.put("tag", "proxy-clean-ip")
        json.put("server", cleanIp)
        json.put("server_port", config.port)
        if (config.protocol == "vless" || config.protocol == "vmess") {
            json.put("uuid", config.uuidOrPassword)
        } else if (config.protocol == "trojan") {
            json.put("password", config.uuidOrPassword)
        }

        val tls = JSONObject()
        tls.put("enabled", config.security == "tls" || config.security == "reality")
        tls.put("server_name", config.sni)
        json.put("tls", tls)

        if (config.transport == "ws") {
            val transport = JSONObject()
            transport.put("type", "ws")
            transport.put("path", config.path)
            val headers = JSONObject()
            headers.put("Host", config.sni)
            transport.put("headers", headers)
            json.put("transport", transport)
        }

        return json.toString(2)
    }

    fun generateClashProxy(config: ShareLinkConfig, cleanIp: String): String {
        return buildString {
            appendLine("- name: Clean-IP-Cloudflare")
            appendLine("  type: ${config.protocol}")
            appendLine("  server: $cleanIp")
            appendLine("  port: ${config.port}")
            if (config.protocol == "vless" || config.protocol == "vmess") {
                appendLine("  uuid: ${config.uuidOrPassword}")
                appendLine("  alterId: 0")
                appendLine("  cipher: auto")
            } else if (config.protocol == "trojan") {
                appendLine("  password: ${config.uuidOrPassword}")
            }
            appendLine("  tls: true")
            appendLine("  servername: ${config.sni}")
            if (config.transport == "ws") {
                appendLine("  network: ws")
                appendLine("  ws-opts:")
                appendLine("    path: ${config.path}")
                appendLine("    headers:")
                appendLine("      Host: ${config.sni}")
            }
        }
    }

    /**
     * Rewrites a VLESS link by replacing its host/IP with the clean IP and updating remarks.
     * Preserves all other query parameters (type, security, path, flow, fp, pbk, sid, etc.).
     */
    fun rewriteVlessWithIp(
        originalLink: String,
        cleanIp: String,
        port: Int? = null,
        remarkLabel: String = ""
    ): String {
        val trimmed = originalLink.trim()
        if (!trimmed.startsWith("vless://", ignoreCase = true)) {
            val parsed = parse(trimmed)
            return if (parsed != null) rewriteWithCleanIp(parsed, cleanIp) else trimmed
        }

        try {
            val content = trimmed.substringAfter("://")
            val originalRemarks = if (content.contains("#")) {
                try {
                    URLDecoder.decode(content.substringAfter("#"), StandardCharsets.UTF_8.name())
                } catch (_: Exception) {
                    content.substringAfter("#")
                }
            } else ""

            val withoutRemarks = content.substringBefore("#")
            val uuid = withoutRemarks.substringBefore("@")
            val afterAt = withoutRemarks.substringAfter("@")

            val hostPortPart: String
            val queryPart: String
            if (afterAt.contains("?")) {
                hostPortPart = afterAt.substringBefore("?")
                queryPart = afterAt.substringAfter("?")
            } else {
                hostPortPart = afterAt
                queryPart = ""
            }

            val hostPort = extractHostAndPort(hostPortPart, 443)
            val originalHost = hostPort.first
            val originalPort = port ?: hostPort.second

            val queryParams = parseQueryParams(queryPart).toMutableMap()

            // If SNI was not explicitly in query and original host is a domain name, preserve as SNI
            if (!queryParams.containsKey("sni") && !NetUtils.isIpv4(originalHost) && !NetUtils.isIpv6(originalHost)) {
                queryParams["sni"] = originalHost
            }
            // If host header was not set, default host to sni
            if (!queryParams.containsKey("host") && queryParams.containsKey("sni")) {
                queryParams["host"] = queryParams["sni"] ?: originalHost
            }

            val bracketedIp = if (NetUtils.isIpv6(cleanIp)) "[${NetUtils.stripBrackets(cleanIp)}]" else cleanIp
            val newQueryString = queryParams.entries.joinToString("&") { (k, v) -> "$k=$v" }
            val querySuffix = if (newQueryString.isNotEmpty()) "?$newQueryString" else ""

            val newRemark = if (remarkLabel.isNotEmpty()) {
                remarkLabel
            } else if (originalRemarks.isNotEmpty()) {
                "$originalRemarks [$cleanIp]"
            } else {
                "CF-CleanIP [$cleanIp]"
            }

            return "vless://$uuid@$bracketedIp:$originalPort$querySuffix#$newRemark"
        } catch (_: Exception) {
            val parsed = parse(trimmed)
            return if (parsed != null) rewriteWithCleanIp(parsed, cleanIp) else trimmed
        }
    }

    /**
     * Generates up to maxCount (default 20) different VLESS configs from a single base VLESS link,
     * each routed to one of the top clean IP addresses.
     */
    fun generateTop20VlessConfigs(
        originalLink: String,
        cleanIps: List<com.example.core.model.ProbeResult>,
        maxCount: Int = 20
    ): List<String> {
        val targets = cleanIps.take(maxCount)
        val trimmed = originalLink.trim()
        if (targets.isEmpty() || trimmed.isEmpty()) return emptyList()

        return targets.mapIndexed { index, result ->
            val rank = index + 1
            val rankStr = if (rank < 10) "0$rank" else "$rank"
            val latencyStr = "${result.avgLatencyMs.toInt()}ms"
            val coloStr = if (result.colo.isNotEmpty()) " (${result.colo})" else ""
            val customRemark = "CF-$rankStr [${result.ip}]$coloStr $latencyStr"
            rewriteVlessWithIp(
                originalLink = trimmed,
                cleanIp = result.ip,
                port = result.port,
                remarkLabel = customRemark
            )
        }
    }

    fun decodeBase64Safe(input: String): String? {
        val sanitized = input.trim().replace("-", "+").replace("_", "/")
        val padded = when (sanitized.length % 4) {
            2 -> "$sanitized=="
            3 -> "$sanitized="
            else -> sanitized
        }
        return try {
            val bytes = try {
                Base64.decode(padded, Base64.DEFAULT)
            } catch (_: Throwable) {
                java.util.Base64.getDecoder().decode(padded)
            }
            String(bytes, StandardCharsets.UTF_8)
        } catch (_: Exception) {
            null
        }
    }

    private fun extractHostAndPort(hostPort: String, defaultPort: Int): Pair<String, Int> {
        val trimmed = hostPort.trim()
        if (trimmed.startsWith("[")) {
            val endBracket = trimmed.indexOf(']')
            if (endBracket != -1) {
                val host = trimmed.substring(1, endBracket)
                val portStr = trimmed.substring(endBracket + 1).removePrefix(":")
                val port = portStr.toIntOrNull() ?: defaultPort
                return Pair(host, port)
            }
        }

        val lastColon = trimmed.lastIndexOf(':')
        return if (lastColon != -1 && !trimmed.contains("]")) {
            val host = trimmed.substring(0, lastColon)
            val port = trimmed.substring(lastColon + 1).toIntOrNull() ?: defaultPort
            Pair(host, port)
        } else {
            Pair(trimmed, defaultPort)
        }
    }

    private fun parseQueryParams(query: String): Map<String, String> {
        if (query.isEmpty()) return emptyMap()
        val result = mutableMapOf<String, String>()
        for (param in query.split("&")) {
            val eq = param.indexOf('=')
            if (eq != -1) {
                val k = param.substring(0, eq).trim().lowercase()
                val v = param.substring(eq + 1).trim()
                result[k] = v
            }
        }
        return result
    }

    fun normalizeHostTypos(host: String): String {
        return host.trim()
            .replace("worers.dev", "workers.dev")
            .replace("coludflare.com", "cloudflare.com")
            .replace("cloudflar.com", "cloudflare.com")
    }
}
