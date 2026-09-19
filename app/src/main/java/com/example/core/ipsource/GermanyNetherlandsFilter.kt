package com.example.core.ipsource

import com.example.core.probe.NetUtils

object GermanyNetherlandsFilter {

    /**
     * Primary Cloudflare Datacenter Airport Codes (Colos) in Germany and Netherlands:
     * - Germany (DE):
     *   - FRA: Frankfurt am Main (Main European Hub)
     *   - BER: Berlin
     *   - DUS: Düsseldorf
     *   - HAM: Hamburg
     *   - MUC: Munich
     *   - STR: Stuttgart
     * - Netherlands (NL):
     *   - AMS: Amsterdam (AMS-IX, High Throughput Hub)
     */
    val GERMANY_NETHERLANDS_COLOS = setOf(
        "FRA", // Frankfurt, Germany
        "AMS", // Amsterdam, Netherlands
        "BER", // Berlin, Germany
        "DUS", // Dusseldorf, Germany
        "HAM", // Hamburg, Germany
        "MUC", // Munich, Germany
        "STR"  // Stuttgart, Germany
    )

    /**
     * Checks if the detected Cloudflare Point of Presence (Colo) is in Germany or Netherlands.
     */
    fun isAllowedColo(colo: String?): Boolean {
        if (colo.isNullOrBlank()) return false
        val clean = colo.trim().uppercase()
        return GERMANY_NETHERLANDS_COLOS.contains(clean) ||
                clean.startsWith("FRA") ||
                clean.startsWith("AMS") ||
                clean.startsWith("MUC") ||
                clean.startsWith("BER") ||
                clean.startsWith("DUS") ||
                clean.startsWith("HAM")
    }

    /**
     * Returns country flag and city name for allowed European colos.
     */
    fun getFormattedLocation(colo: String): String {
        return when (colo.trim().uppercase()) {
            "FRA" -> "🇩🇪 Frankfurt"
            "AMS" -> "🇳🇱 Amsterdam"
            "MUC" -> "🇩🇪 Munich"
            "BER" -> "🇩🇪 Berlin"
            "DUS" -> "🇩🇪 Düsseldorf"
            "HAM" -> "🇩🇪 Hamburg"
            "STR" -> "🇩🇪 Stuttgart"
            else -> colo
        }
    }
}
