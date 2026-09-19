package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.core.export.ResultExporter
import com.example.core.model.ProbeResult
import com.example.ui.components.BidiText
import com.example.ui.i18n.AppStrings
import com.example.ui.theme.CfOrange
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.StatusRed
import com.example.ui.viewmodel.ScannerViewModel
import com.example.ui.viewmodel.SortOption

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ResultsScreen(
    viewModel: ScannerViewModel,
    strings: AppStrings
) {
    val context = LocalContext.current
    val results by viewModel.rawResults.collectAsState()
    val isSpeedTesting by viewModel.isSpeedTesting.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val progress by viewModel.progress.collectAsState()

    var showFilters by remember { mutableStateOf(false) }
    var showGuideDialog by remember { mutableStateOf(false) }
    var maxLatency by remember { mutableStateOf(600f) }
    var maxLoss by remember { mutableStateOf(50f) }
    var selectedColo by remember { mutableStateOf<String?>(null) }
    var currentSort by remember { mutableStateOf(SortOption.SCORE) }

    val isFiltered = maxLatency < 600f || maxLoss < 50f || selectedColo != null

    // Distinct colos for filter chips
    val availableColos = remember(results) {
        results.mapNotNull { it.colo.ifEmpty { null } }.distinct().sorted()
    }

    // Filter and sort results
    val displayedResults = remember(results, maxLatency, maxLoss, selectedColo, currentSort) {
        results
            .filter { it.avgLatencyMs <= maxLatency }
            .filter { it.packetLossPercent <= maxLoss }
            .filter { selectedColo == null || it.colo.equals(selectedColo, ignoreCase = true) }
            .let { list ->
                when (currentSort) {
                    SortOption.SCORE -> list.sortedBy { it.score }
                    SortOption.LATENCY -> list.sortedBy { it.avgLatencyMs }
                    SortOption.LOSS -> list.sortedWith(compareBy({ it.packetLossPercent }, { it.avgLatencyMs }))
                    SortOption.SPEED -> list.sortedByDescending { it.throughputMbps ?: -1.0 }
                }
            }
    }

    if (showGuideDialog) {
        MetricsGuideDialog(strings = strings, onDismiss = { showGuideDialog = false })
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Top Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f, fill = false)
            ) {
                Text(
                    text = strings.availableIps,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    color = CfOrange.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    BidiText(
                        text = "${displayedResults.size} / ${results.size}",
                        color = CfOrange,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Info / Guide Dialog Button
                IconButton(
                    onClick = { showGuideDialog = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.HelpOutline,
                        contentDescription = strings.helpTooltip,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Filter Toggle Button
                IconButton(
                    onClick = { showFilters = !showFilters },
                    modifier = Modifier.size(36.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isFiltered || showFilters) CfOrange.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                        border = if (isFiltered || showFilters) BorderStroke(1.dp, CfOrange) else null,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = strings.filterOptions,
                                tint = if (isFiltered || showFilters) CfOrange else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Speed Test Action Row
        if (results.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { viewModel.runSpeedTestTopN(10) },
                    enabled = !isSpeedTesting,
                    colors = ButtonDefaults.buttonColors(containerColor = CfOrange),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                ) {
                    if (isSpeedTesting) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = strings.testingSpeed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = strings.runSpeedTestOnTop10,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Expandable Filters & Sorting Panel
        AnimatedVisibility(
            visible = showFilters,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Header Row with Reset
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = strings.filterOptions,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )

                        if (isFiltered) {
                            TextButton(
                                onClick = {
                                    maxLatency = 600f
                                    maxLoss = 50f
                                    selectedColo = null
                                }
                            ) {
                                Icon(imageVector = Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = strings.resetFilters, fontSize = 11.sp, color = CfOrange)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Sort Chips with Horizontal Scroll and Explanatory Subtitle
                    Text(
                        text = strings.sortOption,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SortChipItem(
                            label = strings.sortByScore,
                            icon = Icons.Default.Star,
                            isSelected = currentSort == SortOption.SCORE,
                            onClick = { currentSort = SortOption.SCORE }
                        )
                        SortChipItem(
                            label = strings.sortByLatency,
                            icon = Icons.Default.TrendingDown,
                            isSelected = currentSort == SortOption.LATENCY,
                            onClick = { currentSort = SortOption.LATENCY }
                        )
                        SortChipItem(
                            label = strings.sortByLoss,
                            icon = Icons.Default.Verified,
                            isSelected = currentSort == SortOption.LOSS,
                            onClick = { currentSort = SortOption.LOSS }
                        )
                        SortChipItem(
                            label = strings.sortBySpeed,
                            icon = Icons.Default.Speed,
                            isSelected = currentSort == SortOption.SPEED,
                            onClick = { currentSort = SortOption.SPEED }
                        )
                    }

                    // Description of active sort
                    Text(
                        text = when (currentSort) {
                            SortOption.SCORE -> strings.sortByScoreDesc
                            SortOption.LATENCY -> strings.sortByLatencyDesc
                            SortOption.LOSS -> strings.sortByLossDesc
                            SortOption.SPEED -> strings.sortBySpeedDesc
                        },
                        fontSize = 11.sp,
                        color = CfOrange,
                        modifier = Modifier.padding(top = 6.dp, start = 2.dp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Max Latency Slider & Quick Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = strings.maxLatency, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        BidiText(
                            text = if (maxLatency >= 600f) "All (< 600ms)" else "< ${maxLatency.toInt()} ms",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (maxLatency < 200f) StatusGreen else CfOrange
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(100f, 200f, 350f, 600f).forEach { presetVal ->
                            val isSelected = maxLatency == presetVal
                            Surface(
                                onClick = { maxLatency = presetVal },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) CfOrange.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, if (isSelected) CfOrange else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = if (presetVal == 600f) "All" else "<${presetVal.toInt()}ms",
                                    fontSize = 10.sp,
                                    textAlign = TextAlign.Center,
                                    color = if (isSelected) CfOrange else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Slider(
                        value = maxLatency,
                        onValueChange = { maxLatency = it },
                        valueRange = 50f..600f,
                        colors = SliderDefaults.colors(
                            thumbColor = CfOrange,
                            activeTrackColor = CfOrange
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Max Loss Slider & Quick Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = strings.maxLoss, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        BidiText(
                            text = if (maxLoss == 0f) "0% (Zero Loss Only)" else "< ${maxLoss.toInt()}%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (maxLoss == 0f) StatusGreen else StatusAmber
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(0f, 15f, 30f, 50f).forEach { presetLoss ->
                            val isSelected = maxLoss == presetLoss
                            Surface(
                                onClick = { maxLoss = presetLoss },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) StatusGreen.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, if (isSelected) StatusGreen else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = if (presetLoss == 0f) "0% Only" else "<${presetLoss.toInt()}%",
                                    fontSize = 10.sp,
                                    textAlign = TextAlign.Center,
                                    color = if (isSelected) StatusGreen else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Slider(
                        value = maxLoss,
                        onValueChange = { maxLoss = it },
                        valueRange = 0f..50f,
                        colors = SliderDefaults.colors(
                            thumbColor = StatusGreen,
                            activeTrackColor = StatusGreen
                        )
                    )

                    // Colo Filter Chips with City Names
                    if (availableColos.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = strings.coloFilter,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(top = 6.dp)
                        ) {
                            FilterChip(
                                selected = selectedColo == null,
                                onClick = { selectedColo = null },
                                label = { Text("All Cities", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CfOrange.copy(alpha = 0.2f),
                                    selectedLabelColor = CfOrange
                                )
                            )
                            availableColos.forEach { colo ->
                                val cityName = getColoCityName(colo)
                                val labelText = if (cityName != colo) "$colo ($cityName)" else colo
                                FilterChip(
                                    selected = selectedColo == colo,
                                    onClick = { selectedColo = if (selectedColo == colo) null else colo },
                                    label = { Text(labelText, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = CfOrange.copy(alpha = 0.2f),
                                        selectedLabelColor = CfOrange
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        // Results List
        if (displayedResults.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(CfOrange.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_shield_wifi),
                                contentDescription = null,
                                tint = CfOrange,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = if (strings.isRtl) "هیچ نتیجه‌ای با این فیلترها یافت نشد" else "No Matching Clean IPs",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isFiltered) {
                                if (strings.isRtl) "فیلترهای اعمال‌شده بیش از حد محدود هستند. روی بازنشانی فیلترها بزنید." else "Current filters are too strict. Tap 'Reset Filters' to see all IPs."
                            } else {
                                strings.emptyResults
                            },
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        if (isFiltered) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = {
                                    maxLatency = 600f
                                    maxLoss = 50f
                                    selectedColo = null
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CfOrange)
                            ) {
                                Text(strings.resetFilters, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        } else {
            val distinctResults = remember(displayedResults) {
                displayedResults.distinctBy { it.hostPortString }
            }
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(distinctResults, key = { it.hostPortString }) { result ->
                    ResultItemCard(
                        result = result,
                        isFavorite = favorites.contains(result.ip),
                        onToggleFavorite = { viewModel.toggleFavorite(result.ip) },
                        onCopyIp = {
                            ResultExporter.copyToClipboard(context, "Clean IP", result.ip)
                            Toast.makeText(context, "${strings.copied} (${result.ip})", Toast.LENGTH_SHORT).show()
                        },
                        strings = strings
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
fun SortChipItem(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = if (isSelected) CfOrange.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, if (isSelected) CfOrange else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) CfOrange else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                color = if (isSelected) CfOrange else MaterialTheme.colorScheme.onSurface,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

@Composable
fun ResultItemCard(
    result: ProbeResult,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onCopyIp: () -> Unit,
    strings: AppStrings
) {
    var expanded by remember { mutableStateOf(false) }

    val statusColor = when {
        result.packetLossPercent == 0.0 && result.avgLatencyMs < 100 -> StatusGreen
        result.packetLossPercent == 0.0 && result.avgLatencyMs < 250 -> StatusGreen
        result.packetLossPercent < 25.0 -> StatusAmber
        else -> StatusRed
    }

    val qualityBadgeText = when {
        result.packetLossPercent == 0.0 && result.avgLatencyMs < 90 -> if (strings.isRtl) "⚡ فوق‌العاده" else "⚡ Ultra"
        result.packetLossPercent == 0.0 && result.avgLatencyMs < 200 -> if (strings.isRtl) "🟢 عالی" else "🟢 Great"
        result.packetLossPercent < 20.0 -> if (strings.isRtl) "🟡 متوسط" else "🟡 Good"
        else -> if (strings.isRtl) "🟠 ناپایدار" else "🟠 Unstable"
    }

    val cityName = remember(result.colo) {
        getColoCityName(result.colo)
    }

    Card(
        onClick = { expanded = !expanded },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = if (result.packetLossPercent == 0.0 && result.avgLatencyMs < 120) {
            BorderStroke(1.dp, StatusGreen.copy(alpha = 0.4f))
        } else null,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: IP Address, Colo Badge, Quality Badge, Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // IP Address & Indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    BidiText(
                        text = result.hostPortString,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Quality Badge
                    Surface(
                        color = statusColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = qualityBadgeText,
                            color = statusColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Colo Chip
                    if (result.colo.isNotEmpty()) {
                        Surface(
                            color = CfOrange.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = result.colo,
                                color = CfOrange,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    }

                    IconButton(onClick = onToggleFavorite, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (isFavorite) StatusRed else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(onClick = onCopyIp, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy IP",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Details",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // City name tag if available
            if (result.colo.isNotEmpty() && cityName != result.colo) {
                Text(
                    text = "Datacenter: $cityName",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 18.dp, bottom = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Measurement Stats Row: Ping, Jitter, Loss, Speed
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Ping
                    Column {
                        Text(text = strings.latency, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        BidiText(
                            text = String.format(java.util.Locale.US, "%.0f ms", result.avgLatencyMs),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = statusColor
                        )
                    }

                    // Jitter
                    Column {
                        Text(text = strings.jitter, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        BidiText(
                            text = String.format(java.util.Locale.US, "%.1f ms", result.jitterMs),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }

                    // Packet Loss
                    Column {
                        Text(text = strings.packetLoss, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        BidiText(
                            text = String.format(java.util.Locale.US, "%.0f%%", result.packetLossPercent),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (result.packetLossPercent == 0.0) StatusGreen else StatusAmber
                        )
                    }

                    // Speed
                    Column(horizontalAlignment = Alignment.End) {
                        Text(text = strings.throughput, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        BidiText(
                            text = result.speedDisplay,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (result.throughputMbps != null) StatusGreen else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Expanded Attempt Detail Logs
            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = if (strings.isRtl) "گزارش تفصیلی تلاش‌های شبکه:" else "Attempt-by-attempt diagnostic logs:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = CfOrange
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            result.attempts.forEach { att ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "#${att.attemptNumber}: ${if (att.isSuccess) "OK" else att.failureClass.name}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (att.isSuccess) StatusGreen else StatusRed
                                    )
                                    BidiText(
                                        text = if (att.isSuccess) "TCP: ${att.tcpTimeMs}ms | TLS: ${att.tlsTimeMs}ms | HTTP: ${att.httpTimeMs}ms" else att.errorMessage,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            if (result.ttfbMs != null) {
                                Spacer(modifier = Modifier.height(4.dp))
                                BidiText(
                                    text = "TTFB: ${result.ttfbMs} ms | Speed: ${result.speedDisplay}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = StatusGreen
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
fun MetricsGuideDialog(
    strings: AppStrings,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = CfOrange)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = strings.metricsGuideTitle, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                GuideSection(title = strings.latency, desc = strings.guideLatency, color = StatusGreen)
                GuideSection(title = strings.jitter, desc = strings.guideJitter, color = CfOrange)
                GuideSection(title = strings.packetLoss, desc = strings.guideLoss, color = StatusRed)
                GuideSection(title = strings.coloFilter, desc = strings.guideColo, color = MaterialTheme.colorScheme.primary)
                GuideSection(title = strings.throughput, desc = strings.guideThroughput, color = StatusGreen)
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = CfOrange)
            ) {
                Text(if (strings.isRtl) "متوجه شدم" else "Got It")
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
private fun GuideSection(
    title: String,
    desc: String,
    color: Color
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = title, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = color)
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = desc, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

fun getColoCityName(code: String): String {
    return when (code.trim().uppercase()) {
        "FRA" -> "Frankfurt"
        "AMS" -> "Amsterdam"
        "IST" -> "Istanbul"
        "DXB" -> "Dubai"
        "VIE" -> "Vienna"
        "CDG" -> "Paris"
        "LHR" -> "London"
        "HEL" -> "Helsinki"
        "SOF" -> "Sofia"
        "MUC" -> "Munich"
        "WAW" -> "Warsaw"
        "ARN" -> "Stockholm"
        "MAD" -> "Madrid"
        "MXP" -> "Milan"
        "ZRH" -> "Zurich"
        "GYD" -> "Baku"
        "DOH" -> "Doha"
        "BAH" -> "Bahrain"
        "KWI" -> "Kuwait"
        "MCT" -> "Muscat"
        "TBS" -> "Tbilisi"
        "EVN" -> "Yerevan"
        "KBP" -> "Kyiv"
        else -> code
    }
}
