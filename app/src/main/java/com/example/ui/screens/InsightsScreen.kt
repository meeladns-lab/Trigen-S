package com.example.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.i18n.AppStrings
import com.example.ui.theme.CfOrange
import com.example.ui.viewmodel.ScannerViewModel

@Composable
fun InsightsScreen(
    viewModel: ScannerViewModel,
    strings: AppStrings
) {
    var selectedSubTab by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = selectedSubTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = CfOrange,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedSubTab]),
                    color = CfOrange
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(
                selected = selectedSubTab == 0,
                onClick = { selectedSubTab = 0 },
                text = {
                    Text(
                        text = strings.tabDiagnostics,
                        fontWeight = if (selectedSubTab == 0) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 13.sp,
                        color = if (selectedSubTab == 0) CfOrange else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            )
            Tab(
                selected = selectedSubTab == 1,
                onClick = { selectedSubTab = 1 },
                text = {
                    Text(
                        text = strings.tabHistory,
                        fontWeight = if (selectedSubTab == 1) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 13.sp,
                        color = if (selectedSubTab == 1) CfOrange else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            )
        }

        if (selectedSubTab == 0) {
            DiagnosticsScreen(viewModel = viewModel, strings = strings)
        } else {
            HistoryScreen(viewModel = viewModel, strings = strings)
        }
    }
}
