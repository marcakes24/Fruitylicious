package com.example.fruitylicious.ui.staff.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.fruitylicious.STAFF_ADJUSTMENT
import com.example.fruitylicious.STAFF_INVENTORY
import com.example.fruitylicious.STAFF_LOG
import com.example.fruitylicious.STAFF_POS
import com.example.fruitylicious.STAFF_RESTOCK_ENTRY
import com.example.fruitylicious.STAFF_RESTOCK_HISTORY
import com.example.fruitylicious.STAFF_SALES_SUMMARY
import com.example.fruitylicious.STAFF_TRANSACTION_HISTORY
import com.example.fruitylicious.STAFF_WASTE_ENTRY
import com.example.fruitylicious.STAFF_WASTE_HISTORY
import com.example.fruitylicious.ui.shared.BranchIndicator
import com.example.fruitylicious.ui.shared.FruityMenuCard
import com.example.fruitylicious.ui.shared.FruityOutlinedButton
import com.example.fruitylicious.ui.shared.FruityPrimaryButton
import com.example.fruitylicious.ui.shared.FruitySectionTitle
import com.example.fruitylicious.ui.shared.LowStockAlertBanner
import com.example.fruitylicious.ui.shared.SyncStatusBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffDashboardScreen(
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit,
    viewModel: StaffDashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    DisposableEffect(Unit) {
        viewModel.refreshClockStatus()
        onDispose { }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Staff Dashboard")
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFFFFDF6),
                    titleContentColor = Color(0xFF1B5E20)
                ),
                actions = {
                    FloatingActionButton(
                        onClick = {
                            viewModel.logout()
                            onLogout()
                        },
                        containerColor = Color(0xFFE8F5E9),
                        contentColor = Color(0xFF1B5E20)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Logout,
                            contentDescription = "Logout"
                        )
                    }
                }
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
            SyncStatusBar(
                isOnline = uiState.isOnline,
                lastSyncAt = uiState.lastSyncAt,
                lastSyncSuccessful = uiState.lastSyncSuccessful,
                message = uiState.lastSyncMessage
            )

            LowStockAlertBanner(lowStockCount = uiState.lowStockCount)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Welcome, ${uiState.userName.ifBlank { "Staff" }}",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color(0xFF1B5E20)
                    )

                    Text(
                        text = uiState.currentTimeText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF6D6D6D)
                    )
                }

                BranchIndicator(branchName = uiState.branchName)
            }

            if (uiState.isClockedIn) {
                FruityOutlinedButton(
                    text = "Clock Out",
                    onClick = viewModel::clockOut,
                    enabled = !uiState.isClockActionLoading
                )
            } else {
                FruityPrimaryButton(
                    text = "Clock In",
                    onClick = { viewModel.clockIn(null) },
                    isLoading = uiState.isClockActionLoading
                )
            }

            val errorMessage = uiState.error
            if (!errorMessage.isNullOrBlank()) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            FruitySectionTitle(
                title = "Operations",
                subtitle = "Offline-first branch tools"
            )

            FruityMenuCard(
                title = "Point of Sale",
                subtitle = "Create fruit shake transactions",
                icon = Icons.Default.PointOfSale,
                onClick = { onNavigate(STAFF_POS) }
            )

            FruityMenuCard(
                title = "Staff Log",
                subtitle = "View clock in and clock out records",
                icon = Icons.Default.Schedule,
                onClick = { onNavigate(STAFF_LOG) }
            )

            FruityMenuCard(
                title = "Waste Entry",
                subtitle = "Deduct spoiled or wasted inventory",
                icon = Icons.Default.Restaurant,
                onClick = { onNavigate(STAFF_WASTE_ENTRY) }
            )

            FruityMenuCard(
                title = "Waste History",
                subtitle = "Review waste records",
                icon = Icons.Default.History,
                onClick = { onNavigate(STAFF_WASTE_HISTORY) }
            )

            FruityMenuCard(
                title = "Inventory Monitoring",
                subtitle = "Read-only current branch stock",
                icon = Icons.Default.Inventory,
                onClick = { onNavigate(STAFF_INVENTORY) }
            )

            FruityMenuCard(
                title = "Inventory Adjustment",
                subtitle = "Correct stock levels with audit logs",
                icon = Icons.Default.Tune,
                onClick = { onNavigate(STAFF_ADJUSTMENT) }
            )

            FruityMenuCard(
                title = "Restock Entry",
                subtitle = "Add received ingredient stock",
                icon = Icons.Default.Inventory2,
                onClick = { onNavigate(STAFF_RESTOCK_ENTRY) }
            )

            FruityMenuCard(
                title = "Restock History",
                subtitle = "Review ingredient restocks",
                icon = Icons.Default.Restore,
                onClick = { onNavigate(STAFF_RESTOCK_HISTORY) }
            )

            FruityMenuCard(
                title = "Sales Summary",
                subtitle = "View today's completed sales",
                icon = Icons.Default.Assessment,
                onClick = { onNavigate(STAFF_SALES_SUMMARY) }
            )

            FruityMenuCard(
                title = "Transaction History",
                subtitle = "View completed and void transactions",
                icon = Icons.Default.History,
                onClick = { onNavigate(STAFF_TRANSACTION_HISTORY) }
            )
        }
    }
}