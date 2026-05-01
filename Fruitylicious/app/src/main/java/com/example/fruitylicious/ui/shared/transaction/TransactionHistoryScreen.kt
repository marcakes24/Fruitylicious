package com.example.fruitylicious.ui.shared.transaction

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import com.example.fruitylicious.ui.shared.SharedDrawerContent
import com.example.fruitylicious.ui.shared.SharedScreenMode

private val ThGreen = Color(0xFF2E7D32)
private val ThPageBg = Color(0xFFFFEAA0)
private val ThOrange = Color(0xFFF57C00)
private val ThTextMain = Color(0xFF333333)
private val ThTextSub = Color.Gray
private val ThRed = Color.Red

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionHistoryScreen(
    navController: NavController,
    mode: SharedScreenMode = SharedScreenMode.ADMIN,
    userName: String = "User",
    branchName: String = "",
    onLogout: () -> Unit = {},
    viewModel: TransactionHistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var selectedBranch by remember(uiState.isAdmin, uiState.userBranchId) { 
        mutableStateOf(if (uiState.isAdmin) "All" else uiState.userBranchId) 
    }
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedDateMillis by remember { mutableLongStateOf(0L) }

    var selectedTransaction by remember { mutableStateOf<TransactionHistoryRow?>(null) }
    var transactionToVoid by remember { mutableStateOf<TransactionHistoryRow?>(null) }

    val datePickerState = rememberDatePickerState()

    val selectedDateText = remember(selectedDateMillis) {
        if (selectedDateMillis == 0L) {
            ""
        } else {
            SimpleDateFormat("MM/dd/yyyy", Locale.US).format(Date(selectedDateMillis))
        }
    }

    val filteredTransactions = uiState.transactions.filter { transaction ->
        val matchesSearch =
            transaction.displayId.contains(searchQuery, ignoreCase = true) ||
                    transaction.transactionId.contains(searchQuery, ignoreCase = true) ||
                    transaction.staffName.contains(searchQuery, ignoreCase = true) ||
                    transaction.username.contains(searchQuery, ignoreCase = true)

        val matchesBranch = when (selectedBranch) {
            "B1" -> transaction.branchId == 1
            "B2" -> transaction.branchId == 2
            else -> true
        }

        val matchesDate = if (selectedDateMillis == 0L) {
            true
        } else {
            isSameDay(transaction.dateTime, selectedDateMillis)
        }

        matchesSearch && matchesBranch && matchesDate
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedDateMillis = datePickerState.selectedDateMillis ?: 0L
                        showDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        selectedDateMillis = 0L
                        showDatePicker = false
                    }
                ) {
                    Text("Clear")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color.Transparent,
                drawerTonalElevation = 0.dp
            ) {
                SharedDrawerContent(
                    mode = mode,
                    navController = navController,
                    drawerState = drawerState,
                    scope = scope,
                    userName = userName,
                    branchName = branchName,
                    onLogout = onLogout
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(ThPageBg)
        ) {
            Header(
                selectedBranch = selectedBranch,
                isAdmin = uiState.isAdmin,
                onBranchSelect = {
                    selectedBranch = it
                    viewModel.clearMessages()
                },
                onMenuClick = {
                    scope.launch {
                        drawerState.open()
                    }
                }
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 24.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (!uiState.error.isNullOrBlank()) {
                    item {
                        Text(
                            text = uiState.error ?: "",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 13.sp
                        )
                    }
                }

                if (!uiState.successMessage.isNullOrBlank()) {
                    item {
                        Text(
                            text = uiState.successMessage ?: "",
                            color = ThGreen,
                            fontSize = 13.sp
                        )
                    }
                }

                item {
                    FilterCard(
                        searchQuery = searchQuery,
                        onSearchChange = { searchQuery = it },
                        selectedDateText = selectedDateText,
                        onDateClick = { showDatePicker = true }
                    )
                }

                item {
                    TransactionHistoryCard(
                        transactions = filteredTransactions,
                        isLoading = uiState.isLoading,
                        onTransactionClick = { selectedTransaction = it },
                        onVoidClick = { transactionToVoid = it }
                    )
                }
            }
        }
    }

    selectedTransaction?.let { transaction ->
        if (transactionToVoid == null) {
            TransactionDetailsDialog(
                transaction = transaction,
                onDismiss = { selectedTransaction = null },
                onVoidClick = {
                    transactionToVoid = transaction
                }
            )
        }
    }

    transactionToVoid?.let { transaction ->
        VoidTransactionDialog(
            onConfirm = {
                viewModel.voidTransaction(transaction.transactionId)
                transactionToVoid = null
                selectedTransaction = null
            },
            onCancel = {
                transactionToVoid = null
            }
        )
    }
}

