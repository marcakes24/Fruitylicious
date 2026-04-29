package com.example.fruitylicious.ui.staff.sales

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.fruitylicious.ui.shared.FruityInfoCard
import com.example.fruitylicious.ui.shared.FruitySectionTitle
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesSummaryScreen(
    onBack: () -> Unit,
    viewModel: SalesSummaryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val currency = NumberFormat.getCurrencyInstance(Locale("en", "PH"))

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sales Summary") },
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            FruitySectionTitle(
                title = "Today's Sales",
                subtitle = "${uiState.branchName} • ${uiState.dateText}"
            )

            FruityInfoCard(
                title = "Completed Sales",
                value = currency.format(uiState.completedSalesTotal)
            )

            FruityInfoCard(
                title = "Completed Transactions",
                value = uiState.completedTransactionCount.toString()
            )

            FruityInfoCard(
                title = "Voided Transactions",
                value = uiState.voidedTransactionCount.toString()
            )

            FruityInfoCard(
                title = "Average Transaction Value",
                value = currency.format(uiState.averageTransactionValue)
            )
        }
    }
}