package com.example.fruitylicious.ui.staff.pos

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.navigation.NavController
import com.example.fruitylicious.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// ── Colors ────────────────────────────────────────────────────────────────────
private val GreenHeader = Color(0xFF2C8C44)
private val BackgroundYellow = Color(0xFFFFEAA0)
private val CardWhite = Color.White
private val RptGreen = Color(0xFF2C8C44)
private val RptGreenLight = Color(0xFFE8F5E9)

enum class CheckoutStep { FORM, GCASH_QR, SUCCESS }

@Composable
fun CheckoutScreen(
    navController: NavController,
    drawerState: DrawerState,
    scope: CoroutineScope
) {
    var currentStep by remember { mutableStateOf(CheckoutStep.FORM) }
    var customerName by remember { mutableStateOf("") }
    var amountReceived by remember { mutableStateOf("") }
    var selectedPaymentMethod by remember { mutableStateOf("Cash") }
    
    // In a real app, these would come from a shared ViewModel or navigation arguments.
    val totalAmount = 60.0
    val receivedValue = amountReceived.toDoubleOrNull() ?: 0.0
    val change = if (receivedValue >= totalAmount) receivedValue - totalAmount else 0.0
    val isAmountValid = receivedValue >= totalAmount

    Box(modifier = Modifier.fillMaxSize().background(BackgroundYellow)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ── Header ────────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(GreenHeader)
                    .padding(top = 48.dp, bottom = 14.dp, start = 16.dp, end = 16.dp)
            ) {
                IconButton(
                    onClick = { scope.launch { drawerState.open() } },
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        repeat(3) {
                            Box(modifier = Modifier.width(20.dp).height(2.dp).background(Color.White))
                        }
                    }
                }

                Text(
                    text = "CHECK OUT",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            // ── Main Content Area ─────────────────────────────────────────────
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
                            customerName = customerName,
                            onCustomerNameChange = { customerName = it },
                            amountReceived = amountReceived,
                            onAmountChange = { amountReceived = it },
                            selectedPaymentMethod = selectedPaymentMethod,
                            onPaymentMethodSelect = { selectedPaymentMethod = it },
                            totalAmount = totalAmount,
                            change = change,
                            isAmountValid = isAmountValid,
                            onConfirm = {
                                if (selectedPaymentMethod == "Gcash") {
                                    currentStep = CheckoutStep.GCASH_QR
                                } else {
                                    currentStep = CheckoutStep.SUCCESS
                                }
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }
                    CheckoutStep.GCASH_QR -> {
                        GCashQRView(
                            onConfirmPayment = { currentStep = CheckoutStep.SUCCESS },
                            onBack = { currentStep = CheckoutStep.FORM }
                        )
                    }
                    CheckoutStep.SUCCESS -> {
                        PaymentSuccessView(
                            customerName = customerName,
                            totalAmount = totalAmount,
                            receivedAmount = receivedValue,
                            change = change,
                            paymentMethod = selectedPaymentMethod,
                            onNewOrder = {
                                navController.navigate("pos") {
                                    popUpTo("pos") { inclusive = true }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CheckoutForm(
    customerName: String,
    onCustomerNameChange: (String) -> Unit,
    amountReceived: String,
    onAmountChange: (String) -> Unit,
    selectedPaymentMethod: String,
    onPaymentMethodSelect: (String) -> Unit,
    totalAmount: Double,
    change: Double,
    isAmountValid: Boolean,
    onConfirm: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }

        // ── Order Summary Card ──────────────────────────────────────────
        CheckoutCard {
            Text("Order Summary", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("1x Guyabano", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Text("Medium", fontSize = 12.sp, color = Color.Gray)
                }
                Text("₱60.00", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(GreenHeader, RoundedCornerShape(8.dp))
                    .padding(16.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total Amount", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("₱${String.format(Locale.US, "%,.2f", totalAmount)}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }

        // ── Customer Name Card ──────────────────────────────────────────
        CheckoutCard {
            Text("Customer Name", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = customerName,
                onValueChange = onCustomerNameChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Enter customer name", color = Color.LightGray) },
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

        // ── Payment Method Card ─────────────────────────────────────────
        CheckoutCard {
            Text("Payment Method", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PaymentMethodButton(
                    modifier = Modifier.weight(1f),
                    label = "Cash",
                    icon = Icons.Outlined.Payments,
                    isSelected = selectedPaymentMethod == "Cash",
                    onClick = { onPaymentMethodSelect("Cash") }
                )
                PaymentMethodButton(
                    modifier = Modifier.weight(1f),
                    label = "Gcash",
                    icon = Icons.Outlined.CreditCard,
                    isSelected = selectedPaymentMethod == "Gcash",
                    onClick = { onPaymentMethodSelect("Gcash") }
                )
            }
        }

        // ── Amount Received & Change ────────────────────────────────────
        CheckoutCard {
            Text("Amount Received (P)", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = amountReceived,
                onValueChange = { if (it.all { char -> char.isDigit() || char == '.' }) onAmountChange(it) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("0.00", color = Color.LightGray) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
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
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf("100", "200", "500", "1000").forEach { valStr ->
                    AmountPresetButton(label = "₱ $valStr", modifier = Modifier.weight(1f), onClick = { onAmountChange(valStr) })
                }
                AmountPresetButton(label = "Exact", modifier = Modifier.weight(1f), isExact = true, onClick = { onAmountChange(totalAmount.toString()) })
            }

            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Change", fontSize = 14.sp, color = Color.Gray)
                Text("₱${String.format(Locale.US, "%,.2f", change)}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = RptGreen)
            }
            
            if (!isAmountValid && amountReceived.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Amount received must be greater than or equal to total.", color = Color.Red, fontSize = 11.sp)
            }
        }

        // ── Confirm Button ──────────────────────────────────────────────
        Button(
            onClick = onConfirm,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            enabled = isAmountValid,
            colors = ButtonDefaults.buttonColors(
                containerColor = GreenHeader,
                disabledContainerColor = Color.LightGray.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Outlined.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Confirm Payment", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun GCashQRView(onConfirmPayment: () -> Unit, onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.width(300.dp).shadow(8.dp, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            color = Color.White
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(30.dp).clip(CircleShape).background(Color(0xFF007AFF)), contentAlignment = Alignment.Center) {
                        Text("G", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Gcash", color = Color(0xFF007AFF), fontWeight = FontWeight.Bold, fontSize = 24.sp)
                }
                Spacer(modifier = Modifier.height(24.dp))
                
                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(8.dp))
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Try to load logo as placeholder for QR
                    Image(
                        painter = painterResource(id = R.drawable.qrcode),
                        contentDescription = "GCash QR Code",
                        modifier = Modifier.fillMaxSize(0.8f),
                        contentScale = ContentScale.Fit
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Text("M*** N****", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("09*********", color = Color.Gray, fontSize = 14.sp)
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onConfirmPayment,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            colors = ButtonDefaults.buttonColors(containerColor = GreenHeader),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Confirm Payment Received", fontWeight = FontWeight.Bold)
        }
        
        TextButton(onClick = onBack) {
            Text("Go back", color = Color.Gray)
        }
    }
}

@Composable
fun PaymentSuccessView(
    customerName: String,
    totalAmount: Double,
    receivedAmount: Double,
    change: Double,
    paymentMethod: String,
    onNewOrder: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Success Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(RptGreenLight)
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = RptGreen, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Payment Successful", color = RptGreen, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Receipt Card
        Surface(
            modifier = Modifier.fillMaxWidth().weight(1f).shadow(4.dp, RoundedCornerShape(2.dp)),
            color = Color.White
        ) {
            Column(
                modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Logo
                Text("FRUITYLICIOUS", fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, color = RptGreen)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Fruits and Shakes Station", fontSize = 12.sp, color = Color.Gray)
                Text("Dilliman, Quezon City", fontSize = 11.sp, color = Color.Gray)
                Text(SimpleDateFormat("M/dd/yyyy • h:mm a", Locale.US).format(Date()), fontSize = 11.sp, color = Color.Gray)
                Text("Tel: 091-237577", fontSize = 11.sp, color = Color.Gray)

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(color = Color(0xFFEEEEEE))
                Spacer(modifier = Modifier.height(16.dp))

                Text("Customer", fontSize = 11.sp, color = Color.Gray)
                Text(customerName.ifEmpty { "Walk-in" }, fontSize = 16.sp, fontWeight = FontWeight.Bold)

                Spacer(modifier = Modifier.height(20.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("1x Guyabano", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text("Medium", fontSize = 11.sp, color = Color.Gray)
                    }
                    Text("₱${String.format(Locale.US, "%,.2f", totalAmount)}", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(color = Color(0xFFEEEEEE))
                Spacer(modifier = Modifier.height(16.dp))

                ReceiptRow("Total", "₱${String.format(Locale.US, "%,.2f", totalAmount)}")
                ReceiptRow("Payment", paymentMethod)
                ReceiptRow("Cash Received", "₱${String.format(Locale.US, "%,.2f", receivedAmount)}")
                ReceiptRow("Change", "₱${String.format(Locale.US, "%,.2f", change)}")

                Spacer(modifier = Modifier.height(40.dp))
                Text("Served by: Admin User", fontSize = 11.sp, color = Color.Gray)
                Text("Thank you for choosing Fruitylicious!", fontSize = 11.sp, color = Color.Gray, textAlign = TextAlign.Center)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onNewOrder,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            colors = ButtonDefaults.buttonColors(containerColor = GreenHeader),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Outlined.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("New Order", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
fun ReceiptRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 13.sp, color = Color.Gray)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun CheckoutCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = CardWhite,
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
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
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = if (isSelected) GreenHeader else Color.Gray, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(label, color = if (isSelected) GreenHeader else Color.Gray, fontWeight = FontWeight.Bold, fontSize = 15.sp)
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
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isExact) GreenHeader else Color(0xFFEEEEEE))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isExact) GreenHeader else Color.DarkGray
            )
        }
    }
}
