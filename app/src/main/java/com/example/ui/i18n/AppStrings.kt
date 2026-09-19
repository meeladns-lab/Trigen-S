package com.example.ui.i18n

object Bidi {
    /**
     * Surrounds technical string (IP, port, URL, latency) with First Strong Isolate (\u2068)
     * and Pop Directional Isolate (\u2069) characters so it never scrambles in RTL text.
     */
    fun isolate(text: String): String {
        return "\u2068$text\u2069"
    }
}

class AppStrings(val lang: String) {
    val isRtl: Boolean get() = lang == "fa"

    // Navigation
    val navScan: String get() = if (isRtl) "اسکن" else "Scan"
    val navResults: String get() = if (isRtl) "نتایج" else "Results"
    val navExport: String get() = if (isRtl) "خروجی" else "Export"
    val navInsights: String get() = if (isRtl) "تحلیل و تاریخچه" else "Insights"
    val navDiagnostics: String get() = if (isRtl) "عیب‌یابی" else "Diagnostics"
    val navHistory: String get() = if (isRtl) "تاریخچه" else "History"
    val navSettings: String get() = if (isRtl) "تنظیمات" else "Settings"
    val tabDiagnostics: String get() = if (isRtl) "تحلیل اختلالات خط" else "Network Diagnostics"
    val tabHistory: String get() = if (isRtl) "سوابق اسکن و نشان‌شده‌ها" else "History & Favorites"

    // App header
    val appTitle: String get() = if (isRtl) "اسکنر تریژن (Trigen)" else "Trigen Scanner"
    val appSubtitle: String get() = if (isRtl) "موتور هوشمند پویش آی‌پی تمیز کلودفلر" else "High-Performance Cloudflare Clean IP Scanner"

    // Scan Screen
    val ipSource: String get() = if (isRtl) "منبع آی‌پی‌ها" else "IP Source"
    val presets: String get() = if (isRtl) "تنظیمات آماده (پریست)" else "Scan Presets"
    val workers: String get() = if (isRtl) "تعداد تِردهای همزمان (Workers)" else "Concurrent Workers"
    val timeoutSec: String get() = if (isRtl) "مهلت زمانی هر تلاش (ثانیه)" else "Timeout per Attempt (sec)"
    val sampleSize: String get() = if (isRtl) "تعداد نمونه آی‌پی" else "Sample Size"
    val targetPorts: String get() = if (isRtl) "پورت‌های هدف" else "Target Ports"
    val testIpv4: String get() = if (isRtl) "آی‌پی نسخه ۴ (IPv4)" else "IPv4 Addresses"
    val testIpv6: String get() = if (isRtl) "آی‌پی نسخه ۶ (IPv6)" else "IPv6 Addresses"
    val ipv6DisabledNotice: String get() = if (isRtl) "شبکه فعلی از IPv6 پشتیبانی نمی‌کند یا دسترسی عمومی ندارد." else "Current network lacks working global IPv6."
    val sniHost: String get() = if (isRtl) "دامنه SNI" else "SNI Hostname"
    val proxyShareLink: String get() = if (isRtl) "کانفیگ فیلترشکن (اختیاری جهت شخصی‌سازی)" else "Proxy Share Link (Optional for SNI/Path)"
    val shareLinkPlaceholder: String get() = "vless://, trojan://, vmess://, ss://..."
    val neighborScan: String get() = if (isRtl) "اسکن همسایه‌ها (Neighbor Scan)" else "Neighbor Scan (Subnet Sweep)"
    val neighborScanDesc: String get() = if (isRtl) "پویش خودکار آی‌پی‌های اطراف نتایج تمیز" else "Automatically probes nearby IPs around healthy hits"
    val onlyGermanyAndNetherlandsTitle: String get() = if (isRtl) "فقط دیتاسنترهای آلمان و هلند (FRA / AMS)" else "Only Germany & Netherlands Datacenters"
    val onlyGermanyAndNetherlandsDesc: String get() = if (isRtl) "محدودسازی دقیق نتایج به فرانکفورت، آمستردام، برلین، مونیخ و... جهت پینگ پایدار و رفع اختلال در ایران" else "Restricts results strictly to Frankfurt (FRA), Amsterdam (AMS), Munich, Berlin, etc. for lowest latency and zero DPI throttling"
    val randomizedScanBadge: String get() = if (isRtl) "اسکن تصادفی در تمام رنج‌های کلودفلر فعال است" else "Stratified Uniform Random Scanning Active"
    val startScan: String get() = if (isRtl) "شروع اسکن" else "Start Scan"
    val stopScan: String get() = if (isRtl) "توقف اسکن" else "Stop Scan"

