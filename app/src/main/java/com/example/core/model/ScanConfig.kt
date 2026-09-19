package com.example.core.model

enum class ScanPreset(
    val titleEn: String,
    val titleFa: String,
    val workers: Int,
    val timeoutSec: Int,
    val sampleSize: Int,
    val descriptionEn: String,
    val descriptionFa: String
) {
    SAFE(
        "Safe (Default)",
        "امن (پیش‌فرض)",
        50,
        5,
        1000,
        "Recommended for restricted networks. Avoids ISP rate limiting and temporary blocks.",
        "پیشنهاد شده برای شبکه‌های دارای فیلترینگ و کاهش خطر مسدودسازی موقت توسط اپراتور."
    ),
    BALANCED(
        "Balanced",
        "متعادل",
        100,
        3,
        2000,
        "Faster throughput for stable broadband connections with moderate filtering.",
        "سرعت بالاتر برای اینترنت‌های پایدارتر."
    ),
    FAST(
        "Fast (Aggressive)",
        "سریع (ریسک بالا)",
        200,
        2,
        5000,
        "Warning: High concurrency may trigger temporary Cloudflare or ISP rate-limiting.",
        "هشدار: ارسال سریع درخواست‌ها ممکن است باعث مسدودیت موقت چند ساعته شود."
    ),
    CUSTOM(
        "Custom",
        "سفارشی",
        50,
        5,
        1000,
        "Custom parameters defined by user.",
        "تنظیم پارامترها به صورت دستی."
    )
}

enum class IpSourceType(val labelEn: String, val labelFa: String) {
    CLOUDFLARE_DE_NL("Cloudflare Official (Germany & Netherlands Only)", "کلودفلر رسمی (فقط آلمان و هلند)"),
    CLOUDFLARE_OFFICIAL("Cloudflare Official (All Global Ranges)", "کلودفلر رسمی (همه رنج‌های جهانی)"),
    BUNDLED_FALLBACK("Bundled Verified Ranges", "محدوده‌های پیش‌فرض آفلاین"),
    CUSTOM_URL("Custom URL (CIDR list)", "آدرس URL دلخواه"),
    MANUAL("Manual Input (IP / CIDR / Range)", "ورود دستی آی‌پی / رنج"),
    FASTLY("Fastly CDN Ranges", "محدوده‌های شبکه Fastly"),
    GCORE("Gcore CDN Ranges", "محدوده‌های شبکه Gcore")
}

data class ScanConfig(
    val sourceType: IpSourceType = IpSourceType.CLOUDFLARE_DE_NL,
    val customUrl: String = "https://raw.githubusercontent.com/ircfspace/cf-ip-ranges/main/export.ipv4",
    val manualInput: String = "",
    val includeIpv4: Boolean = true,
    val includeIpv6: Boolean = false,
    val onlyGermanyAndNetherlands: Boolean = true,
    val preset: ScanPreset = ScanPreset.SAFE,
    val workers: Int = 50,
    val timeoutSec: Int = 5,
    val sampleSize: Int = 1000,
    val ports: List<Int> = listOf(443),
    val sniHosts: List<String> = listOf(
        "cdnjs.cloudflare.com",
        "time.cloudflare.com",
        "cloudflare-dns.com",
        "developers.cloudflare.com",
        "speed.cloudflare.com",
        "www.cloudflare.com",
        "cloudflare.com",
        "cp.cloudflare.com"
    ),
    val enableNeighborScan: Boolean = false,
    val neighborRadius: Int = 32,
    val runSpeedTestOnTopN: Int = 10,
    val speedTestPayloadKb: Int = 512,
    val shareLinkToTest: String = ""
)
