package com.example.fruitylicious

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

// ── COLORS ────────────────────────────────────────────────────────────────────

private val GreenHeader = Color(0xFF2E7D32)
private val BackgroundYellow = Color(0xFFFFEAA0)
private val OrangeAccent = Color(0xFFFF5722)
private val CardWhite = Color.White

// ── MAIN SCREEN ─────────────────────────────────────────────────────────────

@Composable
fun CheckoutScreen(
    navController: NavController,
    drawerState: DrawerState,
    scope: CoroutineScope
) {
    // UI State
    var amountReceived by remember { mutableStateOf("") }
    var selectedPaymentMethod by remember { mutableStateOf("None") } // "Cash" or "GCash"
    var showGCashPopup by remember { mutableStateOf(false) }
    var showReceiptPopup by remember { mutableStateOf(false) }

    // Constants
    val totalAmount = 85.0
    
    // Derived State for Real-time Change Calculation
    val receivedValue = amountReceived.toDoubleOrNull() ?: 0.0
    val change = if (receivedValue >= totalAmount) receivedValue - totalAmount else 0.0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundYellow)
            .verticalScroll(rememberScrollState())
    ) {
        
        // ── SECTION 1: TOP GREEN HEADER ──────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(GreenHeader)
                .padding(top = 48.dp, bottom = 16.dp, start = 20.dp, end = 20.dp)
        ) {
            // Burger Menu connected to Sidebar
            Column(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .clickable { scope.launch { drawerState.open() } }
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                repeat(3) {
                    Box(
                        modifier = Modifier
                            .width(22.dp)
                            .height(2.dp)
                            .background(Color.White, RoundedCornerShape(1.dp))
                    )
                }
            }

            Text(
                text = "CHECK OUT",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        Column(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth()
        ) {
            
            // ── SECTION 2: ORDER SUMMARY CARD ────────────────────────────────
            Text(
                text = "Order Summary",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1B1B1B)
            )
            
            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = CardWhite,
                shadowElevation = 4.dp
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    SummaryRow("Flavor", "Avocado")
                    SummaryRow("Size", "Medium")
                    SummaryRow("Add ons", "Pearl")
                    SummaryRow("Quantity", "1 pc")
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = Color(0xFFF0F0F0), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Total Amount",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.Black
                        )
                        Text(
                            text = "₱ 85",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = OrangeAccent
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── SECTION 3: AMOUNT RECEIVED INPUT (TYPEABLE) ──────────────────
            Text(
                text = "Amount Received",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1B1B1B)
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = amountReceived,
                onValueChange = { if (it.all { char -> char.isDigit() || char == '.' }) amountReceived = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("₱ 0.00", color = Color.LightGray) },
                prefix = { Text("₱ ", color = Color.Gray) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = CardWhite,
                    focusedContainerColor = CardWhite,
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = GreenHeader
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ── SECTION 4: REAL-TIME CHANGE DISPLAY ──────────────────────────
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = OrangeAccent,
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Change",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "₱${"%,.2f".format(change)}",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── SECTION 5: PAYMENT METHOD SELECTION ─────────────────────────
            Text(
                text = "PAYMENT METHOD",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1B1B1B)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Cash Payment Card (Turns Green when selected)
                PaymentMethodCard(
                    title = "Cash",
                    icon = "₱",
                    isSelected = selectedPaymentMethod == "Cash",
                    modifier = Modifier.weight(1f),
                    onClick = { selectedPaymentMethod = "Cash" }
                )
                // GCash Payment Card (Shows GCash Popup)
                PaymentMethodCard(
                    title = "Gcash",
                    icon = "G",
                    isSelected = selectedPaymentMethod == "GCash",
                    modifier = Modifier.weight(1f),
                    onClick = { 
                        selectedPaymentMethod = "GCash"
                        showGCashPopup = true
                    }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ── SECTION 6: CHECK OUT ACTION BUTTON ───────────────────────────
            Button(
                onClick = { if (selectedPaymentMethod != "None") showReceiptPopup = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .shadow(4.dp, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GreenHeader)
            ) {
                Text(
                    text = "Check Out",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            
            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    // ── SECTION 7: POP-UPS (GCASH & RECEIPT) ───────────────────────────────────
    
    // GCash QR Code Popup with X to close
    if (showGCashPopup) {
        GCashPopup(onDismiss = { showGCashPopup = false })
    }

    // Official Receipt Popup showing transaction details
    if (showReceiptPopup) {
        ReceiptPopup(
            receivedAmount = receivedValue,
            change = change,
            paymentMethod = selectedPaymentMethod,
            onDone = {
                showReceiptPopup = false
                navController.navigate("pos") // Redirect back to POS Screen
            }
        )
    }
}

// ── COMPONENT: SUMMARY ROW ───────────────────────────────────────────────────

@Composable
fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = Color.Gray, fontSize = 14.sp)
        Text(text = value, color = Color.Black, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

// ── COMPONENT: PAYMENT METHOD CARD ───────────────────────────────────────────

@Composable
fun PaymentMethodCard(
    title: String,
    icon: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(if (isSelected) GreenHeader else CardWhite)
    val contentColor by animateColorAsState(if (isSelected) Color.White else Color.Black)

    Surface(
        modifier = modifier
            .height(100.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        color = backgroundColor,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .border(2.dp, contentColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(text = icon, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = contentColor)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = contentColor)
        }
    }
}

// ── COMPONENT: GCASH POP-UP ──────────────────────────────────────────────────

@Composable
fun GCashPopup(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            modifier = Modifier.width(320.dp)
        ) {
            Box(modifier = Modifier.padding(20.dp)) {
                // Top-right X button to close
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .clickable { onDismiss() }
                        .size(24.dp),
                    tint = Color.Gray
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "G",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF007AFF)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Gcash",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF007AFF)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // QR Code Area
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .background(Color(0xFFF5F5F5), RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("QR CODE", color = Color.LightGray)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "M*** N****",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Text(
                        text = "09*********60",
                        fontSize = 16.sp,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

// ── COMPONENT: RECEIPT POP-UP ────────────────────────────────────────────────

@Composable
fun ReceiptPopup(
    receivedAmount: Double,
    change: Double,
    paymentMethod: String,
    onDone: () -> Unit
) {
    Dialog(onDismissRequest = onDone) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Green success checkmark
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(GreenHeader),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Success",
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Your Receipt",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF3E2723)
                )

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(color = Color(0xFFF5F5F5))
                Spacer(modifier = Modifier.height(16.dp))

                Column(modifier = Modifier.fillMaxWidth()) {
                    ReceiptRow("Order ID", "AV 1001")
                    ReceiptRow("Date & Time", "February 14, 2026")
                    ReceiptRow("Flavor", "Avocado")
                    ReceiptRow("Size", "Medium")
                    ReceiptRow("Add ons", "Pearl")
                    ReceiptRow("Quantity", "1 pc")
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = Color(0xFFF5F5F5))
                    Spacer(modifier = Modifier.height(16.dp))

                    ReceiptRow("Total Amount", "₱ 85")
                    ReceiptRow("Payment Received", "₱ ${"%.0f".format(receivedAmount)}")
                    ReceiptRow("Change", "₱ ${"%.0f".format(change)}")
                    ReceiptRow("Payment Method", paymentMethod)
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Done button redirects back to POS
                Button(
                    onClick = onDone,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GreenHeader)
                ) {
                    Text("Done", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ReceiptRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = Color.Gray, fontSize = 13.sp)
        Text(text = value, color = Color(0xFF334155), fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}
