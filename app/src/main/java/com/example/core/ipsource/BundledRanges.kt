package com.example.core.ipsource

object BundledRanges {
    const val EMBEDDED_DATE = "2026-03"

    // Cloudflare published IPv4 ranges
    val CLOUDFLARE_IPV4 = listOf(
        "103.21.244.0/22",
        "103.22.200.0/22",
        "103.31.4.0/22",
        "104.16.0.0/13",
        "104.24.0.0/14",
        "108.162.192.0/18",
        "131.0.72.0/22",
        "141.101.64.0/18",
        "162.158.0.0/15",
        "172.64.0.0/13",
        "173.245.48.0/20",
        "188.114.96.0/20",
        "190.93.240.0/20",
        "197.234.240.0/22",
        "198.41.128.0/17"
    )

    // Cloudflare official Germany (FRA, BER, DUS, MUC, HAM, STR) and Netherlands (AMS) priority ranges
    val CLOUDFLARE_DE_NL_IPV4 = listOf(
        "188.114.96.0/22",
        "188.114.100.0/22",
        "188.114.104.0/22",
        "188.114.108.0/22",
        "141.101.64.0/20",
        "141.101.80.0/20",
        "141.101.120.0/21",
        "104.24.0.0/16",
        "104.26.0.0/18",
        "104.27.0.0/18",
        "108.162.192.0/20",
        "108.162.208.0/20",
        "162.158.0.0/17",
        "162.158.80.0/20",
        "162.158.96.0/20",
        "172.64.0.0/16",
        "172.67.0.0/16",
        "198.41.128.0/18",
        "198.41.192.0/18",
        "131.0.72.0/22",
        "173.245.48.0/21",
        "173.245.56.0/21"
    )

    // Cloudflare published IPv6 ranges
    val CLOUDFLARE_IPV6 = listOf(
        "2400:cb00::/32",
        "2606:4700::/32",
        "2803:f800::/32",
        "2405:b500::/32",
        "2405:8100::/32",
        "2a06:98c0::/29",
        "2c0f:f248::/32"
    )

    // Cloudflare official Germany & Netherlands IPv6 ranges
    val CLOUDFLARE_DE_NL_IPV6 = listOf(
        "2a06:98c0::/29",
        "2606:4700::/32",
        "2400:cb00::/32"
    )

    // Fastly edge ranges
    val FASTLY_IPV4 = listOf(
        "151.101.0.0/16",
        "199.232.0.0/16",
        "146.75.0.0/16"
    )

    // Gcore edge ranges
    val GCORE_IPV4 = listOf(
        "92.223.80.0/22",
        "92.38.168.0/22",
        "185.190.140.0/22"
    )

    // Major Iranian ISP ASNs verified from RIPE Stat / bgp.he.net
    data class IranianIspEntry(val asn: Int, val nameEn: String, val nameFa: String)

    val IRANIAN_ISPS = listOf(
        IranianIspEntry(58224, "TCI (Telecommunication Company of Iran)", "مخابرات ایران (TCI)"),
        IranianIspEntry(197207, "MCI (Mobile Telecommunication Co / Hamrah Aval)", "همراه اول (MCI)"),
        IranianIspEntry(44244, "MTN Irancell", "ایرانسل (Irancell)"),
        IranianIspEntry(57218, "Rightel", "رایتل (Rightel)"),
        IranianIspEntry(31549, "Shatel", "شاتل (Shatel)"),
        IranianIspEntry(16322, "ParsOnline", "پارس آنلاین (ParsOnline)"),
        IranianIspEntry(43754, "Asiatech", "آسیاتک (Asiatech)"),
        IranianIspEntry(50810, "MobinNet", "مبین‌نت (Mobinnet)"),
        IranianIspEntry(48159, "Pishgaman Kavir", "پیشگامان کویر (Pishgaman)"),
        IranianIspEntry(24631, "Fanava", "فن‌آوا (Fanava)"),
        IranianIspEntry(42337, "HiWEB / Dadeh Gostar", "های‌وب (HiWEB)"),
        IranianIspEntry(49100, "Respina Networks", "رسپینا (Respina)"),
        IranianIspEntry(25184, "Afranet", "افرانت (Afranet)")
    )
}
