package com.example.fruitylicious.ui.admin.reports

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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
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
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesReportScreen(
    onBack: () -> Unit,
    viewModel: ReportsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val report = uiState.salesReport
    val currency = NumberFormat.getCurrencyInstance(Locale("en", "PH"))

    LaunchedEffect(Unit) {
        viewModel.loadSalesReport()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sales Report") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadSalesReport() }) {
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
                title = "Sales",
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
                    title = "No sales report loaded",
                    message = "Tap refresh to load server report."
                )
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FruityInfoCard(
                        title = "Total Sales",
                        value = currency.format(report.totalSales),
                        modifier = Modifier.weight(1f)
                    )

                    FruityInfoCard(
                        title = "Transactions",
                        value = report.totalTransactions.toString(),
                        modifier = Modifier.weight(1f)
                    )
                }

                FruityInfoCard(
                    title = "Average Transaction Value",
                    value = currency.format(report.averageTransactionValue)
                )

                LazyColumn(
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(report.items, key = { it.productId }) { item ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = item.productName,
                                    style = MaterialTheme.typography.titleMedium
                                )

                                Text(
                                    text = "Quantity sold: ${item.quantitySold}",
                                    style = MaterialTheme.typography.bodyMedium
                                )

                                Text(
                                    text = "Gross sales: ${currency.format(item.grossSales)}",
                                    style = MaterialTheme.typography.bodyMedium,
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