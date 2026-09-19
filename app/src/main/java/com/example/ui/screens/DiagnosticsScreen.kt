package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.core.export.ResultExporter
import com.example.core.model.DpiLikelihood
import com.example.core.model.FailureClass
import com.example.ui.components.BidiText
import com.example.ui.i18n.AppStrings
import com.example.ui.theme.CfOrange
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.StatusRed
import com.example.ui.viewmodel.ScannerViewModel

@Composable
fun DiagnosticsScreen(
    viewModel: ScannerViewModel,
    strings: AppStrings
) {
    val context = LocalContext.current
    val ispInfo by viewModel.ispInfo.collectAsState()
    val progress by viewModel.progress.collectAsState()

    val totalFailures = progress.failureStats.values.sum()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = strings.diagTitle,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }

        // ISP & Network Status Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_shield_wifi),
                            contentDescription = null,
                            tint = CfOrange,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (strings.isRtl) "تله‌متری و مشخصات شبکه" else "Network Telemetry",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    DiagDetailRow(label = strings.currentIsp, value = "${ispInfo.asOrganization} (${ispInfo.asn})")
                    DiagDetailRow(label = strings.egressIp, value = ispInfo.clientIp)
                    DiagDetailRow(label = strings.nearestColo, value = if (ispInfo.colo.isNotEmpty()) ispInfo.colo else "DXB / FRA")
                    DiagDetailRow(
                        label = "IPv6 Global Reachability",
                        value = if (ispInfo.isIpv6Available) "Connected" else "Not available on this network",
                        valueColor = if (ispInfo.isIpv6Available) StatusGreen else StatusAmber
                    )
                }
            }
        }

        // Failure Taxonomy Histogram (§1.4)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = CfOrange)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = strings.failureHistogram, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        BidiText(text = "$totalFailures total", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (progress.failureStats.isEmpty()) {
                        Text(
                            text = "No failure statistics recorded yet. Start a scan to analyze network middleboxes.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        progress.failureStats.entries
                            .sortedByDescending { it.value }
                            .forEach { (failure, count) ->
                                val ratio = if (totalFailures > 0) count.toFloat() / totalFailures else 0f
                                val barColor = when (failure.dpiLikelihood) {
                                    DpiLikelihood.HIGH -> StatusRed
                                    DpiLikelihood.MEDIUM -> StatusAmber
                                    else -> CfOrange
                                }

                                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = if (strings.isRtl) failure.displayNameFa else failure.displayNameEn,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        BidiText(
                                            text = "$count (${String.format(java.util.Locale.US, "%.0f%%", ratio * 100)})",
                                            fontSize = 11.sp,
                                            color = barColor
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    LinearProgressIndicator(
                                        progress = { ratio },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(CircleShape),
                                        color = barColor,
                                        trackColor = MaterialTheme.colorScheme.surface
                                    )
                                }
                            }
                    }
                }
            }
        }

        // DPI & Filtering Advice Box
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
                        Text(text = strings.dpiAdviceTitle, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val rstCount = progress.failureStats[FailureClass.TCP_RESET] ?: 0
                    val tlsFailCount = progress.failureStats[FailureClass.TLS_HANDSHAKE_FAILURE] ?: 0
                    val certMismatchCount = progress.failureStats[FailureClass.TLS_CERT_NAME_MISMATCH] ?: 0

                    val advice = when {
                        rstCount > 10 -> if (strings.isRtl) {
                            "تزریق پکت ریست (RST Injection) بالا شناسایی شد. این پدیده حاکی از دخالت سامانه فیلترینگ بر روی SNI یا پورت انتخابی است. پیشنهاد می‌شود پورت‌های جایگزین نظیر ۸۴۴۳ یا ۲۰۵۳ را تست کنید یا دامنه SNI اختصاصی قرار دهید."
                        } else {
                            "High TCP Reset injection detected. Middleboxes are actively terminating connections. Alternate ports (8443, 2053) or custom SNIs are recommended."
                        }
                        tlsFailCount > 10 -> if (strings.isRtl) {
                            "اختلال در دست‌تکانی TLS بالا است. دامنه پیش‌فرض فیلتر است؛ در بخش اسکن دامنه‌های SNI دیگر نظیر cp.cloudflare.com یا دامنه شخصی خود را قرار دهید."
                        } else {
                            "TLS handshakes failing. The primary SNI may be blocked; alternate SNI domains in scan settings."
                        }
                        certMismatchCount > 5 -> if (strings.isRtl) {
                            "برخی آی‌پی‌ها متعلق به سرور مبدا (Origin) هستند و لبه انی‌کست نیستند؛ برنامه این موارد را به طور خودکار فیلتر کرده و حذف می‌کند."
                        } else {
                            "Origin IPs encountered. Clean IP Scanner automatically filters these out using certificate verification."
                        }
                        else -> if (strings.isRtl) {
                            "وضعیت شبکه پایدار به نظر می‌رسد. از پریست امن (Safe) برای کشف آی‌پی‌های بدون افت پکت استفاده کنید."
                        } else {
                            "Network conditions appear stable. Use the Safe preset to identify loss-free IPs with minimal latency."
                        }
                    }

                    Text(text = advice, fontSize = 12.sp, lineHeight = 18.sp)
                }
            }
        }

        // Copy Redacted Diagnostics Button
        item {
            Button(
                onClick = {
                    val diagReport = buildString {
                        appendLine("=== Clean IP Scanner Diagnostic Report ===")
                        appendLine("ISP: ${ispInfo.asOrganization}")
                        appendLine("ASN: ${ispInfo.asn}")
                        appendLine("Nearest Colo: ${ispInfo.colo}")
                        appendLine("IPv6 Available: ${ispInfo.isIpv6Available}")
                        appendLine("Total Tested: ${progress.testedCount}")
                        appendLine("Healthy Hits: ${progress.healthyCount}")
                        appendLine("Failures Breakdown:")
                        progress.failureStats.forEach { (k, v) ->
                            appendLine(" - ${k.name}: $v")
                        }
                    }
                    ResultExporter.copyToClipboard(context, "Diagnostics", diagReport)
                    Toast.makeText(context, strings.copied, Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CfOrange)
            ) {
                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = strings.copyDiagnostics, fontSize = 13.sp)
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun DiagDetailRow(label: String, value: String, valueColor: Color = Color.Unspecified) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        BidiText(text = value, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = valueColor)
    }
}