    // Live progress
    val scanningProgress: String get() = if (isRtl) "در حال اسکن..." else "Scanning in progress..."
    val testedTotal: String get() = if (isRtl) "تست شده" else "Tested"
    val healthyHits: String get() = if (isRtl) "آی‌پی سالم" else "Healthy IPs"
    val elapsed: String get() = if (isRtl) "زمان سپری شده" else "Elapsed"
    val eta: String get() = if (isRtl) "زمان تخمینی باقی‌مانده" else "ETA"
    val proxyWarningTitle: String get() = if (isRtl) "هشدار: فیلترشکن یا پروکسی فعال شناسایی شد!" else "Warning: Active Proxy or VPN Detected!"
    val proxyWarningDesc: String get() = if (isRtl) "پینگ ثبت‌شده غیرواقعی و زیر ۵ میلی‌ثانیه است یا ترافیک از مسیر WARP عبور می‌کند. لطفاً پروکسی را قطع کرده و مجدداً اسکن کنید تا آمار حقیقی خط را دریافت نمایید." else "Latency is implausibly low (<5ms) or WARP egress was detected. Disconnect your proxy to get accurate measurements."

    // Results Screen
    val availableIps: String get() = if (isRtl) "آی‌پی‌های پاسخ‌داده" else "Available Clean IPs"
    val filterOptions: String get() = if (isRtl) "فیلترها و مرتب‌سازی" else "Filters & Sorting"
    val maxLatency: String get() = if (isRtl) "سقف مجاز پینگ" else "Max Latency (Ping)"
    val maxLoss: String get() = if (isRtl) "سقف مجاز افت پکت (%)" else "Max Packet Loss (%)"
    val coloFilter: String get() = if (isRtl) "فیلتر دیتاسنتر (Colo)" else "Datacenter Location (Colo)"
    val sortOption: String get() = if (isRtl) "ترتیب نمایش نتایج" else "Sort Results By"
    val sortByScore: String get() = if (isRtl) "بهترین کیفیت (پیشنهادی)" else "Best Overall"
    val sortByScoreDesc: String get() = if (isRtl) "ترکیب بهینه پینگ کم، افت پکت صفر و نوسان پایین" else "Combines lowest latency, 0% drop, and stability"
    val sortByLatency: String get() = if (isRtl) "کمترین پینگ" else "Lowest Ping"
    val sortByLatencyDesc: String get() = if (isRtl) "سریع‌ترین زمان پاسخ‌دهی" else "Fastest initial packet response"
    val sortByLoss: String get() = if (isRtl) "بدون افت پکت" else "Zero Packet Loss"
    val sortByLossDesc: String get() = if (isRtl) "اتصال بدون قطعی (مناسب کانفیگ‌های حساس)" else "Strictly 0% dropped packets to avoid disconnects"
    val sortBySpeed: String get() = if (isRtl) "بیشترین سرعت دانلود" else "Fastest Download"
    val sortBySpeedDesc: String get() = if (isRtl) "بالاترین پهنای باند تست سرعت دانلود و آپلود" else "Ranked by download & upload speed benchmark"
    val runSpeedTestOnTop10: String get() = if (isRtl) "تست سرعت دانلود و آپلود ۱۰ آی‌پی برتر" else "Test Download & Upload (Top 10)"
    val testingSpeed: String get() = if (isRtl) "در حال تست دانلود و آپلود..." else "Testing download & upload speed..."
    val emptyResults: String get() = if (isRtl) "هنوز اسکن انجام نشده یا آی‌پی سالمی یافت نشد. می‌توانید پورت ۴۴۳ را انتخاب کرده و مجدداً اسکن کنید." else "No healthy IPs found yet. Try scanning with port 443 on the Safe preset."
    val resetFilters: String get() = if (isRtl) "بازنشانی فیلترها" else "Reset Filters"
    val metricsGuideTitle: String get() = if (isRtl) "راهنمای معیارهای کیفیت آی‌پی" else "IP Quality Metrics Guide"
    val guideLatency: String get() = if (isRtl) "پینگ (Latency): زمان رفت و برگشت داده (میلی‌ثانیه). زیر ۱۰۰ms عالی، زیر ۲۵۰ms مناسب وبگردی است." else "Ping (Latency): Round-trip response time. Under 100ms is ideal, under 250ms is good."
    val guideJitter: String get() = if (isRtl) "نوسان (Jitter): تغییرات ناگهانی پینگ. هرچه کمتر باشد (<15ms) تماس صوتی و بازی آنلاین روان‌تر است." else "Jitter: Ping instability. Lower (<15ms) ensures smooth streaming without lag spikes."
    val guideLoss: String get() = if (isRtl) "افت پکت (Packet Loss): درصد بسته‌های گم‌شده در مسیر. ۰٪ برای قطع نشدن فیلترشکن ضروری است." else "Packet Loss: Dropped network packets. 0% loss is crucial to prevent VPN disconnections."
    val guideColo: String get() = if (isRtl) "دیتاسنتر (Colo): شهر سرور کلودفلر (مثل FRA = فرانکفورت، AMS = آمستردام، MUC = مونیخ)." else "Colo (Datacenter): Cloudflare edge city handling traffic (e.g., FRA=Frankfurt, AMS=Amsterdam)."
    val guideThroughput: String get() = if (isRtl) "سرعت دانلود و آپلود (Speed): پهنای باند واقعی دانلود و آپلود از طریق کانکشن TLS روی شبکه ایران (مگابایت بر ثانیه)." else "Speed (MB/s): Real-world download and upload throughput measured over direct TLS stream on Iran networks."
    val helpTooltip: String get() = if (isRtl) "راهنمای گزینه‌ها و اصطلاحات" else "Help & Terminology Guide"

