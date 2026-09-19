package com.example

import com.example.core.ipsource.CidrParser
import com.example.core.ipsource.IpSampler
import com.example.core.ipsource.Ipv4Cidr
import com.example.core.ipsource.Ipv4DashRange
import com.example.core.ipsource.Ipv6Cidr
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random

class CidrAndSamplingTest {

    @Test
    fun testIpv4CidrParsing() {
        val cidr = Ipv4Cidr.parse("104.16.0.0/13")
        assertNotNull(cidr)
        assertEquals(13, cidr!!.prefixLength)
        assertEquals(524288L, cidr.totalAddresses)

        val single = Ipv4Cidr.parse("1.1.1.1")
        assertNotNull(single)
        assertEquals(32, single!!.prefixLength)
        assertEquals(1L, single.totalAddresses)
    }

    @Test
    fun testIpv4DashRangeParsing() {
        val dash = Ipv4DashRange.parse("104.16.0.1-104.16.0.50")
        assertNotNull(dash)
        assertEquals(50L, dash!!.estimatedSize)

        val shortDash = Ipv4DashRange.parse("192.168.1.10-20")
        assertNotNull(shortDash)
        assertEquals(11L, shortDash!!.estimatedSize)
    }

    @Test
    fun testIpv6CidrParsing() {
        val v6 = Ipv6Cidr.parse("2606:4700::/32")
        assertNotNull(v6)
        assertEquals(32, v6!!.prefixLength)
        assertTrue(v6.isIpv6)

        val sample = v6.sampleAddress(Random(42))
        assertTrue(sample.contains(":"))
    }

    @Test
    fun testWeightedSamplingAndDeduplication() {
        val ranges = listOfNotNull(
            CidrParser.parseLine("104.16.0.0/13"),
            CidrParser.parseLine("103.21.244.0/22"),
            CidrParser.parseLine("1.1.1.1-1.1.1.100")
        )
        val sampled = IpSampler.sample(ranges, 50, random = Random(1234))
        assertEquals(50, sampled.size)
        // Rule: De-duplicate every emitted address
        val distinctCount = sampled.distinct().size
        assertEquals("Every emitted address must be distinct", 50, distinctCount)
    }
}
