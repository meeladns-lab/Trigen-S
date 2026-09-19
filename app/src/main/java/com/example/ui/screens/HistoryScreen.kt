package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.export.ResultExporter
import com.example.ui.components.BidiText
import com.example.ui.i18n.AppStrings
import com.example.ui.theme.CfOrange
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.StatusRed
import com.example.ui.viewmodel.ScannerViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    viewModel: ScannerViewModel,
    strings: AppStrings
) {
    val context = LocalContext.current
    val history by viewModel.history.collectAsState()
    val favorites by viewModel.favorites.collectAsState()

    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = strings.navHistory,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }

        // Favorites Section
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Favorite, contentDescription = null, tint = StatusRed)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Favorite Clean IPs", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (favorites.isEmpty()) {
                        Text(
                            text = if (strings.isRtl) "هنوز آی‌پی نشان‌شده‌ای ثبت نشده است. با زدن آیکون قلب در لیست نتایج می‌توانید آی‌پی‌های باکیفیت را ذخیره کنید." else "No saved favorites yet. Tap the heart icon on any healthy result to pin it.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        favorites.forEach { favIp ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                BidiText(text = favIp, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = {
                                            ResultExporter.copyToClipboard(context, "Favorite IP", favIp)
                                            Toast.makeText(context, strings.copied, Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(18.dp))
                                    }

                                    IconButton(
                                        onClick = { viewModel.toggleFavorite(favIp) },
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Favorite, contentDescription = "Remove", tint = StatusRed, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Scan History Section
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.History, contentDescription = null, tint = CfOrange)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (strings.isRtl) "سوابق اسکن‌های انجام‌شده" else "Historical Scans by Network",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }

        if (history.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (strings.isRtl) "هنوز سابقه‌ای ذخیره نشده است. با اتمام هر اسکن، نتایج و کارایی شبکه ثبت می‌گردد." else "No completed scan records. Run a scan to save historical network performance.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(history) { entry ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${entry.ispName} (${entry.asn})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            BidiText(
                                text = dateFormat.format(Date(entry.timestamp)),
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Port: ${entry.port} | ${entry.healthyCount} Healthy",
                                fontSize = 11.sp,
                                color = StatusGreen
                            )
                            BidiText(
                                text = "Best: ${String.format(Locale.US, "%.0f ms", entry.bestLatency)}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (entry.topIps.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(6.dp)) {
                                    entry.topIps.take(3).forEach { ip ->
                                        BidiText(text = ip, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