    // Result card details
    val latency: String get() = if (isRtl) "پینگ:" else "Ping:"
    val jitter: String get() = if (isRtl) "نوسان (Jitter):" else "Jitter:"
    val packetLoss: String get() = if (isRtl) "افت پکت:" else "Packet Loss:"
    val throughput: String get() = if (isRtl) "سرعت دانلود:" else "Download Speed:"
    val uploadSpeed: String get() = if (isRtl) "سرعت آپلود:" else "Upload Speed:"
    val attempts: String get() = if (isRtl) "تلاش‌ها:" else "Attempts:"
    val copyIp: String get() = if (isRtl) "کپی آی‌پی" else "Copy IP"
    val copied: String get() = if (isRtl) "کپی شد!" else "Copied!"

    // Export Screen
    val exportTitle: String get() = if (isRtl) "خروجی و اشتراک‌گذاری" else "Export & Sharing"
    val copyAllHealthy: String get() = if (isRtl) "کپی تمام آی‌پی‌های سالم" else "Copy All Healthy IPs"
    val copyTop20: String get() = if (isRtl) "کپی ۲۰ آی‌پی برتر" else "Copy Top 20 IPs"
    val exportCsv: String get() = if (isRtl) "خروجی جدول CSV" else "Export CSV File"
    val exportJson: String get() = if (isRtl) "خروجی ساختاریافته JSON" else "Export JSON"
    val exportSingBox: String get() = if (isRtl) "خروجی تنظیمات Sing-box" else "Sing-box Outbound JSON"
    val exportClash: String get() = if (isRtl) "خروجی پروکسی Clash" else "Clash Proxy YAML"
    val exportSub: String get() = if (isRtl) "لینک سابسکریپشن Base64" else "Base64 Subscription Link"
    val exportHosts: String get() = if (isRtl) "فرمت فایل Hosts" else "Hosts File Format"

