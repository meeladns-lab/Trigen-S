package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.core.model.IpSourceType
import com.example.core.model.ScanPreset
import com.example.ui.components.BidiText
import com.example.ui.i18n.AppStrings
import com.example.ui.theme.CfOrange
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.StatusRed
import com.example.ui.viewmodel.ScannerViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ScanScreen(
    viewModel: ScannerViewModel,
    strings: AppStrings,
    onNavigateToResults: () -> Unit
) {
    val config by viewModel.config.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val ispInfo by viewModel.ispInfo.collectAsState()
    val parsedShareLink by viewModel.parsedShareLink.collectAsState()

    var sourceMenuExpanded by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))

            // ISP & Network Banner
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(CfOrange.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_shield_wifi),
                            contentDescription = "ISP",
                            tint = CfOrange,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = strings.currentIsp,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        BidiText(
                            text = "${ispInfo.asOrganization} (${ispInfo.asn})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "IPv6: ",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (ispInfo.isIpv6Available) "Available" else "Unavailable",
                                fontSize = 11.sp,
                                color = if (ispInfo.isIpv6Available) StatusGreen else StatusAmber,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        // Live Scanning Card (if running or has progress)
        item {
            ScanningProgressCard(
                progress = progress,
                strings = strings,
                onNavigateToResults = onNavigateToResults
            )
        }

        // Action Button: Start or Stop Scan
        item {
            ScanActionButton(
                isRunning = progress.isRunning,
                onToggleScan = {
                    if (progress.isRunning) {
                        viewModel.stopScan()
                    } else {
                        viewModel.startScan()
                    }
                },
                strings = strings
            )
        }

        // Presets Section (§5)
        item {
            Text(
                text = strings.presets,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PRESET_LIST.forEach { preset ->
                    val isSelected = config.preset == preset
                    Card(
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.applyPreset(preset) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) CfOrange.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, CfOrange) else null
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (strings.isRtl) preset.titleFa else preset.titleEn,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = if (isSelected) CfOrange else MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            BidiText(
                                text = "${preset.workers}w / ${preset.timeoutSec}s",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Text(
                text = if (strings.isRtl) config.preset.descriptionFa else config.preset.descriptionEn,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp, start = 4.dp)
            )
        }

        // IP Source Selection
        item {
            Text(
                text = strings.ipSource,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Box(modifier = Modifier.fillMaxWidth()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { sourceMenuExpanded = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (strings.isRtl) config.sourceType.labelFa else config.sourceType.labelEn,
                            fontSize = 14.sp
                        )
                        Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Select")
                    }
                }

                DropdownMenu(
                    expanded = sourceMenuExpanded,
                    onDismissRequest = { sourceMenuExpanded = false }
                ) {
                    IpSourceType.values().forEach { source ->
                        DropdownMenuItem(
                            text = { Text(if (strings.isRtl) source.labelFa else source.labelEn) },
                            onClick = {
                                viewModel.updateConfig { it.copy(sourceType = source) }
                                sourceMenuExpanded = false
                            }
                        )
                    }
                }
            }

            if (config.sourceType == IpSourceType.MANUAL) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = config.manualInput,
                    onValueChange = { input -> viewModel.updateConfig { it.copy(manualInput = input) } },
                    label = { Text("IP, CIDR or Dash Range (e.g. 104.16.0.1-50)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
            } else if (config.sourceType == IpSourceType.CUSTOM_URL) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = config.customUrl,
                    onValueChange = { url -> viewModel.updateConfig { it.copy(customUrl = url) } },
                    label = { Text("Custom CIDR List URL") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        }

        // Target Ports Multi-Select Chips
        item {
            Text(
                text = strings.targetPorts,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                COMMON_PORTS.forEach { port ->
                    val isSelected = config.ports.contains(port)
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.togglePort(port) },
                        label = { BidiText(text = port.toString(), fontSize = 12.sp) },
                        leadingIcon = if (isSelected) {
                            { Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CfOrange.copy(alpha = 0.2f),
                            selectedLabelColor = CfOrange
                        )
                    )
                }
            }
        }

        // IPv4 / IPv6 Toggles
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = strings.testIpv4, fontSize = 14.sp)
                        Switch(
                            checked = config.includeIpv4,
                            onCheckedChange = { checked ->
                                if (checked || config.includeIpv6) {
                                    viewModel.updateConfig { it.copy(includeIpv4 = checked) }
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = strings.testIpv6, fontSize = 14.sp)
                            if (!ispInfo.isIpv6Available) {
                                Text(
                                    text = strings.ipv6DisabledNotice,
                                    fontSize = 11.sp,
                                    color = StatusAmber
                                )
                            }
                        }
                        Switch(
                            checked = config.includeIpv6,
                            onCheckedChange = { checked ->
                                if (checked || config.includeIpv4) {
                                    viewModel.updateConfig { it.copy(includeIpv6 = checked) }
                                }
                            }
                        )
                    }
                }
            }
        }

        // Germany & Netherlands Filter & Randomized Scanning Strategy
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(
                                text = strings.onlyGermanyAndNetherlandsTitle,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = strings.onlyGermanyAndNetherlandsDesc,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = config.onlyGermanyAndNetherlands,
                            onCheckedChange = { viewModel.toggleOnlyGermanyAndNetherlands() }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(StatusGreen)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = strings.randomizedScanBadge,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = StatusGreen
                        )
                    }
                }
            }
        }

        // Share Link Input (Optional)
        item {
            Text(
                text = strings.proxyShareLink,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = config.shareLinkToTest,
                onValueChange = { viewModel.setShareLink(it) },
                placeholder = { Text(strings.shareLinkPlaceholder, fontSize = 12.sp) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                maxLines = 2
            )

            if (parsedShareLink != null) {
                Text(
                    text = "Parsed: ${parsedShareLink?.protocol?.uppercase()} | SNI: ${parsedShareLink?.sni} | Port: ${parsedShareLink?.port}",
                    fontSize = 11.sp,
                    color = StatusGreen,
                    modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                )
            }
        }

        // Custom Workers / Timeout / Sample Size Sliders
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Workers Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = strings.workers, fontSize = 12.sp)
                        BidiText(text = "${config.workers}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Slider(
                        value = config.workers.toFloat(),
                        onValueChange = { v -> viewModel.updateConfig { it.copy(workers = v.toInt(), preset = ScanPreset.CUSTOM) } },
                        valueRange = 10f..250f,
                        steps = 23
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Timeout Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = strings.timeoutSec, fontSize = 12.sp)
                        BidiText(text = "${config.timeoutSec}s", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Slider(
                        value = config.timeoutSec.toFloat(),
                        onValueChange = { v -> viewModel.updateConfig { it.copy(timeoutSec = v.toInt(), preset = ScanPreset.CUSTOM) } },
                        valueRange = 1f..10f,
                        steps = 8
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Sample Size Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = strings.sampleSize, fontSize = 12.sp)
                        BidiText(text = "${config.sampleSize}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Slider(
                        value = config.sampleSize.toFloat(),
                        onValueChange = { v -> viewModel.updateConfig { it.copy(sampleSize = v.toInt(), preset = ScanPreset.CUSTOM) } },
                        valueRange = 100f..5000f,
                        steps = 48
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

private val COMMON_PORTS = listOf(443, 8443, 2053, 2083, 2087, 2096, 80, 8080)
private val PRESET_LIST = listOf(ScanPreset.SAFE, ScanPreset.BALANCED, ScanPreset.FAST)

@Composable
private fun ScanningProgressCard(
    progress: com.example.core.model.ScanProgress,
    strings: AppStrings,
    onNavigateToResults: () -> Unit
) {
    AnimatedVisibility(visible = progress.isRunning || progress.testedCount > 0) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (progress.isProxyDetected) StatusRed.copy(alpha = 0.1f) else CfOrange.copy(alpha = 0.1f)
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (progress.isRunning) strings.scanningProgress else strings.availableIps,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 15.sp
                    )
                    if (progress.testedCount > 0) {
                        Button(
                            onClick = onNavigateToResults,
                            colors = ButtonDefaults.buttonColors(containerColor = CfOrange)
                        ) {
                            Text(strings.navResults, fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Progress Bar
                val progressFraction = if (progress.totalCandidates > 0) {
                    (progress.testedCount.toFloat() / progress.totalCandidates.toFloat()).coerceIn(0f, 1f)
                } else 0f
                val percentInt = (progressFraction * 100).toInt()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LinearProgressIndicator(
                        progress = { progressFraction },
                        modifier = Modifier
                            .weight(1f)
                            .height(8.dp)
                            .clip(CircleShape),
                        color = CfOrange,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "$percentInt%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = CfOrange
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Stats Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = strings.testedTotal,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        BidiText(
                            text = "${progress.testedCount} / ${progress.totalCandidates}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = strings.healthyHits,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        BidiText(
                            text = "${progress.healthyCount}",
                            fontWeight = FontWeight.Bold,
                            color = StatusGreen,
                            fontSize = 14.sp
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = strings.eta,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        BidiText(
                            text = "${progress.etaSeconds}s",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }

                // Proxy Detection Warning (§1.5)
                if (progress.isProxyDetected) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        color = StatusRed.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Warning",
                                tint = StatusRed,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = strings.proxyWarningTitle,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = StatusRed
                                )
                                Text(
                                    text = strings.proxyWarningDesc,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScanActionButton(
    isRunning: Boolean,
    onToggleScan: () -> Unit,
    strings: AppStrings
) {
    Button(
        onClick = onToggleScan,
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isRunning) StatusRed else CfOrange
        )
    ) {
        Icon(
            imageVector = if (isRunning) Icons.Default.Close else Icons.Default.PlayArrow,
            contentDescription = null,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = if (isRunning) strings.stopScan else strings.startScan,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
