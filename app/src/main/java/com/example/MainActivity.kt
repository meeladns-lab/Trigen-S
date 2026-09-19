package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.BidiText
import com.example.ui.i18n.AppStrings
import com.example.ui.screens.ExportScreen
import com.example.ui.screens.InsightsScreen
import com.example.ui.screens.ResultsScreen
import com.example.ui.screens.ScanScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.CfOrange
import com.example.ui.theme.CleanIpScannerTheme
import com.example.ui.theme.StatusGreen
import com.example.ui.viewmodel.ScannerViewModel

enum class NavTab(val icon: ImageVector) {
    SCAN(Icons.Default.Radar),
    RESULTS(Icons.Default.List),
    EXPORT(Icons.Default.Share),
    INSIGHTS(Icons.Default.Assessment),
    SETTINGS(Icons.Default.Settings)
}

class MainActivity : ComponentActivity() {
    private val viewModel: ScannerViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val currentLang by viewModel.currentLanguage.collectAsState()
            val progress by viewModel.progress.collectAsState()
            val rawResults by viewModel.rawResults.collectAsState()
            val strings = remember(currentLang) { AppStrings(currentLang) }
            val layoutDirection = if (strings.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr

            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                CleanIpScannerTheme(isFa = (currentLang == "fa" || strings.isRtl)) {
                    var selectedTab by remember { mutableStateOf(NavTab.SCAN) }

                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        contentWindowInsets = WindowInsets.safeDrawing,
                        topBar = {
                            TopAppBar(
                                title = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Image(
                                            painter = painterResource(id = R.drawable.trigen_logo_1789832646197),
                                            contentDescription = strings.appTitle,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(CircleShape)
                                                .border(1.dp, CfOrange.copy(alpha = 0.5f), CircleShape)
                                        )

                                        Spacer(modifier = Modifier.width(10.dp))

                                        Column {
                                            Text(
                                                text = strings.appTitle,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp
                                            )
                                            if (progress.isRunning) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(6.dp)
                                                            .clip(CircleShape)
                                                            .background(CfOrange)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = strings.scanningProgress,
                                                        fontSize = 10.sp,
                                                        color = CfOrange,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                }
                                            } else if (rawResults.isNotEmpty()) {
                                                val healthyCount = rawResults.count { it.isHealthy }
                                                Text(
                                                    text = "$healthyCount ${strings.healthyHits}",
                                                    fontSize = 10.sp,
                                                    color = StatusGreen,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                    }
                                },
                                actions = {
                                    // Quick Language Toggle
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.padding(end = 8.dp)
                                    ) {
                                        IconButton(
                                            onClick = {
                                                viewModel.setLanguage(if (currentLang == "en") "fa" else "en")
                                            },
                                            modifier = Modifier.size(38.dp)
                                        ) {
                                            Text(
                                                text = if (currentLang == "en") "فا" else "EN",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = CfOrange
                                            )
                                        }
                                    }
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    titleContentColor = MaterialTheme.colorScheme.onSurface
                                )
                            )
                        },
                        bottomBar = {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surface,
                                tonalElevation = 3.dp
                            ) {
                                NavTab.entries.forEach { tab ->
                                    val title = when (tab) {
                                        NavTab.SCAN -> strings.navScan
                                        NavTab.RESULTS -> strings.navResults
                                        NavTab.EXPORT -> strings.navExport
                                        NavTab.INSIGHTS -> strings.navInsights
                                        NavTab.SETTINGS -> strings.navSettings
                                    }

                                    NavigationBarItem(
                                        icon = { Icon(imageVector = tab.icon, contentDescription = title) },
                                        label = { Text(title, fontSize = 11.sp, maxLines = 1, fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal) },
                                        alwaysShowLabel = true,
                                        selected = selectedTab == tab,
                                        onClick = { selectedTab = tab },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = CfOrange,
                                            selectedTextColor = CfOrange,
                                            indicatorColor = CfOrange.copy(alpha = 0.18f)
                                        )
                                    )
                                }
                            }
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            when (selectedTab) {
                                NavTab.SCAN -> ScanScreen(
                                    viewModel = viewModel,
                                    strings = strings,
                                    onNavigateToResults = { selectedTab = NavTab.RESULTS }
                                )
                                NavTab.RESULTS -> ResultsScreen(
                                    viewModel = viewModel,
                                    strings = strings
                                )
                                NavTab.EXPORT -> ExportScreen(
                                    viewModel = viewModel,
                                    strings = strings
                                )
                                NavTab.INSIGHTS -> InsightsScreen(
                                    viewModel = viewModel,
                                    strings = strings
                                )
                                NavTab.SETTINGS -> SettingsScreen(
                                    viewModel = viewModel,
                                    strings = strings
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