@Composable
private fun Header(
    selectedBranch: String,
    isAdmin: Boolean,
    onBranchSelect: (String) -> Unit,
    onMenuClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(ThGreen)
            .padding(top = 40.dp, bottom = 12.dp, start = 16.dp, end = 16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = onMenuClick) {
                Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.White)
            }

            Text(
                text = "TRANSACTION HISTORY",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )

            if (isAdmin) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFF5F5F5))
                        .padding(4.dp)
                ) {
                    listOf("B1", "B2", "All").forEach { branch ->
                        val isSelected = selectedBranch == branch

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) ThGreen else Color.Transparent)
                                .clickable { onBranchSelect(branch) }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = branch,
                                color = if (isSelected) Color.White else Color(0xFF666E7A),
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

@Composable
private fun FilterCard(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    selectedDateText: String,
    onDateClick: () -> Unit
) {
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
                onValueChange = onSearchChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = "Search transaction or staff",
                        color = Color.LightGray,
                        fontSize = 14.sp
                    )
                },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = Color.LightGray)
                },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color(0xFFEEEEEE),
                    focusedBorderColor = ThGreen
                ),
                singleLine = true
            )

            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = selectedDateText,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            text = "mm/dd/yyyy",
                            color = Color.LightGray,
                            fontSize = 14.sp
                        )
                    },
                    leadingIcon = {
                        Icon(Icons.Default.DateRange, contentDescription = null, tint = Color.LightGray)
                    },
                    trailingIcon = {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = Color.Black)
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = Color(0xFFEEEEEE),
                        focusedBorderColor = ThGreen
                    )
                )

                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable { onDateClick() }
                )
            }
        }
    }
}

