package com.example.core.model

/**
 * Detailed failure taxonomy distinguishing DPI blocking from network throttle or configuration errors.
 */
enum class FailureClass(val displayNameEn: String, val displayNameFa: String, val dpiLikelihood: DpiLikelihood) {
    NONE("Success", "موفق", DpiLikelihood.NONE),
    TCP_TIMEOUT("TCP Timeout", "تایم‌اوت TCP", DpiLikelihood.LOW),
    TCP_REFUSED("TCP Refused", "اتصال TCP رد شد", DpiLikelihood.LOW),
    TCP_RESET("TCP Reset (RST Injection)", "تزریق پکت ریست (DPI)", DpiLikelihood.HIGH),
    TLS_TIMEOUT("TLS Handshake Timeout", "تایم‌اوت دست‌تکانی TLS", DpiLikelihood.MEDIUM),
    TLS_HANDSHAKE_FAILURE("TLS Handshake Failure", "خطای دست‌تکانی TLS", DpiLikelihood.HIGH),
    TLS_CERT_NAME_MISMATCH("TLS Cert Name Mismatch (Origin IP)", "عدم تطابق گواهی (آی‌پی سرور مبدا)", DpiLikelihood.NONE),
    TLS_CERT_EXPIRED("TLS Cert Expired", "گواهی TLS منقضی شده", DpiLikelihood.NONE),
    TLS_CERT_UNKNOWN_AUTHORITY("TLS Unknown Authority", "مرجع گواهی ناشناخته", DpiLikelihood.NONE),
    HTTP_TIMEOUT("HTTP Trace Timeout", "تایم‌اوت درخواست HTTP", DpiLikelihood.MEDIUM),
    HTTP_STATUS_ERROR("HTTP Status Error", "کد وضعیت HTTP نامعتبر", DpiLikelihood.MEDIUM),
    NOT_CLOUDFLARE("Not Cloudflare Response", "پاسخ کلودفلر نیست", DpiLikelihood.HIGH),
    NON_DE_NL_DATACENTER("Non-DE/NL Datacenter", "خارج از دیتاسنترهای آلمان و هلند", DpiLikelihood.NONE),
    CANCELLED("Scan Cancelled", "لغو شد", DpiLikelihood.NONE);

    fun adviceEn(): String = when (this) {
        NON_DE_NL_DATACENTER -> "Excluded because traffic routes to a datacenter outside Germany (FRA, MUC, BER, etc.) or Netherlands (AMS)."
        TCP_RESET -> "DPI middlebox active. The ISP is actively resetting connections to this IP/port."
        TLS_HANDSHAKE_FAILURE -> "DPI is blocking TLS ClientHello or SNI. Try alternating the SNI domain."
        TLS_CERT_NAME_MISMATCH -> "This IP is a Cloudflare origin host, not an edge anycast node. It cannot serve proxy traffic."
        TLS_TIMEOUT -> "TLS packets are being severely throttled or dropped after TCP handshake."
        TCP_TIMEOUT -> "IP is unreachable, route is dead or severely blackholed."
        NOT_CLOUDFLARE -> "Middlebox or captive portal intercepted the connection."
        else -> "Transient network error or timeout."
    }

    fun adviceFa(): String = when (this) {
        NON_DE_NL_DATACENTER -> "این آی‌پی حذف شد زیرا ترافیک به دیتاسنتری خارج از آلمان (فرانکفورت و...) یا هلند (آمستردام) هدایت می‌شود."
        TCP_RESET -> "سامانه فیلترینگ فعال است. اپراتور پکت‌های این آی‌پی یا پورت را ریست می‌کند."
        TLS_HANDSHAKE_FAILURE -> "فیلترینگ روی SNI یا هندشیک TLS مداخله می‌کند. دامنه SNI جایگزین را تست کنید."
        TLS_CERT_NAME_MISMATCH -> "این آی‌پی متعلق به سرور مبدا است، نه لبه انی‌کست کلودفلر؛ برای فیلترشکن کاربرد ندارد."
        TLS_TIMEOUT -> "پکت‌های TLS توسط شبکه به شدت کند یا دور انداخته می‌شوند."
        TCP_TIMEOUT -> "آی‌پی در دسترس نیست یا در این شبکه مسدود شده است."
        NOT_CLOUDFLARE -> "پاسخ از فیلترینگ یا صفحه مسدودی دریافت شد."
        else -> "خطای موقت شبکه یا تایم‌اوت."
    }
}

enum class DpiLikelihood {
    NONE, LOW, MEDIUM, HIGH
}
