package com.example.core.ipsource

import java.net.InetAddress
import java.util.Random

object IpSampler {

    /**
     * Stratified uniform randomized sampling across all ranges.
     * Ensures equal representation across all subnets rather than crowding into large blocks,
     * and thoroughly shuffles candidate order.
     */
    fun sampleRandomized(
        ranges: List<ParsedRange>,
        targetCount: Int,
        knownLiveIpv6Prefixes: Set<String> = emptySet(),
        random: Random = Random()
    ): List<String> {
        if (ranges.isEmpty() || targetCount <= 0) return emptyList()

        val results = LinkedHashSet<String>()

        // 1. If known live IPv6 prefixes are provided, add seed addresses
        for (prefix in knownLiveIpv6Prefixes) {
            val v6 = Ipv6Cidr.parse(prefix)
            if (v6 != null) {
                for (i in 0 until 5) {
                    val addr = v6.sampleAddress(random)
                    results.add(addr)
                    if (results.size >= targetCount) return results.toList().shuffled(random)
                }
            }
        }

        // 2. Stratified round-robin across all ranges to guarantee uniform coverage
        val perRangeTarget = (targetCount / ranges.size).coerceAtLeast(3)
        val maxAttempts = targetCount * 30
        var attempts = 0

        // Round 1: Sample evenly from each range
        for (range in ranges.shuffled(random)) {
            var rangeSamples = 0
            var rangeAttempts = 0
            while (rangeSamples < perRangeTarget && rangeAttempts < 80 && results.size < targetCount) {
                rangeAttempts++
                val addr = range.sampleAddress(random)
                if (results.add(addr)) {
                    rangeSamples++
                }
            }
        }

        // Round 2: Fill remaining quota with balanced random picks
        var rangeIdx = 0
        val shuffledRanges = ranges.shuffled(random)
        while (results.size < targetCount && attempts < maxAttempts) {
            attempts++
            val range = shuffledRanges[rangeIdx % shuffledRanges.size]
            rangeIdx++
            val addr = range.sampleAddress(random)
            results.add(addr)
        }

        return results.toList().shuffled(random)
    }

    /**
     * Weighted sampling across ranges proportional to range size, ensuring all emitted addresses are unique.
     */
    fun sample(
        ranges: List<ParsedRange>,
        targetCount: Int,
        knownLiveIpv6Prefixes: Set<String> = emptySet(),
        random: Random = Random()
    ): List<String> {
        return sampleRandomized(ranges, targetCount, knownLiveIpv6Prefixes, random)
    }

    /**
     * Generates neighbor addresses around a responsive target IP.
     * Respects bounds, deduplicates, and limits neighbor count.
     */
    fun generateNeighbors(ip: String, radius: Int = 32, maxNeighbors: Int = 12, random: Random = Random()): List<String> {
        if (ip.contains(":")) {
            // IPv6 neighbor expansion: vary last 8 bits
            return try {
                val inet = InetAddress.getByName(ip)
                val bytes = inet.address
                val results = mutableListOf<String>()
                val originalLast = bytes[15].toInt() and 0xFF
                for (offset in 1..radius) {
                    if (results.size >= maxNeighbors) break
                    val targetLast = (originalLast + offset) and 0xFF
                    val clone = bytes.clone()
                    clone[15] = targetLast.toByte()
                    val neighbor = InetAddress.getByAddress(clone).hostAddress
                    if (neighbor != null && neighbor != ip) {
                        results.add(neighbor)
                    }
                }
                results
            } catch (_: Exception) {
                emptyList()
            }
        }

        // IPv4 neighbor expansion
        val ipInt = Ipv4Cidr.ipv4ToLong(ip) ?: return emptyList()
        val results = mutableListOf<String>()
        val offsets = mutableListOf<Int>()
        for (d in 1..radius) {
            offsets.add(d)
            offsets.add(-d)
        }
        offsets.shuffle(random)

        for (offset in offsets) {
            if (results.size >= maxNeighbors) break
            val candidate = ipInt + offset
            // Stay within same /24
            if ((candidate and 0xFFFFFF00L) == (ipInt and 0xFFFFFF00L)) {
                val host = candidate and 0xFF
                if (host in 1..254 && candidate != ipInt) {
                    results.add(Ipv4Cidr.longToIpv4(candidate))
                }
            }
        }
        return results
    }
}
