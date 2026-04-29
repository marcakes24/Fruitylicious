package com.example.fruitylicious.ui.staff.transactions

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
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ReceiptLong
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.fruitylicious.data.local.entity.TransactionEntity
import com.example.fruitylicious.ui.shared.FruityEmptyState
import com.example.fruitylicious.util.DateTimeUtil
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionHistoryScreen(
    onBack: () -> Unit,
    viewModel: TransactionHistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val currency = NumberFormat.getCurrencyInstance(Locale("en", "PH"))

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transaction History") },
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
        if (uiState.transactions.isEmpty()) {
            FruityEmptyState(
                title = "No transactions",
                message = "Completed and void transactions will appear here.",
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFFFFDF6))
                    .padding(padding)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFFFFDF6))
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(uiState.transactions, key = { it.transactionId }) { transaction ->
                    TransactionCard(
                        transaction = transaction,
                        amountText = currency.format(transaction.totalAmount),
                        onVoid = {
                            viewModel.voidTransaction(transaction.transactionId)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun TransactionCard(
    transaction: TransactionEntity,
    amountText: String,
    onVoid: () -> Unit
) {
    val isVoid = transaction.status == "void"

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isVoid) Color(0xFFFFEBEE) else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isVoid) Icons.Default.Cancel else Icons.Default.CheckCircle,
                        contentDescription = transaction.status,
                        tint = if (isVoid) Color(0xFFB71C1C) else Color(0xFF2E7D32)
                    )

                    Column {
                        Text(
                            text = transaction.transactionId.take(8).uppercase(),
                            style = MaterialTheme.typography.titleMedium
                        )

                        Text(
                            text = DateTimeUtil.formatDateTime(transaction.dateTime),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF6D6D6D)
                        )
                    }
                }

                Text(
                    text = amountText,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isVoid) Color(0xFFB71C1C) else Color(0xFF1B5E20)
                )
            }

            Text(
                text = "Payment: ${transaction.paymentType}",
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = "Status: ${transaction.status}",
                style = MaterialTheme.typography.bodyMedium,
                color = if (isVoid) Color(0xFFB71C1C) else Color(0xFF2E7D32)
            )

            if (!isVoid) {
                FilledTonalButton(onClick = onVoid) {
                    Icon(
                        imageVector = Icons.Default.ReceiptLong,
                        contentDescription = "Void"
                    )
                    Text(
                        text = "Void Transaction",
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    }
}