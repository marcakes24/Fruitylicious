package com.example.fruitylicious.ui.shared.waste

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.fruitylicious.ui.shared.AdminSideBarContent
import com.example.fruitylicious.ui.shared.SharedDrawerContent
import com.example.fruitylicious.ui.shared.SharedScreenMode
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val WsGreen = Color(0xFF2E7D32)
private val WsPageBg = Color(0xFFFFEAA0)
private val WsTextMain = Color(0xFF1E293B)
private val WsTextSub = Color(0xFF64748B)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WasteManagementScreen(
    navController: NavController,
    mode: SharedScreenMode = SharedScreenMode.ADMIN,
    userName: String = "User",
    branchName: String = "",
    onLogout: () -> Unit = {},
    viewModel: WasteManagementViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var showWasteEntry by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedBranch by remember(uiState.isAdmin, uiState.userBranchId) { 
        mutableStateOf(if (uiState.isAdmin) "All" else uiState.userBranchId) 
    }
    var selectedDateMillis by remember { mutableLongStateOf(0L) }

    val datePickerState = rememberDatePickerState()

    val selectedDateText = remember(selectedDateMillis) {
        if (selectedDateMillis == 0L) {
            ""
        } else {
            SimpleDateFormat("MM/dd/yyyy", Locale.US).format(Date(selectedDateMillis))
        }
    }

    val filteredHistory = uiState.history.filter { item ->
        val matchesSearch = item.ingredientName.contains(searchQuery, ignoreCase = true)

        val matchesBranch = when (selectedBranch) {
            "B1" -> item.branchId == 1
            "B2" -> item.branchId == 2
            else -> true
        }

        val matchesDate = if (selectedDateMillis == 0L) {
            true
        } else {
            isSameDay(item.dateTime, selectedDateMillis)
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
                .background(WsPageBg)
        ) {
            Header(
                selectedBranch = selectedBranch,
                isAdmin = uiState.isAdmin,
                onBranchSelect = { selectedBranch = it },
                onMenuClick = {
                    scope.launch { drawerState.open() }
                }
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Button(
                        onClick = {
                            showWasteEntry = true
                            viewModel.clearMessages()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .shadow(2.dp, RoundedCornerShape(12.dp)),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = WsGreen)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Waste Entry",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

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
                            color = WsGreen,
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
                    WasteHistoryCard(
                        items = filteredHistory
                    )
                }
            }
        }
    }

    if (showWasteEntry) {
        WasteEntryDialog(
            ingredients = uiState.ingredients.filter {
                when (selectedBranch) {
                    "B1" -> it.branchId == 1
                    "B2" -> it.branchId == 2
                    else -> it.branchId == 1
                }
            },
            onDismiss = { showWasteEntry = false },
            onSubmit = { ingredient, quantity, reason ->
                viewModel.submitWaste(
                    ingredient = ingredient,
                    quantityText = quantity,
                    reason = reason
                )
                showWasteEntry = false
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
            .background(WsGreen)
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
                text = "WASTE MANAGEMENT",
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
                                .background(if (isSelected) WsGreen else Color.Transparent)
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
                    Text("Search ingredient", color = Color.LightGray, fontSize = 14.sp)
                },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = Color.LightGray)
                },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color(0xFFEEEEEE),
                    focusedBorderColor = WsGreen
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
                        Text("mm/dd/yyyy", color = Color.LightGray, fontSize = 14.sp)
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
                        focusedBorderColor = WsGreen
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
private fun WasteHistoryCard(
    items: List<WasteHistoryRow>
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
                    text = "Waste History",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF333333)
                )

                Surface(
                    color = Color(0xFFFFB300),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "${items.size} entries",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            HorizontalDivider(color = Color(0xFFF0F0F0), thickness = 1.dp)

            if (items.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No records found", color = Color.Gray, fontSize = 14.sp)
                }
            } else {
                items.forEachIndexed { index, entry ->
                    WasteRecordRow(entry)

                    if (index < items.size - 1) {
                        HorizontalDivider(color = Color(0xFFF0F0F0), thickness = 0.5.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun WasteRecordRow(entry: WasteHistoryRow) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(52.dp),
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFFF8FAFC)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = null,
                    tint = Color(0xFFCBD5E1),
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = entry.ingredientName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = WsTextMain
                )

                Text(
                    text = "- ${formatQuantity(entry.quantity)} ${entry.unitType}",
                    color = WsTextSub,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Branch ${entry.branchId}",
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8)
                )

                Text(
                    text = formatDateTime(entry.dateTime),
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8)
                )
            }

            if (entry.reason.isNotBlank()) {
                Text(
                    text = entry.reason,
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
private fun WasteEntryDialog(
    ingredients: List<WasteIngredientRow>,
    onDismiss: () -> Unit,
    onSubmit: (WasteIngredientRow, String, String) -> Unit
) {
    var selectedIngredient by remember { mutableStateOf<WasteIngredientRow?>(null) }
    var quantity by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    var dropdownExpanded by remember { mutableStateOf(false) }

    val isFormValid = selectedIngredient != null && quantity.isNotBlank()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Waste Entry", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = WsTextMain)
                        Text("Record waste of an ingredient", fontSize = 13.sp, color = WsTextSub)
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("Ingredient", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF334155))
                Spacer(modifier = Modifier.height(8.dp))

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedIngredient?.ingredientName ?: "",
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text("Choose an ingredient", color = Color.LightGray)
                        },
                        trailingIcon = {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = Color(0xFFCBD5E1),
                            focusedBorderColor = WsGreen
                        )
                    )

                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { dropdownExpanded = true }
                    )

                    DropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false },
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .background(Color.White)
                            .heightIn(max = 400.dp)
                    ) {
                        ingredients.forEach { ingredient ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(ingredient.ingredientName, fontSize = 14.sp, color = Color(0xFF334155))
                                        Text(
                                            "${formatQuantity(ingredient.currentStock)} ${ingredient.unitType}",
                                            fontSize = 12.sp,
                                            color = Color.Gray
                                        )
                                    }
                                },
                                onClick = {
                                    selectedIngredient = ingredient
                                    dropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Quantity ${selectedIngredient?.unitType?.let { "($it)" } ?: ""}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF334155)
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = quantity,
                    onValueChange = {
                        if (it.all { char -> char.isDigit() || char == '.' }) {
                            quantity = it
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text("Enter quantity", color = Color.LightGray)
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = Color(0xFFCBD5E1),
                        focusedBorderColor = WsGreen
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color(0xFFE2E8F0))
                Spacer(modifier = Modifier.height(16.dp))

                Text("Reason", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF334155))
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    placeholder = {
                        Text("Add reason", color = Color.LightGray)
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = Color(0xFFCBD5E1),
                        focusedBorderColor = WsGreen
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        val ingredient = selectedIngredient
                        if (ingredient != null) {
                            onSubmit(ingredient, quantity, reason)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .shadow(2.dp, RoundedCornerShape(12.dp)),
                    enabled = isFormValid,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WsGreen,
                        disabledContainerColor = Color(0xFFCBD5E1)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = if (isFormValid) Color.White else Color(0xFF94A3B8)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "Submit Waste",
                        color = if (isFormValid) Color.White else Color(0xFF94A3B8),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private fun formatQuantity(value: Double): String {
    return if (value % 1.0 == 0.0) {
        value.toInt().toString()
    } else {
        String.format(Locale.US, "%.2f", value)
    }
}

private fun formatDateTime(timestamp: Long): String {
    return SimpleDateFormat("MM/dd/yyyy hh:mm a", Locale.US).format(Date(timestamp))
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