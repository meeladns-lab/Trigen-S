package com.example.core.model

data class IspInfo(
    val asOrganization: String = "Detecting...",
    val asn: String = "AS---",
    val country: String = "---",
    val clientIp: String = "---",
    val colo: String = "---",
    val isOfflineDetected: Boolean = false,
    val isIpv6Available: Boolean = false,
    val source: String = "Cloudflare Meta"
)

data class ScanProgress(
    val isRunning: Boolean = false,
    val totalCandidates: Int = 0,
    val testedCount: Int = 0,
    val healthyCount: Int = 0,
    val currentTarget: String = "",
    val elapsedSeconds: Long = 0,
    val etaSeconds: Long = 0,
    val coloDistribution: Map<String, Int> = emptyMap(),
    val failureStats: Map<FailureClass, Int> = emptyMap(),
    val isProxyDetected: Boolean = false,
    val proxyWarningMessage: String = "",
    val averageHealthyLatencyMs: Double = 0.0
)
