package com.example.core.ipsource

import java.math.BigInteger
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.util.Random

sealed class ParsedRange {
    abstract val isIpv6: Boolean
    abstract val estimatedSize: Long
    abstract fun sampleAddress(random: Random): String
}

data class Ipv4Cidr(
    val startInt: Long,
    val prefixLength: Int,
    val totalAddresses: Long,
    val rawString: String
) : ParsedRange() {
    override val isIpv6: Boolean = false
    override val estimatedSize: Long = totalAddresses

    override fun sampleAddress(random: Random): String {
        if (totalAddresses <= 1) return longToIpv4(startInt)
        // Avoid network (0) and broadcast (255) for /24 or larger ranges if possible
        val offset = if (totalAddresses > 4) {
            1L + (random.nextDouble() * (totalAddresses - 2)).toLong().coerceIn(0L, totalAddresses - 3)
        } else {
            (random.nextDouble() * totalAddresses).toLong().coerceIn(0L, totalAddresses - 1)
        }
        return longToIpv4(startInt + offset)
    }

    /**
     * Enumerates /24 sub-blocks inside this CIDR to ensure uniform distribution
     */
    fun sampleOnePerSlash24(random: Random): List<String> {
        if (prefixLength >= 24) {
            return listOf(sampleAddress(random))
        }
        val subBlocks = 1 shl (24 - prefixLength)
        val result = mutableListOf<String>()
        for (i in 0 until subBlocks) {
            val blockBase = startInt + (i.toLong() shl 8)
            val host = 1 + random.nextInt(253) // 1..254
            result.add(longToIpv4(blockBase + host))
        }
        return result
    }

    companion object {
        fun parse(cidr: String): Ipv4Cidr? {
            return try {
                val parts = cidr.trim().split("/")
                val ip = parts[0].trim()
                val prefix = if (parts.size > 1) parts[1].trim().toInt() else 32
                if (prefix !in 0..32) return null
                val ipInt = ipv4ToLong(ip) ?: return null
                val mask = if (prefix == 0) 0L else (-1L shl (32 - prefix)) and 0xFFFFFFFFL
                val base = ipInt and mask
                val total = 1L shl (32 - prefix)
                Ipv4Cidr(base, prefix, total, cidr.trim())
            } catch (_: Exception) {
                null
            }
        }

        fun ipv4ToLong(ip: String): Long? {
            return try {
                val parts = ip.split(".")
                if (parts.size != 4) return null
                var result = 0L
                for (p in parts) {
                    val octet = p.toInt()
                    if (octet !in 0..255) return null
                    result = (result shl 8) or octet.toLong()
                }
                result and 0xFFFFFFFFL
            } catch (_: Exception) {
                null
            }
        }

        fun longToIpv4(value: Long): String {
            val v = value and 0xFFFFFFFFL
            return "${(v shr 24) and 0xFF}.${(v shr 16) and 0xFF}.${(v shr 8) and 0xFF}.${v and 0xFF}"
        }
    }
}

data class Ipv4DashRange(
    val startInt: Long,
    val endInt: Long,
    val rawString: String
) : ParsedRange() {
    override val isIpv6: Boolean = false
    override val estimatedSize: Long = (endInt - startInt + 1).coerceAtLeast(1)

    override fun sampleAddress(random: Random): String {
        val count = endInt - startInt + 1
        if (count <= 1) return Ipv4Cidr.longToIpv4(startInt)
        val offset = (random.nextDouble() * count).toLong().coerceIn(0L, count - 1)
        return Ipv4Cidr.longToIpv4(startInt + offset)
    }

    companion object {
        fun parse(range: String): Ipv4DashRange? {
            val parts = range.split("-")
            if (parts.size != 2) return null
            val start = parts[0].trim()
            val end = parts[1].trim()
            val startInt = Ipv4Cidr.ipv4ToLong(start) ?: return null
            val endInt = if (end.contains(".")) {
                Ipv4Cidr.ipv4ToLong(end) ?: return null
            } else {
                // e.g. 1.2.3.4-200
                val lastOctet = end.toIntOrNull() ?: return null
                if (lastOctet !in 0..255) return null
                (startInt and 0xFFFFFF00L) or lastOctet.toLong()
            }
            if (endInt < startInt) return null
            return Ipv4DashRange(startInt, endInt, range)
        }
    }
}

data class Ipv6Cidr(
    val baseAddress: ByteArray, // 16 bytes
    val prefixLength: Int,
    val rawString: String
) : ParsedRange() {
    override val isIpv6: Boolean = true
    // Cap estimated size to prevent overflow (e.g. for /32)
    override val estimatedSize: Long = 1_000_000L

    override fun sampleAddress(random: Random): String {
        return sampleRandomInPrefix(random, 48)
    }

    /**
     * Samples a random /48 inside this prefix, and an address inside that /48 (stage 1 discovery)
     */
    fun sampleRandomInPrefix(random: Random, targetSubnetPrefix: Int = 48): String {
        val bytes = baseAddress.clone()
        // Randomize bits between prefixLength and targetSubnetPrefix
        val startByte = prefixLength / 8
        val endByte = (targetSubnetPrefix / 8).coerceAtMost(16)
        for (i in startByte until endByte) {
            val rnd = random.nextInt(256)
            bytes[i] = rnd.toByte()
        }
        // Set fixed low host bytes, common for Cloudflare edge anycast (e.g. ::1 or ::100)
        for (i in 8 until 15) {
            bytes[i] = 0
        }
        bytes[15] = (1 + random.nextInt(250)).toByte()
        return try {
            InetAddress.getByAddress(bytes).hostAddress ?: rawString
        } catch (_: Exception) {
            rawString
        }
    }

    companion object {
        fun parse(cidr: String): Ipv6Cidr? {
            return try {
                val parts = cidr.trim().split("/")
                val ipStr = parts[0].trim()
                val prefix = if (parts.size > 1) parts[1].trim().toInt() else 128
                if (prefix !in 0..128) return null
                val inet = InetAddress.getByName(ipStr)
                if (inet !is Inet6Address) return null
                val bytes = inet.address
                Ipv6Cidr(bytes, prefix, cidr.trim())
            } catch (_: Exception) {
                null
            }
        }
    }
}

object CidrParser {
    fun parseLine(line: String): ParsedRange? {
        val trimmed = line.trim()
        if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("//")) return null

        // If line is CSV, extract first field
        val firstField = if (trimmed.contains(",")) trimmed.split(",")[0].trim() else trimmed

        // Check dash range
        if (firstField.contains("-")) {
            val dash = Ipv4DashRange.parse(firstField)
            if (dash != null) return dash
        }

        // Check IPv6
        if (firstField.contains(":")) {
            val v6 = Ipv6Cidr.parse(firstField)
            if (v6 != null) return v6
        }

        // Check IPv4 CIDR or single IP
        val v4 = Ipv4Cidr.parse(firstField)
        if (v4 != null) return v4

        return null
    }

    fun parseAll(text: String): List<ParsedRange> {
        return text.lines()
            .mapNotNull { parseLine(it) }
    }
}
