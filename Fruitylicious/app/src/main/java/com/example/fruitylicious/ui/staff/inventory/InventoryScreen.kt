package com.example.fruitylicious.ui.staff.inventory

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.example.fruitylicious.ui.shared.FruityEmptyState
import com.example.fruitylicious.ui.shared.LowStockAlertBanner

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    onBack: () -> Unit,
    viewModel: InventoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Inventory Monitoring") },
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
        ) {
            LowStockAlertBanner(lowStockCount = uiState.lowStockIngredientIds.size)

            if (uiState.inventory.isEmpty()) {
                FruityEmptyState(
                    title = "No inventory",
                    message = "Inventory records will appear after sync.",
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(uiState.inventory, key = { "${it.ingredientId}:${it.branchId}" }) { item ->
                        val ingredient = uiState.ingredientNames[item.ingredientId] ?: "Ingredient ${item.ingredientId}"
                        val unit = uiState.ingredientUnits[item.ingredientId] ?: ""
                        val isLowStock = uiState.lowStockIngredientIds.contains(item.ingredientId)

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isLowStock) Color(0xFFFFF8E1) else Color.White
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Inventory,
                                        contentDescription = ingredient,
                                        tint = Color(0xFF2E7D32)
                                    )

                                    Column {
                                        Text(
                                            text = ingredient,
                                            style = MaterialTheme.typography.titleMedium
                                        )

                                        Text(
                                            text = if (isLowStock) "Low stock" else "In stock",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (isLowStock) Color(0xFFF57F17) else Color(0xFF6D6D6D)
                                        )
                                    }
                                }

                                Text(
                                    text = "${item.currentStock} $unit",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color(0xFF1B5E20)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}