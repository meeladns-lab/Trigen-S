package com.example.core.probe

data class TimeoutBudget(
    val totalTimeoutMs: Long,
    val dialTimeoutMs: Long,
    val tlsTimeoutMs: Long,
    val httpTimeoutMs: Long
) {
    init {
        // §1.2 enforcement: TCP dial <= T/4, TLS handshake <= T/2, HTTP response >= T/4
        require(dialTimeoutMs <= totalTimeoutMs / 4) {
            "Dial timeout $dialTimeoutMs must be <= totalTimeout/4 (${totalTimeoutMs / 4})"
        }
        require(tlsTimeoutMs <= totalTimeoutMs / 2) {
            "TLS timeout $tlsTimeoutMs must be <= totalTimeout/2 (${totalTimeoutMs / 2})"
        }
        require(httpTimeoutMs >= totalTimeoutMs / 4) {
            "HTTP timeout $httpTimeoutMs must be >= totalTimeout/4 (${totalTimeoutMs / 4})"
        }
        require(dialTimeoutMs + tlsTimeoutMs + httpTimeoutMs <= totalTimeoutMs) {
            "Sum of phase budgets exceeds total timeout budget"
        }
    }

    companion object {
        fun fromTotalSeconds(totalSeconds: Int): TimeoutBudget {
            val totalMs = (totalSeconds.coerceAtLeast(2) * 1000L)
            return fromTotalMs(totalMs)
        }

        fun fromTotalMs(totalMs: Long): TimeoutBudget {
            val dial = totalMs / 4
            val tls = totalMs / 2
            val http = totalMs - dial - tls // exactly totalMs / 4
            return TimeoutBudget(
                totalTimeoutMs = totalMs,
                dialTimeoutMs = dial,
                tlsTimeoutMs = tls,
                httpTimeoutMs = http
            )
        }
    }
}
