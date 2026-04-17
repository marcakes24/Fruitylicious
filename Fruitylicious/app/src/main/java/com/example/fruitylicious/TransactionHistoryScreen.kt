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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import java.util.*

// ── DATA MODEL ──────────────────────────────────────────────────────────────

data class TransactionEntry(
    val id: String,
    val staff: String,
    val date: String,
    val time: String,
    val amount: String
)

// ── MAIN SCREEN ─────────────────────────────────────────────────────────────

@Composable
fun TransactionHistoryScreen(
    navController: NavController,
    drawerState: DrawerState,
    scope: CoroutineScope
) {
    // UI State for managing pop-ups
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedTransaction by remember { mutableStateOf<TransactionEntry?>(null) }
    
    // Usable Search and Date States
    var searchQuery by remember { mutableStateOf("") }
    var selectedDateText by remember { mutableStateOf("") }

    // Sample data for the list
    val transactions = listOf(
        TransactionEntry("AV 0001", "Mariz Tuliao", "4/14/2026", "12:38 AM", "₱85"),
        TransactionEntry("AV 0001", "Mariz Tuliao", "4/14/2026", "12:38 AM", "₱85"),
        TransactionEntry("AV 0001", "Mariz Tuliao", "4/14/2026", "12:38 AM", "₱85"),
        TransactionEntry("AV 0001", "Mariz Tuliao", "4/14/2026", "12:38 AM", "₱85"),
        TransactionEntry("AV 0001", "Mariz Tuliao", "4/14/2026", "12:38 AM", "₱85")
    )

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
                        // Search Field - Now Usable
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

                        // Date Selector Field - Now Usable
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = selectedDateText,
                                onValueChange = { },
                                readOnly = true, // Read-only because we use the picker
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
                            // Transparent overlay to catch clicks for the picker
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
                                color = Color(0xFFFFB300), // Orange badge
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "25 entries",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        HorizontalDivider(color = Color(0xFFF0F0F0), thickness = 1.dp)

                        // Transaction Items (Triggers Transaction Details Pop-up)
                        transactions.forEachIndexed { index, transaction ->
                            TransactionListItem(transaction) {
                                selectedTransaction = transaction
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

    // ── SECTION 4: DIALOGS (POP-UPS) ──────────────────────────────────────────

    // Custom Calendar Pop-up - Now Functional
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
    if (selectedTransaction != null) {
        TransactionDetailsDialog(
            transaction = selectedTransaction!!,
            onDismiss = { selectedTransaction = null }
        )
    }
}

// ── COMPONENT: TRANSACTION LIST ITEM ────────────────────────────────────────

@Composable
fun TransactionListItem(transaction: TransactionEntry, onClick: () -> Unit) {
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
                text = transaction.id,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color(0xFF333333)
            )
            Text(
                text = transaction.staff,
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
        
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = transaction.amount,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = Color(0xFFF57C00) // Deep orange
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = transaction.date,
                    fontSize = 11.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = transaction.time,
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
fun TransactionDetailsDialog(transaction: TransactionEntry, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                // Detail Header
                Text(
                    text = transaction.id,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF334155)
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Detail Rows
                DetailRow("Product", "Avocado")
                DetailRow("Size", "Small")
                DetailRow("Add ons", "Pearl")
                DetailRow("Qty", "1pc")
                DetailRow("Total Amount", transaction.amount, isAmount = true)
                DetailRow("Payment Method", "Cash")
                DetailRow("Date & Time", "February 10, 2026 - 10:00 AM")
                DetailRow("Processed by", transaction.staff)
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
