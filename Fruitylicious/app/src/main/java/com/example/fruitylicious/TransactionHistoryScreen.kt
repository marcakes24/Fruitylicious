package com.example.fruitylicious

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.fruitylicious.data.local.entity.AppDatabase
import com.example.fruitylicious.data.local.entity.TransactionEntity
import com.example.fruitylicious.viewmodel.TransactionViewModel
import com.example.fruitylicious.viewmodel.TransactionDetailData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import java.util.*
import java.text.SimpleDateFormat

// ── MAIN SCREEN ─────────────────────────────────────────────────────────────

@Composable
fun TransactionHistoryScreen(
    navController: NavController,
    drawerState: DrawerState,
    scope: CoroutineScope
) {
    // Initialize ViewModel
    val context = LocalContext.current
    val viewModel = remember {
        val database = AppDatabase.getDatabase(context)
        TransactionViewModel(
            transactionDao = database.transactionDao(),
            transactionItemDao = database.transactionItemDao(),
            userDao = database.userDao(),
            productDao = database.productDao()
        )
    }

    // UI State for managing pop-ups
    var showDatePicker by remember { mutableStateOf(false) }
    var showVoidConfirmation by remember { mutableStateOf(false) }
    var transactionToVoid by remember { mutableStateOf<String?>(null) }

    // Usable Search and Date States
    var searchQuery by remember { mutableStateOf("") }
    var selectedDateText by remember { mutableStateOf("") }

    // Collect states from ViewModel
    val transactions by viewModel.transactions.collectAsState()
    val selectedTransactionDetails by viewModel.selectedTransactionDetails.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    // Apply search query when it changes
    LaunchedEffect(searchQuery) {
        viewModel.searchTransactions(searchQuery)
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
                // Hamburger Menu Trigger
                Column(
                    modifier = Modifier
                        .clickable { scope.launch { drawerState.open() } }
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    repeat(3) {
                        Box(
                            modifier = Modifier
                                .width(20.dp)
                                .height(2.dp)
                                .background(Color.White)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Page Title
                Text(
                    text = "TRANSACTION HISTORY",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 20.dp, bottom = 20.dp),
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
                        // Search Field
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Search transaction ID, payment type, etc.", color = Color.LightGray, fontSize = 14.sp) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.LightGray) },
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = Color(0xFFEEEEEE),
                                focusedBorderColor = Color(0xFF2E7D32)
                            )
                        )

                        // Date Selector Field
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = selectedDateText,
                                onValueChange = { },
                                readOnly = true,
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("mm/dd/yyyy", color = Color.LightGray, fontSize = 14.sp) },
                                leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null, tint = Color.LightGray) },
                                trailingIcon = { 
                                    Icon(
                                        Icons.Default.CalendarMonth, 
                                        contentDescription = null, 
                                        tint = Color.Black
                                    ) 
                                },
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedBorderColor = Color(0xFFEEEEEE),
                                    focusedBorderColor = Color(0xFF2E7D32)
                                )
                            )
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clickable { showDatePicker = true }
                            )
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
                        // List Header with Entry Count
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
                                    text = "${transactions.size} ${if (transactions.size == 1) "entry" else "entries"}",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        HorizontalDivider(color = Color(0xFFF0F0F0), thickness = 1.dp)

                        // Transaction Items or Loading/Empty State
                        if (isLoading) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(40.dp),
                                    color = Color(0xFF2E7D32)
                                )
                            }
                        } else if (transactions.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No transactions found",
                                    fontSize = 14.sp,
                                    color = Color.Gray
                                )
                            }
                        } else {
                            transactions.forEachIndexed { index, transaction ->
                                TransactionListItemNew(transaction) {
                                    viewModel.loadTransactionDetails(transaction.transactionId)
                                }
                                if (index < transactions.size - 1) {
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

    // Custom Calendar Pop-up
    if (showDatePicker) {
        CustomDatePickerDialog(
            onDismiss = { showDatePicker = false },
            onDateSelected = { date ->
                selectedDateText = date
                showDatePicker = false
            }
        )
    }

    // Transaction Details Pop-up
    if (selectedTransactionDetails != null) {
        TransactionDetailsDialogNew(
            transaction = selectedTransactionDetails!!,
            onDismiss = { viewModel.clearSelectedTransaction() },
            onVoidClick = {
                transactionToVoid = selectedTransactionDetails!!.transactionId
                showVoidConfirmation = true
            }
        )
    }

    // Void Transaction Confirmation Dialog
    if (showVoidConfirmation && transactionToVoid != null) {
        VoidTransactionConfirmationDialog(
            onConfirm = {
                viewModel.voidTransaction(transactionToVoid!!)
                showVoidConfirmation = false
                transactionToVoid = null
            },
            onCancel = {
                showVoidConfirmation = false
                transactionToVoid = null
            }
        )
    }
}

// ── COMPONENT: TRANSACTION LIST ITEM ────────────────────────────────────────

@Composable
fun TransactionListItemNew(
    transaction: TransactionEntity,
    onClick: () -> Unit
) {
    val dateFormat = SimpleDateFormat("M/d/yyyy", Locale.getDefault())
    val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    val date = dateFormat.format(Date(transaction.dateTime))
    val time = timeFormat.format(Date(transaction.dateTime))
    val amount = "₱%.2f".format(transaction.totalAmount)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = transaction.transactionId,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color(0xFF333333)
            )
            Text(
                text = transaction.paymentType,
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
        
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = amount,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = Color(0xFFF57C00)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = date,
                    fontSize = 11.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = time,
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

// ── COMPONENT: CALENDAR POP-UP (DATE PICKER) ────────────────────────────────

@Composable
fun CustomDatePickerDialog(onDismiss: () -> Unit, onDateSelected: (String) -> Unit) {
    var calendar by remember { mutableStateOf(Calendar.getInstance()) }
    val monthNames = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
    
    val currentMonth = calendar.get(Calendar.MONTH)
    val currentYear = calendar.get(Calendar.YEAR)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = Color.White,
            modifier = Modifier.width(340.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Calendar Header (Month/Year Selection)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        val newCal = calendar.clone() as Calendar
                        newCal.add(Calendar.MONTH, -1)
                        calendar = newCal
                    }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous Month")
                    }
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(monthNames[currentMonth], fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color(0xFF2E7D32))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(currentYear.toString(), fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color(0xFF2E7D32))
                    }

                    IconButton(onClick = {
                        val newCal = calendar.clone() as Calendar
                        newCal.add(Calendar.MONTH, 1)
                        calendar = newCal
                    }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next Month")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Days of Week Header
                val daysOfWeek = listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su")
                Row(modifier = Modifier.fillMaxWidth()) {
                    daysOfWeek.forEach { day ->
                        Text(
                            text = day,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Calendar Grid Days Calculation
                val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
                val firstDayOfMonthCal = calendar.clone() as Calendar
                firstDayOfMonthCal.set(Calendar.DAY_OF_MONTH, 1)
                // Adjusting Calendar.DAY_OF_WEEK (Sun=1) to Mo=0, Tu=1... Su=6
                var firstDayOfWeek = firstDayOfMonthCal.get(Calendar.DAY_OF_WEEK) - 2
                if (firstDayOfWeek < 0) firstDayOfWeek = 6 // Sunday was 1, so 1-2 = -1, becomes 6

                // Previous month days to fill start
                val prevMonthCal = calendar.clone() as Calendar
                prevMonthCal.add(Calendar.MONTH, -1)
                val daysInPrevMonth = prevMonthCal.getActualMaximum(Calendar.DAY_OF_MONTH)

                Column {
                    var dayCounter = 1
                    var nextMonthDayCounter = 1
                    for (week in 0 until 6) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            for (dayIndex in 0 until 7) {
                                val currentGridIndex = week * 7 + dayIndex
                                
                                val day: Int
                                val isCurrentMonth: Boolean
                                val isNeighbor: Boolean
                                
                                if (currentGridIndex < firstDayOfWeek) {
                                    day = daysInPrevMonth - (firstDayOfWeek - currentGridIndex - 1)
                                    isCurrentMonth = false
                                    isNeighbor = true
                                } else if (dayCounter <= daysInMonth) {
                                    day = dayCounter
                                    isCurrentMonth = true
                                    isNeighbor = false
                                    dayCounter++
                                } else {
                                    day = nextMonthDayCounter
                                    isCurrentMonth = false
                                    isNeighbor = true
                                    nextMonthDayCounter++
                                }

                                // Selection logic (just an example, let's say today or first of month)
                                val isSelected = false // We can add selection state if needed

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .padding(2.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) Color(0xFF2E7D32) else Color.Transparent)
                                        .clickable { 
                                            if (isCurrentMonth) {
                                                val formattedMonth = String.format("%02d", currentMonth + 1)
                                                val formattedDay = String.format("%02d", day)
                                                onDateSelected("$formattedMonth/$formattedDay/$currentYear")
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = day.toString(),
                                        color = if (isSelected) Color.White 
                                                else if (isNeighbor) Color(0xFF2E7D32) // Styled per image
                                                else Color.Black,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                        if (dayCounter > daysInMonth && week >= 4) break
                    }
                }
            }
        }
    }
}

// ── COMPONENT: TRANSACTION DETAILS POP-UP ───────────────────────────────────

@Composable
fun TransactionDetailsDialogNew(
    transaction: TransactionDetailData,
    onDismiss: () -> Unit,
    onVoidClick: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                // Detail Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = transaction.transactionId,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF334155)
                    )

                    Surface(
                        color = if (transaction.status == "COMPLETED") Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = transaction.status,
                            color = if (transaction.status == "COMPLETED") Color(0xFF2E7D32) else Color(0xFFC62828),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                
                // Detail Rows
                DetailRow("Staff Member", transaction.staffName)
                DetailRow("Date & Time", transaction.date)
                DetailRow("Payment Method", "Cash")
                DetailRow("Total Amount", transaction.amount, isAmount = true)

                // Transaction Items
                if (transaction.items.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Items",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF475569)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    transaction.items.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${item.productName} x${item.quantity}",
                                fontSize = 12.sp,
                                color = Color(0xFF475569)
                            )
                            Text(
                                text = "₱%.2f".format(item.subtotal),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF1E293B)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action Buttons
                if (transaction.status != "VOID") {
                    Button(
                        onClick = onVoidClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFEF5350)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Void",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Void Transaction",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// ── COMPONENT: VOID TRANSACTION CONFIRMATION DIALOG ────────────────────────

@Composable
fun VoidTransactionConfirmationDialog(
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    Dialog(onDismissRequest = onCancel) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Trash Icon
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFFFEBEE)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Void",
                        tint = Color(0xFFF44336),
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Void Transaction",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF1E293B)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Are you sure you want to void this transaction?",
                    fontSize = 14.sp,
                    color = Color(0xFF64748B),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Buttons Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onCancel,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2E7D32)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "Cancel",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = onConfirm,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFEF5350)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "Void",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// ── COMPONENT: REUSABLE DETAIL ROW ──────────────────────────────────────────

@Composable
fun DetailRow(label: String, value: String, isAmount: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = Color(0xFF475569)
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = if (isAmount) Color(0xFFF57C00) else Color(0xFF1E293B),
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f).padding(start = 16.dp)
        )
    }
}

// ── PREVIEW ──────────────────────────────────────────────────────────────────

@Preview(showBackground = true)
@Composable
fun TransactionHistoryScreenPreview() {
    TransactionHistoryScreen(
        navController = rememberNavController(),
        drawerState = rememberDrawerState(initialValue = DrawerValue.Closed),
        scope = MainScope()
    )
}
