package com.example.core.model

data class AttemptRecord(
    val attemptNumber: Int,
    val tcpTimeMs: Long = -1,
    val tlsTimeMs: Long = -1,
    val httpTimeMs: Long = -1,
    val totalTimeMs: Long = -1,
    val isSuccess: Boolean = false,
    val failureClass: FailureClass = FailureClass.NONE,
    val errorMessage: String = "",
    val colo: String = "",
    val clientEgressIp: String = "",
    val warp: String = "",
    val tlsVersion: String = ""
)

data class ProbeResult(
    val ip: String,
    val port: Int,
    val isIpv6: Boolean,
    val attempts: List<AttemptRecord>,
    val successCount: Int,
    val totalAttempts: Int = 4,
    val minLatencyMs: Double,
    val avgLatencyMs: Double,
    val maxLatencyMs: Double,
    val jitterMs: Double,
    val packetLossPercent: Double,
    val colo: String = "",
    val clientEgressIp: String = "",
    val warp: String = "",
    val tlsVersion: String = "",
    val primaryFailure: FailureClass = FailureClass.NONE,
    val throughputMbps: Double? = null,
    val uploadSpeedMbps: Double? = null,
    val ttfbMs: Long? = null,
    val score: Double = 999999.0,
    val isHealthy: Boolean = false,
    val isNeighbor: Boolean = false
) {
    val hostPortString: String
        get() = if (isIpv6) "[$ip]:$port" else "$ip:$port"

    val isLossFree: Boolean
        get() = packetLossPercent == 0.0

    val downloadSpeedDisplay: String
        get() = if (throughputMbps != null && throughputMbps > 0.0) {
            String.format(java.util.Locale.US, "%.2f MB/s", throughputMbps)
        } else {
            "n/a"
        }

    val uploadSpeedDisplay: String
        get() = if (uploadSpeedMbps != null && uploadSpeedMbps > 0.0) {
            String.format(java.util.Locale.US, "%.2f MB/s", uploadSpeedMbps)
        } else {
            "n/a"
        }

    val speedDisplay: String
        get() = when {
            throughputMbps != null && uploadSpeedMbps != null -> {
                String.format(java.util.Locale.US, "↓%.1f  ↑%.1f MB/s", throughputMbps, uploadSpeedMbps)
            }
            throughputMbps != null -> {
                String.format(java.util.Locale.US, "↓%.2f MB/s", throughputMbps)
            }
            uploadSpeedMbps != null -> {
                String.format(java.util.Locale.US, "↑%.2f MB/s", uploadSpeedMbps)
            }
            else -> "n/a"
        }
}
