package com.example.fruitylicious.ui.staff.pos

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.fruitylicious.R
import com.example.fruitylicious.STAFF_POS
import com.example.fruitylicious.data.repository.CartItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val GreenHeader = Color(0xFF2C8C44)
private val BackgroundYellow = Color(0xFFFFEAA0)
private val CardWhite = Color.White
private val RptGreen = Color(0xFF2C8C44)
private val RptGreenLight = Color(0xFFE8F5E9)

enum class CheckoutStep {
    FORM,
    GCASH_QR,
    SUCCESS
}

@Composable
fun CheckoutScreen(
    onNavigate: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: CheckoutViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var currentStep by remember { mutableStateOf(CheckoutStep.FORM) }
    var customerName by remember { mutableStateOf("") }
    var amountReceived by remember { mutableStateOf("") }
    var selectedPaymentMethod by remember { mutableStateOf("Cash") }

    val totalAmount = uiState.totalAmount
    val receivedValue = amountReceived.toDoubleOrNull() ?: 0.0
    val isCash = selectedPaymentMethod == "Cash"

    val change = if (isCash && receivedValue >= totalAmount) {
        receivedValue - totalAmount
    } else {
        0.0
    }

    val isAmountValid = if (isCash) {
        receivedValue >= totalAmount
    } else {
        uiState.cartItems.isNotEmpty()
    }

    LaunchedEffect(uiState.transactionId) {
        if (uiState.transactionId.isNotBlank()) {
            currentStep = CheckoutStep.SUCCESS
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundYellow)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            CheckoutHeader(
                onBack = onBack
            )

            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "CheckoutStepTransition"
            ) { step ->
                when (step) {
                    CheckoutStep.FORM -> {
                        CheckoutForm(
                            cartItems = uiState.cartItems,
                            customerName = customerName,
                            onCustomerNameChange = { customerName = it },
                            amountReceived = amountReceived,
                            onAmountChange = { amountReceived = it },
                            selectedPaymentMethod = selectedPaymentMethod,
                            onPaymentMethodSelect = {
                                selectedPaymentMethod = it
                                viewModel.clearError()
                            },
                            totalAmount = totalAmount,
                            change = change,
                            isAmountValid = isAmountValid,
                            isLoading = uiState.isLoading,
                            error = uiState.error,
                            onConfirm = {
                                if (selectedPaymentMethod == "Gcash") {
                                    currentStep = CheckoutStep.GCASH_QR
                                } else {
                                    viewModel.confirmPayment(
                                        paymentType = selectedPaymentMethod,
                                        receivedAmountText = amountReceived
                                    )
                                }
                            },
                            onBack = onBack
                        )
                    }

                    CheckoutStep.GCASH_QR -> {
                        GCashQRView(
                            isLoading = uiState.isLoading,
                            error = uiState.error,
                            onConfirmPayment = {
                                viewModel.confirmPayment(
                                    paymentType = selectedPaymentMethod,
                                    receivedAmountText = amountReceived
                                )
                            },
                            onBack = {
                                viewModel.clearError()
                                currentStep = CheckoutStep.FORM
                            }
                        )
                    }

                    CheckoutStep.SUCCESS -> {
                        PaymentSuccessView(
                            customerName = customerName,
                            receiptItems = uiState.receiptItems,
                            totalAmount = if (uiState.totalAmount > 0.0) {
                                uiState.totalAmount
                            } else {
                                uiState.receiptItems.sumOf { it.subtotal }
                            },
                            receivedAmount = uiState.receivedAmount,
                            change = uiState.change,
                            paymentMethod = uiState.paymentType,
                            transactionId = uiState.transactionId,
                            completedAt = uiState.completedAt,
                            onNewOrder = {
                                onNavigate(STAFF_POS)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CheckoutHeader(
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(GreenHeader)
            .padding(top = 48.dp, bottom = 14.dp, start = 16.dp, end = 16.dp)
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White
            )
        }

        Text(
            text = "CHECK OUT",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
fun CheckoutForm(
    cartItems: List<CartItem>,
    customerName: String,
    onCustomerNameChange: (String) -> Unit,
    amountReceived: String,
    onAmountChange: (String) -> Unit,
    selectedPaymentMethod: String,
    onPaymentMethodSelect: (String) -> Unit,
    totalAmount: Double,
    change: Double,
    isAmountValid: Boolean,
    isLoading: Boolean,
    error: String?,
    onConfirm: () -> Unit,
    onBack: () -> Unit
) {
    val isCash = selectedPaymentMethod == "Cash"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back"
            )
        }

        CheckoutCard {
            Text(
                text = "Order Summary",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (cartItems.isEmpty()) {
                Text(
                    text = "Cart is empty.",
                    color = Color.Gray,
                    fontSize = 13.sp
                )
            } else {
                cartItems.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${item.quantity}x ${item.productName}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )

                            Text(
                                text = buildCartDescription(item),
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }

                        Text(
                            text = "₱${String.format(Locale.US, "%,.2f", item.subtotal)}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(GreenHeader, RoundedCornerShape(8.dp))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Total Amount",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )

                    Text(
                        text = "₱${String.format(Locale.US, "%,.2f", totalAmount)}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }

        CheckoutCard {
            Text(
                text = "Customer Name",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = customerName,
                onValueChange = onCustomerNameChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text("Enter customer name", color = Color.LightGray)
                },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GreenHeader,
                    unfocusedBorderColor = Color(0xFFEEEEEE),
                    unfocusedContainerColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent
                ),
                singleLine = true
            )
        }

        CheckoutCard {
            Text(
                text = "Payment Method",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PaymentMethodButton(
                    modifier = Modifier.weight(1f),
                    label = "Cash",
                    icon = Icons.Outlined.Payments,
                    isSelected = selectedPaymentMethod == "Cash",
                    onClick = {
                        onPaymentMethodSelect("Cash")
                    }
                )

                PaymentMethodButton(
                    modifier = Modifier.weight(1f),
                    label = "Gcash",
                    icon = Icons.Outlined.CreditCard,
                    isSelected = selectedPaymentMethod == "Gcash",
                    onClick = {
                        onPaymentMethodSelect("Gcash")
                    }
                )
            }
        }

        if (isCash) {
            CheckoutCard {
                Text(
                    text = "Amount Received (₱)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = amountReceived,
                    onValueChange = {
                        if (it.all { char -> char.isDigit() || char == '.' }) {
                            onAmountChange(it)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text("0.00", color = Color.LightGray)
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal
                    ),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GreenHeader,
                        unfocusedBorderColor = Color(0xFFEEEEEE),
                        unfocusedContainerColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("100", "200", "500", "1000").forEach { value ->
                        AmountPresetButton(
                            label = "₱$value",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                onAmountChange(value)
                            }
                        )
                    }

                    AmountPresetButton(
                        label = "Exact",
                        modifier = Modifier.weight(1f),
                        isExact = true,
                        onClick = {
                            onAmountChange(totalAmount.toString())
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Change",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )

                    Text(
                        text = "₱${String.format(Locale.US, "%,.2f", change)}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = RptGreen
                    )
                }

                if (!isAmountValid && amountReceived.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Amount received must be greater than or equal to total.",
                        color = Color.Red,
                        fontSize = 11.sp
                    )
                }
            }
        }

        if (!error.isNullOrBlank()) {
            Text(
                text = error,
                color = Color.Red,
                fontSize = 12.sp
            )
        }

        Button(
            onClick = onConfirm,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            enabled = isAmountValid && cartItems.isNotEmpty() && !isLoading,
            colors = ButtonDefaults.buttonColors(
                containerColor = GreenHeader,
                disabledContainerColor = Color.LightGray.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isLoading) {
                Text(
                    text = "Processing...",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "Confirm Payment",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun GCashQRView(
    isLoading: Boolean,
    error: String?,
    onConfirmPayment: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier
                .width(300.dp)
                .shadow(8.dp, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            color = Color.White
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF007AFF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "G",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "Gcash",
                        color = Color(0xFF007AFF),
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(8.dp))
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.qrcode),
                        contentDescription = "GCash QR Code",
                        modifier = Modifier.fillMaxSize(0.8f),
                        contentScale = ContentScale.Fit
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "M*** N****",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )

                Text(
                    text = "09*********",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
        }

        if (!error.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = error,
                color = Color.Red,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onConfirmPayment,
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            colors = ButtonDefaults.buttonColors(containerColor = GreenHeader),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = if (isLoading) "Processing..." else "Confirm Payment Received",
                fontWeight = FontWeight.Bold
            )
        }

        TextButton(
            onClick = onBack,
            enabled = !isLoading
        ) {
            Text(
                text = "Go back",
                color = Color.Gray
            )
        }
    }
}

@Composable
fun PaymentSuccessView(
    customerName: String,
    receiptItems: List<CheckoutReceiptItem>,
    totalAmount: Double,
    receivedAmount: Double,
    change: Double,
    paymentMethod: String,
    transactionId: String,
    completedAt: Long,
    onNewOrder: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(RptGreenLight)
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = RptGreen,
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "Payment Successful",
                    color = RptGreen,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .shadow(4.dp, RoundedCornerShape(2.dp)),
            color = Color.White
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "FRUITYLICIOUS",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 24.sp,
                    color = RptGreen
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text("Fruits and Shakes Station", fontSize = 12.sp, color = Color.Gray)
                Text("Diliman, Quezon City", fontSize = 11.sp, color = Color.Gray)

                Text(
                    text = SimpleDateFormat("M/dd/yyyy • h:mm a", Locale.US).format(
                        Date(
                            if (completedAt > 0L) completedAt else System.currentTimeMillis()
                        )
                    ),
                    fontSize = 11.sp,
                    color = Color.Gray
                )

                if (transactionId.isNotBlank()) {
                    Text(
                        text = "Transaction: ${shortenTransactionId(transactionId)}",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }

                Text("Tel: 091-237577", fontSize = 11.sp, color = Color.Gray)

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(color = Color(0xFFEEEEEE))
                Spacer(modifier = Modifier.height(16.dp))

                Text("Customer", fontSize = 11.sp, color = Color.Gray)

                Text(
                    text = customerName.ifBlank { "Walk-in" },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(20.dp))

                receiptItems.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${item.quantity}x ${item.productName}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text = buildString {
                                    append(item.sizeName)

                                    if (item.addonsText.isNotBlank()) {
                                        append(" • ")
                                        append(item.addonsText)
                                    }
                                },
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }

                        Text(
                            text = "₱${String.format(Locale.US, "%,.2f", item.subtotal)}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(color = Color(0xFFEEEEEE))
                Spacer(modifier = Modifier.height(16.dp))

                ReceiptRow("Total", "₱${String.format(Locale.US, "%,.2f", totalAmount)}")
                ReceiptRow("Payment", paymentMethod)
                ReceiptRow("Cash Received", "₱${String.format(Locale.US, "%,.2f", receivedAmount)}")
                ReceiptRow("Change", "₱${String.format(Locale.US, "%,.2f", change)}")

                Spacer(modifier = Modifier.height(40.dp))

                Text("Served by: Staff User", fontSize = 11.sp, color = Color.Gray)

                Text(
                    text = "Thank you for choosing Fruitylicious!",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onNewOrder,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            colors = ButtonDefaults.buttonColors(containerColor = GreenHeader),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "New Order",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
fun ReceiptRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = Color.Gray
        )

        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun CheckoutCard(
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = CardWhite,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

@Composable
fun PaymentMethodButton(
    modifier: Modifier = Modifier,
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(64.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) RptGreenLight else Color.White,
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) GreenHeader else Color(0xFFEEEEEE)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) GreenHeader else Color.Gray,
                modifier = Modifier.size(22.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = label,
                color = if (isSelected) GreenHeader else Color.Gray,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }
    }
}

@Composable
fun AmountPresetButton(
    label: String,
    modifier: Modifier = Modifier,
    isExact: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(38.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        color = if (isExact) RptGreenLight else Color.White,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (isExact) GreenHeader else Color(0xFFEEEEEE)
        )
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isExact) GreenHeader else Color.DarkGray
            )
        }
    }
}

private fun buildCartDescription(item: CartItem): String {
    return buildString {
        append(item.sizeName)

        if (item.addons.isNotEmpty()) {
            append(" • ")
            append(
                item.addons.joinToString(", ") { addon ->
                    addon.addonName
                }
            )
        }
    }
}

private fun shortenTransactionId(transactionId: String): String {
    return if (transactionId.length <= 8) {
        transactionId.uppercase()
    } else {
        "TX-${transactionId.takeLast(6).uppercase()}"
    }
}