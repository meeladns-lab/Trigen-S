package com.example

import com.example.core.probe.TimeoutBudget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TimeoutBudgetTest {

    @Test
    fun testTimeoutBudgetSplit5Seconds() {
        val budget = TimeoutBudget.fromTotalSeconds(5)
        assertEquals(5000L, budget.totalTimeoutMs)
        // §1.2: TCP dial <= T/4, TLS handshake <= T/2, HTTP response >= T/4
        assertTrue("Dial timeout must be <= T/4", budget.dialTimeoutMs <= budget.totalTimeoutMs / 4)
        assertTrue("TLS timeout must be <= T/2", budget.tlsTimeoutMs <= budget.totalTimeoutMs / 2)
        assertTrue("HTTP timeout must be >= T/4", budget.httpTimeoutMs >= budget.totalTimeoutMs / 4)
        assertEquals(1250L, budget.dialTimeoutMs)
        assertEquals(2500L, budget.tlsTimeoutMs)
        assertEquals(1250L, budget.httpTimeoutMs)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testInvalidTimeoutBudgetThrows() {
        // Violating Dial <= T/4
        TimeoutBudget(
            totalTimeoutMs = 4000L,
            dialTimeoutMs = 2000L, // > 1000
            tlsTimeoutMs = 1000L,
            httpTimeoutMs = 1000L
        )
    }
}
