package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.i18n.AppStrings
import com.example.ui.theme.CfOrange
import com.example.ui.theme.StatusGreen
import com.example.ui.viewmodel.ScannerViewModel

@Composable
fun SettingsScreen(
    viewModel: ScannerViewModel,
    strings: AppStrings
) {
    val currentLang by viewModel.currentLanguage.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            // App Identity & Status Banner
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(CfOrange.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_shield_wifi),
                            contentDescription = null,
                            tint = CfOrange,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = strings.appTitle,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (strings.isRtl) "اسکنر پیشرفته و امن شبکه • نسخه ۱.۰" else "High-Reliability Anycast Engine • v1.0",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(StatusGreen)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (strings.isRtl) "سیستم آماده • ۱۰۰٪ پردازش محلی" else "Engine Active • Fully Localized",
                                fontSize = 11.sp,
                                color = StatusGreen,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // Language Switch Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Language, contentDescription = null, tint = CfOrange)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = strings.language, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.setLanguage("en") }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (currentLang == "en") Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = if (currentLang == "en") CfOrange else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(text = strings.english, fontSize = 14.sp)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.setLanguage("fa") }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (currentLang == "fa") Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = if (currentLang == "fa") CfOrange else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(text = strings.persian, fontSize = 14.sp)
                    }
                }
            }
        }

        // Privacy & Local-Only Guarantee
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = CfOrange)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (strings.isRtl) "عدم جمع‌آوری داده و پردازش کاملاً محلی" else "Zero Telemetry & 100% Local Execution",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = strings.privacyNotice,
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Technical Information & Principles
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = CfOrange)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (strings.isRtl) "اصول فنی و نحوه عملکرد برنامه" else "Why Clean IP Scanner Works",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val principles = if (strings.isRtl) listOf(
                        "• عدم استفاده از پینگ ICMP: به جای پینگ کاذب، مسیرهای حقیقی TCP و TLS را تست می‌کند.",
                        "• زمان‌بندی بودجه سه‌گانه: توزیع هوشمند مهلت برقراری اتصال TCP، TLS و HTTP مانع از گزارش‌های کاذب می‌شود.",
                        "• تطبیق دقیق SNI و گواهی SSL: اختلالات فیلترینگ و سرورهای میانی را به دقت کشف می‌کند.",
                        "• تشخیص نزدیک‌ترین دیتاسنتر Anycast: بهترین نودهای کلودفلر نزدیک به اپراتور شما (DXB, FRA, IST, AMS) را اولویت‌بندی می‌کند.",
                        "• هشدار خودکار فعال بودن پروکسی: در صورت روشن بودن VPN یا WARP هشدار می‌دهد تا از فریب خوردن نتایج جلوگیری شود."
                    ) else listOf(
                        "• No ICMP Pings: Handshakes test actual TCP & TLS paths rather than misleading ICMP.",
                        "• Split-Timeout Budget: Dial (T/4), TLS (T/2), HTTP (T/4) prevents false packet loss reports.",
                        "• Explicit SNI & Cert Matching: Drops Cloudflare origin IPs and catches SNI middlebox interference.",
                        "• Anycast Datacenter Detection: Finds edge nodes closest to your ISP (DXB, FRA, IST, AMS).",
                        "• Proxy Detection: Alerts if active VPN/WARP invalidates scan numbers."
                    )

                    principles.forEach { principle ->
                        Text(
                            text = principle,
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
