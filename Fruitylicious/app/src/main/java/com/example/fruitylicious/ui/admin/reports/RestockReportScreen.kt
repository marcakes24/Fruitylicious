package com.example.fruitylicious.ui.admin.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.fruitylicious.ui.shared.FruityEmptyState
import com.example.fruitylicious.ui.shared.FruityInfoCard
import com.example.fruitylicious.ui.shared.FruitySectionTitle
import com.example.fruitylicious.util.DateTimeUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestockReportScreen(
    onBack: () -> Unit,
    viewModel: ReportsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val report = uiState.restockReport

    LaunchedEffect(Unit) {
        viewModel.loadRestockReport()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Restock Report") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadRestockReport() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            FruitySectionTitle(
                title = "Restock",
                subtitle = "${uiState.branchName} • ${DateTimeUtil.formatDate(uiState.from)}"
            )

            val error = uiState.error
            if (!error.isNullOrBlank()) {
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error
                )
            }

            if (report == null) {
                FruityEmptyState(
                    title = "No restock report loaded",
                    message = "Tap refresh to load server report."
                )
            } else {
                FruityInfoCard(
                    title = "Total Restock Quantity",
                    value = report.totalRestockQuantity.toString()
                )

                LazyColumn(
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(report.items, key = { it.restockId }) { item ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Inventory2,
                                    contentDescription = "Restock",
                                    tint = Color(0xFF2E7D32)
                                )

                                Text(
                                    text = item.ingredientName,
                                    style = MaterialTheme.typography.titleMedium
                                )

                                Text(
                                    text = "Quantity Added: ${item.quantityAdded} ${item.unitType}",
                                    style = MaterialTheme.typography.bodyMedium
                                )

                                Text(
                                    text = "Supplier: ${item.supplier}",
                                    style = MaterialTheme.typography.bodyMedium
                                )

                                Text(
                                    text = "Logged by: ${item.userName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF6D6D6D)
                                )

                                Text(
                                    text = DateTimeUtil.formatDateTime(item.dateTime),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF6D6D6D)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}