package com.example.core.netinfo

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.example.core.ipsource.BundledRanges
import com.example.core.model.IspInfo
import com.example.core.probe.NetUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class IspDetector(private val context: Context) {

    private val _ispInfo = MutableStateFlow(IspInfo())
    val ispInfo: StateFlow<IspInfo> = _ispInfo

    private val _networkChangeEvent = MutableStateFlow<String?>(null)
    val networkChangeEvent: StateFlow<String?> = _networkChangeEvent

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    init {
        registerNetworkCallback()
    }

    private fun registerNetworkCallback() {
        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()

            cm?.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    val caps = cm.getNetworkCapabilities(network)
                    val isWifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
                    val isCellular = caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
                    val type = if (isWifi) "Wi-Fi" else if (isCellular) "Mobile Data" else "Network"
                    _networkChangeEvent.value = "Network connected: $type"
                }

                override fun onLost(network: Network) {
                    _networkChangeEvent.value = "Network disconnected"
                }
            })
        } catch (_: Exception) {}
    }

    fun clearNetworkChangeEvent() {
        _networkChangeEvent.value = null
    }

    suspend fun detectIsp(): IspInfo = withContext(Dispatchers.IO) {
        val ipv6Supported = NetUtils.checkIpv6Connectivity()

        // 1. Try Cloudflare Meta
        try {
            val req = Request.Builder()
                .url("https://speed.cloudflare.com/meta")
                .header("User-Agent", "CleanIPScanner/1.0")
                .build()

            httpClient.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body?.string() ?: ""
                    val json = JSONObject(body)
                    val asnNum = json.optInt("asn", 0)
                    val asOrg = json.optString("asOrganization", "")
                    val clientIp = json.optString("clientIp", "")
                    val colo = json.optString("colo", "")
                    val country = json.optString("country", "")

                    val matchedOffline = BundledRanges.IRANIAN_ISPS.firstOrNull { it.asn == asnNum }
                    val finalOrg = matchedOffline?.let { "${it.nameEn} (${it.nameFa})" }
                        ?: if (asOrg.isNotEmpty()) asOrg else "AS$asnNum"

                    val info = IspInfo(
                        asOrganization = finalOrg,
                        asn = "AS$asnNum",
                        country = country,
                        clientIp = clientIp,
                        colo = colo,
                        isOfflineDetected = false,
                        isIpv6Available = ipv6Supported,
                        source = "speed.cloudflare.com/meta"
                    )
                    _ispInfo.value = info
                    return@withContext info
                }
            }
        } catch (_: Exception) {}

        // 2. Try ipwho.is fallback
        try {
            val req = Request.Builder()
                .url("https://ipwho.is/")
                .header("User-Agent", "CleanIPScanner/1.0")
                .build()

            httpClient.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body?.string() ?: ""
                    val json = JSONObject(body)
                    val conn = json.optJSONObject("connection")
                    val asnNum = conn?.optInt("asn", 0) ?: 0
                    val org = conn?.optString("org", "") ?: ""
                    val ip = json.optString("ip", "")
                    val country = json.optString("country", "")

                    val matchedOffline = BundledRanges.IRANIAN_ISPS.firstOrNull { it.asn == asnNum }
                    val finalOrg = matchedOffline?.let { "${it.nameEn} (${it.nameFa})" } ?: org

                    val info = IspInfo(
                        asOrganization = finalOrg,
                        asn = "AS$asnNum",
                        country = country,
                        clientIp = ip,
                        colo = "---",
                        isOfflineDetected = false,
                        isIpv6Available = ipv6Supported,
                        source = "ipwho.is"
                    )
                    _ispInfo.value = info
                    return@withContext info
                }
            }
        } catch (_: Exception) {}

        // 3. Offline fallback
        val info = IspInfo(
            asOrganization = "Offline / Restricted Network",
            asn = "AS---",
            country = "IR / Local",
            clientIp = "---",
            colo = "---",
            isOfflineDetected = true,
            isIpv6Available = ipv6Supported,
            source = "Offline Fallback"
        )
        _ispInfo.value = info
        info
    }
}