    // VLESS Multi-Address Exporter
    val vlessExportHeader: String get() = if (isRtl) "استخراج ۲۰ آدرس در یک کانفیگ VLESS" else "VLESS Multi-Address Exporter (Top 20 Clean IPs)"
    val vlessExportSubtitle: String get() = if (isRtl) "۱ کانفیگ VLESS وارد کنید و ۲۰ کانفیگ با سریع‌ترین آی‌پی‌های تمیز خروجی بگیرید." else "Add 1 VLESS config to generate and export 20 configs with top clean IPs."
    val vlessInputLabel: String get() = if (isRtl) "لینک کانفیگ VLESS (ورودی)" else "VLESS Configuration Link"
    val vlessInputPlaceholder: String get() = "vless://uuid@host:port?type=ws&security=tls#Name"
    val copyTop20Vless: String get() = if (isRtl) "کپی هر ۲۰ کانفیگ VLESS" else "Copy Top 20 VLESS Configs"
    val shareTop20Vless: String get() = if (isRtl) "اشتراک‌گذاری ۲۰ کانفیگ" else "Share Top 20 Configs"
    val copyTop20VlessSub: String get() = if (isRtl) "کپی سابسکریپشن Base64" else "Copy Base64 Subscription"
    val previewGeneratedConfigs: String get() = if (isRtl) "مشاهده پیش‌نمایش کانفیگ‌های تولید شده" else "Preview Generated Configurations"


    // Diagnostics
    val diagTitle: String get() = if (isRtl) "وضعیت شبکه و تحلیل اختلالات" else "Network Diagnostics & DPI Analysis"
    val currentIsp: String get() = if (isRtl) "اپراتور شناسایی‌شده" else "Detected ISP / ASN"
    val egressIp: String get() = if (isRtl) "آی‌پی عمومی کاربر" else "Public Client IP"
    val nearestColo: String get() = if (isRtl) "نزدیک‌ترین دیتاسنتر" else "Nearest Colo"
    val failureHistogram: String get() = if (isRtl) "توزیع انواع خطاهای اتصال" else "Failure Classes Breakdown"
    val dpiAdviceTitle: String get() = if (isRtl) "تحلیل فنی وضعیت فیلترینگ" else "Filtering Analysis & Next Steps"
    val copyDiagnostics: String get() = if (isRtl) "کپی گزارش عیب‌یابی (بدون اطلاعات حساس)" else "Copy Redacted Diagnostics"

    // Settings
    val settingsTitle: String get() = if (isRtl) "تنظیمات و درباره" else "Settings & About"
    val language: String get() = if (isRtl) "زبان برنامه" else "Application Language"
    val english: String get() = "English"
    val persian: String get() = "فارسی"
    val privacyNotice: String get() = if (isRtl) "حفظ حریم خصوصی: این اپلیکیشن هیچ‌گونه داده، آمار یا تحلیلی به هیچ سرور واسطی ارسال نمی‌کند. تمام محاسبات و پینگ‌ها به صورت ۱۰۰٪ محلی روی دستگاه شما انجام می‌شود." else "Privacy: Clean IP Scanner collects zero telemetry, zero analytics, and runs 100% locally on your device."
}
