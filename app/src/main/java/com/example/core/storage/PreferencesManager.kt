package com.example.core.storage

import android.content.Context
import android.content.SharedPreferences
import com.example.core.model.ScanPreset
import org.json.JSONArray
import org.json.JSONObject

data class SavedHistoryEntry(
    val timestamp: Long,
    val asn: String,
    val ispName: String,
    val port: Int,
    val healthyCount: Int,
    val topIps: List<String>,
    val bestLatency: Double
)

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("clean_ip_scanner_prefs", Context.MODE_PRIVATE)

    var language: String
        get() = prefs.getString("app_language", "en") ?: "en"
        set(value) = prefs.edit().putString("app_language", value).apply()

    var lastPreset: ScanPreset
        get() {
            val name = prefs.getString("last_preset", ScanPreset.SAFE.name)
            return try { ScanPreset.valueOf(name ?: ScanPreset.SAFE.name) } catch (_: Exception) { ScanPreset.SAFE }
        }
        set(value) = prefs.edit().putString("last_preset", value.name).apply()

    fun getDiscoveredIpv6Prefixes(): Set<String> {
        return prefs.getStringSet("discovered_ipv6_prefixes", emptySet()) ?: emptySet()
    }

    fun saveDiscoveredIpv6Prefix(prefix: String) {
        val current = getDiscoveredIpv6Prefixes().toMutableSet()
        current.add(prefix)
        prefs.edit().putStringSet("discovered_ipv6_prefixes", current).apply()
    }

    fun getFavorites(): Set<String> {
        return prefs.getStringSet("favorite_ips", emptySet()) ?: emptySet()
    }

    fun toggleFavorite(ip: String): Boolean {
        val current = getFavorites().toMutableSet()
        val isNowFav = if (current.contains(ip)) {
            current.remove(ip)
            false
        } else {
            current.add(ip)
            true
        }
        prefs.edit().putStringSet("favorite_ips", current).apply()
        return isNowFav
    }

    fun saveScanHistory(entry: SavedHistoryEntry) {
        val historyJson = prefs.getString("scan_history_json", "[]") ?: "[]"
        val array = JSONArray(historyJson)
        val obj = JSONObject()
        obj.put("timestamp", entry.timestamp)
        obj.put("asn", entry.asn)
        obj.put("ispName", entry.ispName)
        obj.put("port", entry.port)
        obj.put("healthyCount", entry.healthyCount)
        val ipArray = JSONArray()
        entry.topIps.forEach { ipArray.put(it) }
        obj.put("topIps", ipArray)
        obj.put("bestLatency", entry.bestLatency)

        // Insert at beginning, keep up to 30 runs
        val newArray = JSONArray()
        newArray.put(obj)
        for (i in 0 until array.length().coerceAtMost(29)) {
            newArray.put(array.getJSONObject(i))
        }
        prefs.edit().putString("scan_history_json", newArray.toString()).apply()
    }

    fun getScanHistory(): List<SavedHistoryEntry> {
        val historyJson = prefs.getString("scan_history_json", "[]") ?: "[]"
        val array = JSONArray(historyJson)
        val list = mutableListOf<SavedHistoryEntry>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val ipArray = obj.optJSONArray("topIps") ?: JSONArray()
            val ips = mutableListOf<String>()
            for (j in 0 until ipArray.length()) {
                ips.add(ipArray.getString(j))
            }
            list.add(
                SavedHistoryEntry(
                    timestamp = obj.optLong("timestamp", 0),
                    asn = obj.optString("asn", "AS---"),
                    ispName = obj.optString("ispName", "Unknown"),
                    port = obj.optInt("port", 443),
                    healthyCount = obj.optInt("healthyCount", 0),
                    topIps = ips,
                    bestLatency = obj.optDouble("bestLatency", 0.0)
                )
            )
        }
        return list
    }
}
