package com.example.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.export.ResultExporter
import com.example.core.linkparse.ShareLinkParser
import com.example.ui.components.BidiText
import com.example.ui.i18n.AppStrings
import com.example.ui.theme.CfOrange
import com.example.ui.theme.StatusGreen
import com.example.ui.viewmodel.ScannerViewModel

@Composable
fun ExportScreen(
    viewModel: ScannerViewModel,
    strings: AppStrings
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val results by viewModel.rawResults.collectAsState()
    val parsedShareLink by viewModel.parsedShareLink.collectAsState()
    val vlessInput by viewModel.vlessExportInput.collectAsState()

    val healthyResults = results.filter { it.isHealthy }
    var isPreviewExpanded by remember { mutableStateOf(false) }

    val top20Configs = remember(vlessInput, healthyResults) {
        if (vlessInput.isNotBlank() && healthyResults.isNotEmpty()) {
            ResultExporter.toTop20VlessConfigs(vlessInput, healthyResults, 20)
        } else {
            emptyList()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = strings.exportTitle,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Text(
                text = "${healthyResults.size} ${strings.healthyHits}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Quick Copy Buttons
        if (healthyResults.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (strings.isRtl) "هنوز هیچ آی‌پی سالمی برای خروجی موجود نیست. ابتدا از تب اسکن، یک اسکن اجرا نمایید." else "No healthy IPs available for export yet. Run a scan from the Scan tab to discover clean IPs.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        val txt = ResultExporter.toPlainTxt(healthyResults)
                        ResultExporter.copyToClipboard(context, "All Clean IPs", txt)
                        Toast.makeText(context, "${strings.copied} (${healthyResults.size} IPs)", Toast.LENGTH_SHORT).show()
                    },
                    enabled = healthyResults.isNotEmpty(),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CfOrange)
                ) {
                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = strings.copyAllHealthy, fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = {
                        val txt = ResultExporter.toPlainTxt(healthyResults.take(20))
                        ResultExporter.copyToClipboard(context, "Top 20 Clean IPs", txt)
                        Toast.makeText(context, "${strings.copied} (20 IPs)", Toast.LENGTH_SHORT).show()
                    },
                    enabled = healthyResults.isNotEmpty(),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = strings.copyTop20, fontSize = 12.sp)
                }
            }
        }

        // ==========================================
        // VLESS Top 20 Multi-Address Exporter Card
        // ==========================================
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, CfOrange.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = CfOrange.copy(alpha = 0.15f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.VpnKey,
                                    contentDescription = null,
                                    tint = CfOrange,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = strings.vlessExportHeader,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = strings.vlessExportSubtitle,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Input field for VLESS link
                    OutlinedTextField(
                        value = vlessInput,
                        onValueChange = { viewModel.setVlessExportInput(it) },
                        label = { Text(strings.vlessInputLabel, fontSize = 12.sp) },
                        placeholder = { Text(strings.vlessInputPlaceholder, fontSize = 11.sp) },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        trailingIcon = {
                            Row {
                                if (vlessInput.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.setVlessExportInput("") }) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Clear",
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                IconButton(onClick = {
                                    val clip = clipboardManager.getText()?.text
                                    if (!clip.isNullOrBlank()) {
                                        viewModel.setVlessExportInput(clip.trim())
                                        Toast.makeText(context, "Pasted from clipboard", Toast.LENGTH_SHORT).show()
                                    }
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.ContentPaste,
                                        contentDescription = "Paste",
                                        tint = CfOrange,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Status Indicator
                    if (top20Configs.isNotEmpty()) {
                        Surface(
                            color = StatusGreen.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = StatusGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (strings.isRtl)
                                        "آماده: ${top20Configs.size} کانفیگ VLESS با ۲۰ آی‌پی تمیز برتر تولید شد."
                                    else
                                        "Ready: ${top20Configs.size} VLESS configs generated with top clean IPs.",
                                    fontSize = 11.sp,
                                    color = StatusGreen,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    } else if (vlessInput.isNotBlank() && healthyResults.isEmpty()) {
                        Text(
                            text = if (strings.isRtl)
                                "کانفیگ دریافت شد. لطفاً ابتدا یک اسکن اجرا کنید تا آی‌پی‌های تمیز شناسایی شده و در این کانفیگ تزریق شوند."
                            else
                                "Config loaded. Please run a scan from the Scan tab first to discover clean IPs.",
                            fontSize = 11.sp,
                            color = CfOrange
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // Primary Action: Copy All 20 Configs
                    Button(
                        onClick = {
                            val allConfigs = top20Configs.joinToString("\n")
                            ResultExporter.copyToClipboard(context, "Top 20 VLESS Configs", allConfigs)
                            Toast.makeText(
                                context,
                                "${strings.copied} (${top20Configs.size} configs)",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        enabled = top20Configs.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CfOrange)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = strings.copyTop20Vless,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Secondary Actions: Share All 20 and Base64 Subscription
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val allConfigs = top20Configs.joinToString("\n")
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, allConfigs)
                                    type = "text/plain"
                                }
                                val shareIntent = Intent.createChooser(sendIntent, strings.shareTop20Vless)
                                context.startActivity(shareIntent)
                            },
                            enabled = top20Configs.isNotEmpty(),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = strings.shareTop20Vless, fontSize = 11.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                val sub = ResultExporter.toTop20VlessSubscriptionBase64(vlessInput, healthyResults, 20)
                                ResultExporter.copyToClipboard(context, "VLESS Subscription", sub)
                                Toast.makeText(context, strings.copied, Toast.LENGTH_SHORT).show()
                            },
                            enabled = top20Configs.isNotEmpty(),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(text = strings.copyTop20VlessSub, fontSize = 11.sp)
                        }
                    }

                    // Expandable Preview Accordion
                    if (top20Configs.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isPreviewExpanded = !isPreviewExpanded }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = strings.previewGeneratedConfigs,
                                fontSize = 11.sp,
                                color = CfOrange,
                                fontWeight = FontWeight.SemiBold
                            )
                            Icon(
                                imageVector = if (isPreviewExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = CfOrange,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        AnimatedVisibility(visible = isPreviewExpanded) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                top20Configs.forEachIndexed { index, configLink ->
                                    val ip = healthyResults.getOrNull(index)?.ip ?: ""
                                    val latency = healthyResults.getOrNull(index)?.avgLatencyMs?.toInt() ?: 0
                                    Surface(
                                        color = MaterialTheme.colorScheme.surface,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "#${index + 1} • $ip  (${latency}ms)",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                BidiText(
                                                    text = configLink.take(65) + "...",
                                                    fontSize = 9.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            IconButton(
                                                onClick = {
                                                    ResultExporter.copyToClipboard(context, "VLESS Config #${index + 1}", configLink)
                                                    Toast.makeText(context, "${strings.copied} (#${index + 1})", Toast.LENGTH_SHORT).show()
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.ContentCopy,
                                                    contentDescription = "Copy",
                                                    tint = CfOrange,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Share-Link Rewritten Outputs (if user provided proxy config via Scan tab)
        if (parsedShareLink != null && healthyResults.isNotEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.VpnKey, contentDescription = null, tint = CfOrange)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (strings.isRtl) "کانفیگ‌های بازنویسی‌شده با آی‌پی تمیز" else "Rewritten Proxy Configurations",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Best Clean IP Link
                        val bestIp = healthyResults.first().ip
                        val rewrittenBest = ShareLinkParser.rewriteWithCleanIp(parsedShareLink!!, bestIp)

                        Text(text = "Best Clean IP (${bestIp}):", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            BidiText(
                                text = parsedShareLink!!.getRedactedString(),
                                fontSize = 11.sp,
                                modifier = Modifier.padding(8.dp)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    ResultExporter.copyToClipboard(context, "Clean IP Link", rewrittenBest)
                                    Toast.makeText(context, strings.copied, Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = CfOrange)
                            ) {
                                Text("Copy Link", fontSize = 11.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    val sub = ResultExporter.toSubscriptionBase64(healthyResults.take(15), parsedShareLink!!)
                                    ResultExporter.copyToClipboard(context, "Subscription", sub)
                                    Toast.makeText(context, strings.copied, Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(strings.exportSub, fontSize = 11.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val singBox = ShareLinkParser.generateSingBoxOutbound(parsedShareLink!!, bestIp)
                                    ResultExporter.copyToClipboard(context, "Sing-box JSON", singBox)
                                    Toast.makeText(context, strings.copied, Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Sing-box Outbound", fontSize = 11.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    val clash = ShareLinkParser.generateClashProxy(parsedShareLink!!, bestIp)
                                    ResultExporter.copyToClipboard(context, "Clash YAML", clash)
                                    Toast.makeText(context, strings.copied, Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Clash Proxy", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        // Export Format Cards
        item {
            ExportOptionCard(
                title = strings.exportCsv,
                description = "Full table with IP, Port, Colo, Min/Avg Latency, Jitter, Packet Loss, and Throughput.",
                icon = Icons.Default.Description,
                onCopy = {
                    val csv = ResultExporter.toCsv(healthyResults)
                    ResultExporter.copyToClipboard(context, "Results CSV", csv)
                    Toast.makeText(context, strings.copied, Toast.LENGTH_SHORT).show()
                }
            )
        }

        item {
            ExportOptionCard(
                title = strings.exportJson,
                description = "Structured JSON Lines format for scripts and automated proxy client updaters.",
                icon = Icons.Default.Code,
                onCopy = {
                    val json = ResultExporter.toJson(healthyResults)
                    ResultExporter.copyToClipboard(context, "Results JSON", json)
                    Toast.makeText(context, strings.copied, Toast.LENGTH_SHORT).show()
                }
            )
        }

        item {
            ExportOptionCard(
                title = strings.exportHosts,
                description = "Host-mapping lines formatted for /etc/hosts or DNS rewrite tools.",
                icon = Icons.Default.Share,
                onCopy = {
                    val hosts = ResultExporter.toHostsFormat(healthyResults)
                    ResultExporter.copyToClipboard(context, "Hosts Format", hosts)
                    Toast.makeText(context, strings.copied, Toast.LENGTH_SHORT).show()
                }
            )
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun ExportOptionCard(
    title: String,
    description: String,
    icon: ImageVector,
    onCopy: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = CfOrange,
                modifier = Modifier.size(28.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(
                    text = description,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(onClick = onCopy) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy",
                    tint = CfOrange
                )
            }
        }
    }
}

