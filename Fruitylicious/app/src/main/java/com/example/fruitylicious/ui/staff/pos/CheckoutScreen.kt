package com.example.fruitylicious.ui.staff.pos

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
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import com.example.fruitylicious.STAFF_RECEIPT
import com.example.fruitylicious.ui.shared.FruityEmptyState
import com.example.fruitylicious.ui.shared.FruityPrimaryButton
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    onNavigate: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: CheckoutViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val currency = NumberFormat.getCurrencyInstance(Locale("en", "PH"))

    LaunchedEffect(uiState.completedTransactionId) {
        val transactionId = uiState.completedTransactionId
        if (!transactionId.isNullOrBlank()) {
            onNavigate(STAFF_RECEIPT)
            viewModel.consumeCompletedTransaction()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Checkout") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFFFFDF6),
                    titleContentColor = Color(0xFF1B5E20)
                )
            )
        },
        bottomBar = {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = currency.format(uiState.totalAmount),
                            style = MaterialTheme.typography.titleLarge,
                            color = Color(0xFF1B5E20)
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

                    FruityPrimaryButton(
                        text = "Complete Payment",
                        onClick = viewModel::completePayment,
                        isLoading = uiState.isLoading,
                        enabled = uiState.cartItems.isNotEmpty()
                    )
                }
            }
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
            Text(
                text = "Payment Type",
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF1B5E20)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Cash", "GCash", "Card").forEach { paymentType ->
                    FilterChip(
                        selected = uiState.paymentType == paymentType,
                        onClick = { viewModel.onPaymentTypeChanged(paymentType) },
                        label = { Text(paymentType) },
                        leadingIcon = if (uiState.paymentType == paymentType) {
                            {
                                Icon(
                                    imageVector = Icons.Default.Payments,
                                    contentDescription = paymentType
                                )
                            }
                        } else {
                            null
                        }
                    )
                }
            }

            Text(
                text = "Order Items",
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF1B5E20)
            )

            if (uiState.cartItems.isEmpty()) {
                FruityEmptyState(
                    title = "No checkout items",
                    message = "Return to POS and add products to the cart."
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.cartItems, key = { it.productId }) { item ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = item.productName,
                                        style = MaterialTheme.typography.titleMedium
                                    )

                                    Text(
                                        text = "${item.quantity} × ${currency.format(item.unitPrice)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF6D6D6D)
                                    )
                                }

                                Text(
                                    text = currency.format(item.subtotal),
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}