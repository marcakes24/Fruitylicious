package com.example.fruitylicious.ui.admin.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.fruitylicious.ADMIN_REPORT_INVENTORY
import com.example.fruitylicious.ADMIN_REPORT_RESTOCK
import com.example.fruitylicious.ADMIN_REPORT_SALES
import com.example.fruitylicious.ADMIN_REPORT_TRANSACTIONS
import com.example.fruitylicious.ADMIN_REPORT_WASTE
import com.example.fruitylicious.ui.shared.FruityMenuCard
import com.example.fruitylicious.ui.shared.FruitySectionTitle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsDashboardScreen(
    onNavigate: (String) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reports") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFFFFDF6),
                    titleContentColor = Color(0xFF1B5E20)
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFFFFDF6))
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            FruitySectionTitle(
                title = "Admin Reports",
                subtitle = "Server-backed reports for branch and combined analytics"
            )

            FruityMenuCard(
                title = "Sales Report",
                subtitle = "View branch sales totals and product sales",
                icon = Icons.Default.Assessment,
                onClick = { onNavigate(ADMIN_REPORT_SALES) }
            )

            FruityMenuCard(
                title = "Inventory Report",
                subtitle = "View current branch stock and low-stock status",
                icon = Icons.Default.Inventory,
                onClick = { onNavigate(ADMIN_REPORT_INVENTORY) }
            )

            FruityMenuCard(
                title = "Waste Report",
                subtitle = "Review waste quantities and reasons",
                icon = Icons.Default.Warning,
                onClick = { onNavigate(ADMIN_REPORT_WASTE) }
            )

            FruityMenuCard(
                title = "Restock Report",
                subtitle = "Review restock quantities and suppliers",
                icon = Icons.Default.Restore,
                onClick = { onNavigate(ADMIN_REPORT_RESTOCK) }
            )

            FruityMenuCard(
                title = "Transaction Report",
                subtitle = "Review detailed transaction records",
                icon = Icons.Default.ReceiptLong,
                onClick = { onNavigate(ADMIN_REPORT_TRANSACTIONS) }
            )
        }
    }
}