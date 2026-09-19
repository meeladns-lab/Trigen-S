package com.example.core.model

data class ShareLinkConfig(
    val protocol: String, // vless, trojan, vmess, ss
    val uuidOrPassword: String,
    val host: String,
    val port: Int,
    val sni: String,
    val path: String = "",
    val transport: String = "tcp", // tcp, ws, grpc, xhttp
    val security: String = "tls", // tls, reality, none
    val remarks: String = "",
    val rawOriginal: String = ""
) {
    fun getRedactedString(): String {
        val safePort = port
        val safeHost = host
        return "$protocol://***@$safeHost:$safePort?type=$transport&security=$security&sni=$sni#$remarks"
    }
}
