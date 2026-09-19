package com.example.core.probe

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.Inet6Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket

object NetUtils {

    /**
     * Strictly formats host and port adhering to RFC 3986.
     * Always wraps IPv6 addresses in brackets: [2606:4700::1]:443
     */
    fun formatHostPort(host: String, port: Int): String {
        val cleanHost = stripBrackets(host)
        return if (isIpv6(cleanHost)) {
            "[$cleanHost]:$port"
        } else {
            "$cleanHost:$port"
        }
    }

    /**
     * Formats URL with candidate host/IP and port
     */
    fun formatUrl(protocol: String, host: String, port: Int, path: String = ""): String {
        val hostPort = formatHostPort(host, port)
        val normalizedPath = if (path.startsWith("/")) path else "/$path"
        val isDefaultPort = (protocol == "http" && port == 80) || (protocol == "https" && port == 443)
        return if (isDefaultPort && !isIpv6(host)) {
            "$protocol://$host$normalizedPath"
        } else {
            "$protocol://$hostPort$normalizedPath"
        }
    }

    fun stripBrackets(host: String): String {
        val trimmed = host.trim()
        return if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
            trimmed.substring(1, trimmed.length - 1)
        } else {
            trimmed
        }
    }

    fun isIpv6(ip: String): Boolean {
        val clean = stripBrackets(ip)
        return clean.contains(":")
    }

    fun isIpv4(ip: String): Boolean {
        val parts = ip.trim().split(".")
        if (parts.size != 4) return false
        return parts.all { part ->
            val num = part.toIntOrNull()
            num != null && num in 0..255 && (part == "0" || !part.startsWith("0"))
        }
    }

    /**
     * Checks if the device has a working global IPv6 address and can establish an IPv6 TCP handshake.
     */
    suspend fun checkIpv6Connectivity(): Boolean = withContext(Dispatchers.IO) {
        try {
            // First check if any network interface has a non-link-local IPv6 address
            var hasGlobalIpv6 = false
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                if (!iface.isUp || iface.isLoopback) continue
                val addrs = iface.inetAddresses
                while (addrs.hasMoreElements()) {
                    val addr = addrs.nextElement()
                    if (addr is Inet6Address && !addr.isLinkLocalAddress && !addr.isLoopbackAddress && !addr.isSiteLocalAddress) {
                        hasGlobalIpv6 = true
                        break
                    }
                }
                if (hasGlobalIpv6) break
            }

            if (!hasGlobalIpv6) return@withContext false

            // Second: probe reachability to Cloudflare public DNS IPv6 (2606:4700:4700::1111:53) with 2s timeout
            Socket().use { socket ->
                socket.connect(InetSocketAddress("2606:4700:4700::1111", 53), 2000)
                true
            }
        } catch (_: Exception) {
            false
        }
    }
}