@Composable
private fun TransactionHistoryCard(
    transactions: List<TransactionHistoryRow>,
    isLoading: Boolean,
    onTransactionClick: (TransactionHistoryRow) -> Unit,
    onVoidClick: (TransactionHistoryRow) -> Unit
) {
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
                    color = ThTextMain
                )

                Surface(
                    color = Color(0xFFFFB300),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "${transactions.size} entries",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            HorizontalDivider(color = Color(0xFFF0F0F0), thickness = 1.dp)

            when {
                isLoading -> {
                    EmptyText("Loading transactions...")
                }

                transactions.isEmpty() -> {
                    EmptyText("No transactions found")
                }

                else -> {
                    transactions.forEachIndexed { index, transaction ->
                        TransactionListItem(
                            transaction = transaction,
                            onClick = { onTransactionClick(transaction) },
                            onVoidClick = { onVoidClick(transaction) }
                        )

                        if (index < transactions.size - 1) {
                            HorizontalDivider(color = Color(0xFFF0F0F0), thickness = 0.5.dp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyText(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = ThTextSub)
    }
}

@Composable
private fun TransactionListItem(
    transaction: TransactionHistoryRow,
    onClick: () -> Unit,
    onVoidClick: () -> Unit
) {
    val isVoid = transaction.status.equals("void", ignoreCase = true)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = transaction.displayId,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = ThTextMain
            )

            Text(
                text = if (isVoid) "Void" else "Completed",
                fontSize = 12.sp,
                color = if (isVoid) ThRed else ThTextSub
            )

            Text(
                text = transaction.staffName,
                fontSize = 12.sp,
                color = ThTextSub
            )

            Text(
                text = "B${transaction.branchId}",
                fontSize = 11.sp,
                color = ThTextSub
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "₱${String.format(Locale.US, "%,.2f", transaction.totalAmount)}",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = ThOrange
            )

            if (!isVoid) {
                Surface(
                    color = ThRed,
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
                text = formatDateTime(transaction.dateTime),
                fontSize = 11.sp,
                color = ThTextSub,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun TransactionDetailsDialog(
    transaction: TransactionHistoryRow,
    onDismiss: () -> Unit,
    onVoidClick: () -> Unit
) {
    val isVoid = transaction.status.equals("void", ignoreCase = true)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = transaction.displayId,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = ThTextMain
                )

                Spacer(modifier = Modifier.height(20.dp))

                transaction.items.forEachIndexed { index, item ->
                    DetailItem("Product ${index + 1}", item.productName)

                    if (item.sizeName.isNotBlank()) {
                        DetailItem("Size", item.sizeName)
                    }

                    if (item.addons.isNotEmpty()) {
                        DetailItem("Add-ons", item.addons.joinToString(", "))
                    }

                    DetailItem("Qty", item.quantity.toString())
                    DetailItem("Subtotal", "₱${String.format(Locale.US, "%,.2f", item.subtotal)}", isAmount = true)

                    if (index < transaction.items.size - 1) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                DetailItem("Total Amount", "₱${String.format(Locale.US, "%,.2f", transaction.totalAmount)}", isAmount = true)
                DetailItem("Payment Method", transaction.paymentType)
                DetailItem("Date & Time", formatFullDateTime(transaction.dateTime))
                DetailItem("Processed by", transaction.staffName)
                DetailItem("Branch", "B${transaction.branchId}")
                DetailItem("Status", if (isVoid) "Void" else "Completed")

                Spacer(modifier = Modifier.height(24.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE2E8F0)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Close", color = Color.Black, fontWeight = FontWeight.Bold)
                    }

                    if (!isVoid) {
                        Button(
                            onClick = onVoidClick,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ThRed),
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
private fun DetailItem(
    label: String,
    value: String,
    isAmount: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 14.sp, color = Color(0xFF64748B))

        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = if (isAmount) ThOrange else Color(0xFF1E293B)
        )
    }
}

@Composable
private fun VoidTransactionDialog(
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    Dialog(onDismissRequest = onCancel) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
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
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = ThRed,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Void Transaction",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF1E293B)
                )

                Text(
                    text = "Are you sure you want to\nvoid this transaction?",
                    textAlign = TextAlign.Center,
                    color = Color(0xFF64748B),
                    modifier = Modifier.padding(top = 8.dp),
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(32.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onCancel,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6B8E6B)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onConfirm,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
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

private fun formatDateTime(timestamp: Long): String {
    return SimpleDateFormat("MM/dd/yyyy hh:mm a", Locale.US).format(Date(timestamp))
}

private fun formatFullDateTime(timestamp: Long): String {
    return SimpleDateFormat("MMMM dd, yyyy - hh:mm a", Locale.US).format(Date(timestamp))
}

private fun isSameDay(firstMillis: Long, secondMillis: Long): Boolean {
    val first = Calendar.getInstance().apply {
        timeInMillis = firstMillis
    }

    val second = Calendar.getInstance().apply {
        timeInMillis = secondMillis
    }

    return first.get(Calendar.YEAR) == second.get(Calendar.YEAR) &&
            first.get(Calendar.DAY_OF_YEAR) == second.get(Calendar.DAY_OF_YEAR)
}