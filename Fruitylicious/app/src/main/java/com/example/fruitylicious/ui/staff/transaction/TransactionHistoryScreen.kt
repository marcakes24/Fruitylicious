package com.example.fruitylicious.ui.staff.transaction

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// ── DATA MODELS ──────────────────────────────────────────────────────────────

data class TransactionEntry(
    val id: String,
    val staff: String,
    val date: String,
    val time: String,
    val amount: Double,
    var status: String = "Completed",
    val branch: String = "B1",
    val product: String = "Avocado",
    val size: String = "Small",
    val addOns: String = "Pearl",
    val qty: String = "1pc",
    val paymentMethod: String = "Cash",
    val fullDateTime: String = "February 10, 2026 - 10:00 AM"
)

// ── MAIN SCREEN ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionHistoryScreen(
    navController: NavController,
    drawerState: DrawerState,
    scope: CoroutineScope
) {
    // UI State
    var searchQuery by remember { mutableStateOf("") }
    var selectedDateText by remember { mutableStateOf("") }
    var selectedBranch by remember { mutableStateOf("All") }
    
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedTransaction by remember { mutableStateOf<TransactionEntry?>(null) }
    var showVoidConfirm by remember { mutableStateOf(false) }

    // Functional Transaction List
    val transactionList = remember { 
        mutableStateListOf(
            TransactionEntry("AV 0001", "Mariz Tuliao", "04/14/2026", "12:38 AM", 85.0, branch = "B1"),
            TransactionEntry("AV 0001", "Mariz Tuliao", "04/14/2026", "12:38 AM", 85.0, branch = "B2"),
            TransactionEntry("AV 0001", "Mariz Tuliao", "04/14/2026", "12:38 AM", 85.0, branch = "B1"),
            TransactionEntry("AV 0001", "Mariz Tuliao", "04/14/2026", "12:38 AM", 85.0, branch = "B1"),
            TransactionEntry("AV 0002", "John Doe", "04/15/2026", "02:15 PM", 120.0, branch = "B2"),
            TransactionEntry("AV 0003", "Jane Smith", "04/13/2026", "11:20 AM", 95.0, branch = "B1")
        )
    }

    // Filter Logic
    val filteredTransactions = remember(searchQuery, selectedBranch, selectedDateText, transactionList.size) {
        transactionList.filter { item ->
            val matchesSearch = item.id.contains(searchQuery, ignoreCase = true) || 
                              item.staff.contains(searchQuery, ignoreCase = true)
            val matchesBranch = if (selectedBranch == "All") true else item.branch == selectedBranch
            val matchesDate = if (selectedDateText.isEmpty()) true else item.date == selectedDateText
            
            matchesSearch && matchesBranch && matchesDate
        }.reversed()
    }

    // Date Picker State
    val datePickerState = rememberDatePickerState()

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val sdf = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
                        selectedDateText = sdf.format(Date(millis))
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { 
                    selectedDateText = ""
                    showDatePicker = false 
                }) { Text("Clear") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFEAA0)) // Pale yellow background
    ) {
        
        // ── SECTION 1: TOP GREEN HEADER ──────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF2E7D32))
                .padding(top = 40.dp, bottom = 12.dp, start = 16.dp, end = 16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                    Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.White)
                }

                Text(
                    text = "TRANSACTION HISTORY",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                // Branch Selector
                Surface(
                    color = Color(0xFF1B5E20),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf("B1", "B2", "All").forEach { branch ->
                            val isSelected = selectedBranch == branch
                            Surface(
                                color = if (isSelected) Color.White else Color.Transparent,
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .clickable { selectedBranch = branch }
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(horizontal = 12.dp)
                                ) {
                                    Text(
                                        text = branch,
                                        color = if (isSelected) Color(0xFF2E7D32) else Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 24.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            
            // ── SECTION 2: SEARCH & DATE FILTER CARD ──────────────────────────
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = Color.White,
                    shadowElevation = 2.dp
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Search ingredient, staff, etc.", color = Color.LightGray, fontSize = 14.sp) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.LightGray) },
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = Color(0xFFEEEEEE),
                                focusedBorderColor = Color(0xFF2E7D32)
                            )
                        )

                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = selectedDateText,
                                onValueChange = {},
                                readOnly = true,
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("mm/dd/yyyy", color = Color.LightGray, fontSize = 14.sp) },
                                leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null, tint = Color.LightGray) },
                                trailingIcon = { Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = Color.Black) },
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedBorderColor = Color(0xFFEEEEEE),
                                    focusedBorderColor = Color(0xFF2E7D32)
                                )
                            )
                            Box(modifier = Modifier.matchParentSize().clickable { showDatePicker = true })
                        }
                    }
                }
            }

            // ── SECTION 3: TRANSACTION HISTORY LIST CARD ──────────────────────
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = Color.White,
                    shadowElevation = 2.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Transaction History",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF333333)
                            )
                            Surface(
                                color = Color(0xFFFFB300),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "${filteredTransactions.size} entries",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        HorizontalDivider(color = Color(0xFFF0F0F0), thickness = 1.dp)

                        if (filteredTransactions.isEmpty()) {
                            Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                Text("No transactions found", color = Color.Gray)
                            }
                        } else {
                            filteredTransactions.forEachIndexed { index, transaction ->
                                TransactionListItem(
                                    transaction = transaction,
                                    onClick = { selectedTransaction = transaction },
                                    onVoidClick = {
                                        selectedTransaction = transaction
                                        showVoidConfirm = true
                                    }
                                )
                                if (index < filteredTransactions.size - 1) {
                                    HorizontalDivider(color = Color(0xFFF0F0F0), thickness = 0.5.dp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ── DIALOGS ──────────────────────────────────────────────────────────────

    if (selectedTransaction != null && !showVoidConfirm) {
        TransactionDetailsDialog(
            transaction = selectedTransaction!!,
            onDismiss = { selectedTransaction = null },
            onVoidClick = { showVoidConfirm = true }
        )
    }

    if (showVoidConfirm && selectedTransaction != null) {
        VoidTransactionDialog(
            onConfirm = {
                // Find and update the original list
                val originalIndex = transactionList.indexOfFirst { it.id == selectedTransaction!!.id && it.date == selectedTransaction!!.date && it.time == selectedTransaction!!.time }
                if (originalIndex != -1) {
                    transactionList[originalIndex] = transactionList[originalIndex].copy(status = "Void")
                }
                showVoidConfirm = false
                selectedTransaction = null
            },
            onCancel = { showVoidConfirm = false }
        )
    }
}

// ── COMPONENTS ──────────────────────────────────────────────────────────────

@Composable
fun TransactionListItem(
    transaction: TransactionEntry, 
    onClick: () -> Unit,
    onVoidClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = transaction.id, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF333333))
            Text(text = transaction.status, fontSize = 12.sp, color = if(transaction.status == "Void") Color.Red else Color.Gray)
            Text(text = transaction.staff, fontSize = 12.sp, color = Color.Gray)
        }
        
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "₱${String.format("%.0f", transaction.amount)}",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color(0xFFF57C00)
            )
            
            if (transaction.status != "Void") {
                Surface(
                    color = Color.Red,
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.clickable { onVoidClick() }
                ) {
                    Text(
                        text = "Void",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
                    )
                }
            } else {
                Surface(
                    color = Color.LightGray,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "Voided",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
                    )
                }
            }
            
            Text(
                text = "${transaction.date}  ${transaction.time}",
                fontSize = 11.sp,
                color = Color.Gray,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun TransactionDetailsDialog(
    transaction: TransactionEntry,
    onDismiss: () -> Unit,
    onVoidClick: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(text = transaction.id, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF333333))
                Spacer(modifier = Modifier.height(20.dp))
                
                DetailItem("Product", transaction.product)
                DetailItem("Size", transaction.size)
                DetailItem("Add ons", transaction.addOns)
                DetailItem("Qty", transaction.qty)
                DetailItem("Total Amount", "₱${String.format("%.0f", transaction.amount)}", isAmount = true)
                DetailItem("Payment Method", transaction.paymentMethod)
                DetailItem("Date & Time", transaction.fullDateTime)
                DetailItem("Processed by", transaction.staff)
                DetailItem("Status", transaction.status)

                Spacer(modifier = Modifier.height(24.dp))
                
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE2E8F0)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Close", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                    
                    if (transaction.status != "Void") {
                        Button(
                            onClick = onVoidClick,
                            modifier = Modifier.weight(1f).height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Void", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DetailItem(label: String, value: String, isAmount: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 14.sp, color = Color(0xFF64748B))
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = if (isAmount) Color(0xFFF57C00) else Color(0xFF1E293B)
        )
    }
}

@Composable
fun VoidTransactionDialog(
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    Dialog(onDismissRequest = onCancel) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFFEE2E2),
                    modifier = Modifier.size(72.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red, modifier = Modifier.size(36.dp))
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                Text(text = "Void Transaction", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1E293B))
                Text(
                    text = "Are you sure you want to\nvoid this transaction",
                    textAlign = TextAlign.Center,
                    color = Color(0xFF64748B),
                    modifier = Modifier.padding(top = 8.dp),
                    lineHeight = 20.sp
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = onCancel,
                        modifier = Modifier.weight(1f).height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6B8E6B)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f).height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFCA5A5)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Void", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
