package com.example.fruitylicious.ui.admin.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.fruitylicious.ADMIN_AUDIT_LOGS
import com.example.fruitylicious.ADMIN_INGREDIENTS
import com.example.fruitylicious.ADMIN_INVENTORY
import com.example.fruitylicious.ADMIN_PRODUCTS
import com.example.fruitylicious.ADMIN_RECIPES
import com.example.fruitylicious.ADMIN_REPORTS_DASHBOARD
import com.example.fruitylicious.ADMIN_RESTOCK_HISTORY
import com.example.fruitylicious.ADMIN_STAFF_LOGS
import com.example.fruitylicious.ADMIN_USERS
import com.example.fruitylicious.ADMIN_WASTE_HISTORY
import com.example.fruitylicious.STAFF_POS
import com.example.fruitylicious.STAFF_TRANSACTION_HISTORY
import com.example.fruitylicious.ui.shared.BranchIndicator
import com.example.fruitylicious.ui.shared.FruityInfoCard
import com.example.fruitylicious.ui.shared.FruityMenuCard
import com.example.fruitylicious.ui.shared.FruitySectionTitle
import com.example.fruitylicious.ui.shared.LowStockAlertBanner
import com.example.fruitylicious.ui.shared.SyncStatusBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit,
    viewModel: AdminDashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin Dashboard") },
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
                        text = "Welcome, ${uiState.userName.ifBlank { "Admin" }}",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color(0xFF1B5E20)
                    )

                    Text(
                        text = "Cross-branch management console",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF6D6D6D)
                    )
                }

                BranchIndicator(branchName = uiState.selectedBranchName)
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FruityInfoCard(
                    title = "Products",
                    value = uiState.productCount.toString(),
                    modifier = Modifier.weight(1f)
                )

                FruityInfoCard(
                    title = "Ingredients",
                    value = uiState.ingredientCount.toString(),
                    modifier = Modifier.weight(1f)
                )
            }

            FruitySectionTitle(
                title = "Staff Tools",
                subtitle = "Admin can also access branch POS tools"
            )

            FruityMenuCard(
                title = "Point of Sale",
                subtitle = "Create branch transactions",
                icon = Icons.Default.Store,
                onClick = { onNavigate(STAFF_POS) }
            )

            FruityMenuCard(
                title = "Transaction History",
                subtitle = "View local branch transactions",
                icon = Icons.Default.ReceiptLong,
                onClick = { onNavigate(STAFF_TRANSACTION_HISTORY) }
            )

            FruitySectionTitle(
                title = "Management",
                subtitle = "Maintain global and branch-specific records"
            )

            FruityMenuCard(
                title = "Manage Products",
                subtitle = "Add, edit, and delete fruit shake products",
                icon = Icons.Default.Fastfood,
                onClick = { onNavigate(ADMIN_PRODUCTS) }
            )

            FruityMenuCard(
                title = "Manage Ingredients",
                subtitle = "Maintain ingredients, packaging, and thresholds",
                icon = Icons.Default.Kitchen,
                onClick = { onNavigate(ADMIN_INGREDIENTS) }
            )

            FruityMenuCard(
                title = "Recipe Management",
                subtitle = "Map products to required ingredients",
                icon = Icons.Default.RestaurantMenu,
                onClick = { onNavigate(ADMIN_RECIPES) }
            )

            FruityMenuCard(
                title = "Inventory Monitoring",
                subtitle = "View inventory by branch and adjust stock",
                icon = Icons.Default.Inventory,
                onClick = { onNavigate(ADMIN_INVENTORY) }
            )

            FruityMenuCard(
                title = "Waste",
                subtitle = "Review and enter waste logs",
                icon = Icons.Default.Warning,
                onClick = { onNavigate(ADMIN_WASTE_HISTORY) }
            )

            FruityMenuCard(
                title = "Restock",
                subtitle = "Review and enter restock logs",
                icon = Icons.Default.Restore,
                onClick = { onNavigate(ADMIN_RESTOCK_HISTORY) }
            )

            FruityMenuCard(
                title = "Users",
                subtitle = "Manage admin and staff accounts",
                icon = Icons.Default.People,
                onClick = { onNavigate(ADMIN_USERS) }
            )

            FruityMenuCard(
                title = "Staff Logs",
                subtitle = "Filter staff attendance logs",
                icon = Icons.Default.People,
                onClick = { onNavigate(ADMIN_STAFF_LOGS) }
            )

            FruityMenuCard(
                title = "Reports",
                subtitle = "Sales, inventory, waste, restock, and transactions",
                icon = Icons.Default.Assessment,
                onClick = { onNavigate(ADMIN_REPORTS_DASHBOARD) }
            )

            FruityMenuCard(
                title = "Audit Logs",
                subtitle = "Review user actions and syncable audit records",
                icon = Icons.Default.FactCheck,
                onClick = { onNavigate(ADMIN_AUDIT_LOGS) }
            )
        }
    }
}