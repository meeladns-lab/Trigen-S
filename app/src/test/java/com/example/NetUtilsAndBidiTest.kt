package com.example

import com.example.core.probe.NetUtils
import com.example.ui.i18n.Bidi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NetUtilsAndBidiTest {

    @Test
    fun testIpv6BracketFormatting() {
        // §1.8: Bracket notation everywhere for IPv6
        val v4 = NetUtils.formatHostPort("104.16.0.1", 443)
        assertEquals("104.16.0.1:443", v4)

        val v6 = NetUtils.formatHostPort("2606:4700::1", 443)
        assertEquals("[2606:4700::1]:443", v6)

        // Already bracketed shouldn't double-bracket
        val alreadyBracketed = NetUtils.formatHostPort("[2606:4700::1]", 8443)
        assertEquals("[2606:4700::1]:8443", alreadyBracketed)
    }

    @Test
    fun testBidiIsolation() {
        val raw = "104.16.0.1:443"
        val isolated = Bidi.isolate(raw)
        assertTrue(isolated.startsWith("\u2068"))
        assertTrue(isolated.endsWith("\u2069"))
        assertEquals("\u2068104.16.0.1:443\u2069", isolated)
    }
}
