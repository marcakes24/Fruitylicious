package com.example.fruitylicious.ui.shared.inventory

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Remove
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.fruitylicious.data.local.entity.BranchEntity
import com.example.fruitylicious.ui.shared.SharedDrawerContent
import com.example.fruitylicious.ui.shared.SharedScreenMode
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val IaGreen = Color(0xFF2E7D32)
private val IaPageBg = Color(0xFFFFEAA0)
private val IaTextMain = Color(0xFF1E293B)
private val IaRed = Color(0xFFB91C1C)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryAdjustmentScreen(
    navController: NavController,
    mode: SharedScreenMode = SharedScreenMode.ADMIN,
    userName: String = "User",
    branchName: String = "",
    onLogout: () -> Unit = {},
    viewModel: InventoryAdjustmentViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var showEntryDialog by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    val selectedDateText = remember(uiState.startDate) {
        if (uiState.startDate == null) "" else SimpleDateFormat("MM/dd/yyyy", Locale.US).format(Date(uiState.startDate!!))
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.setDateRange(datePickerState.selectedDateMillis, datePickerState.selectedDateMillis)
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.setDateRange(null, null)
                    showDatePicker = false
                }) { Text("Clear") }
            }
        ) { DatePicker(state = datePickerState) }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(drawerContainerColor = Color.Transparent, drawerTonalElevation = 0.dp) {
                SharedDrawerContent(mode, navController, drawerState, scope, userName, branchName, onLogout)
            }
        }
    ) {
        Column(modifier = Modifier.fillMaxSize().background(IaPageBg)) {
            Header(
                selectedBranchId = uiState.selectedBranchId,
                branches = uiState.branches,
                isAdmin = uiState.isAdmin,
                isOnline = uiState.isOnline,
                localBranchId = uiState.localBranchId, 
                onBranchSelect = { viewModel.selectBranch(it) },
                onMenuClick = { scope.launch { drawerState.open() } }
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 20.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Button(
                        onClick = { showEntryDialog = true; viewModel.clearMessages() },
                        modifier = Modifier.fillMaxWidth().height(56.dp).shadow(2.dp, RoundedCornerShape(12.dp)),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = IaGreen)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("New Adjustment", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }

                item {
                    FilterCard(
                        searchQuery = uiState.searchQuery,
                        onSearchChange = { viewModel.setSearchQuery(it) },
                        selectedDateText = selectedDateText,
                        onDateClick = { showDatePicker = true }
                    )
                }

                if (!uiState.error.isNullOrBlank()) {
                    item { Text(text = uiState.error!!, color = MaterialTheme.colorScheme.error, fontSize = 13.sp) }
                }
                if (!uiState.successMessage.isNullOrBlank()) {
                    item { Text(text = uiState.successMessage!!, color = IaGreen, fontSize = 13.sp) }
                }

                item { RecentAdjustmentsCard(items = uiState.history) }
            }
        }
    }

    if (showEntryDialog) {
        AdjustmentEntryDialog(
            ingredients = uiState.ingredients,
            onDismiss = { showEntryDialog = false },
            onSubmit = { ing, type, qty, reason ->
                viewModel.submitAdjustment(ing, type, qty, reason)
                showEntryDialog = false
            }
        )
    }
}

@Composable
private fun Header(
    selectedBranchId: Int?,
    branches: List<BranchEntity>,
    isAdmin: Boolean,
    isOnline: Boolean,
    localBranchId: Int,
    onBranchSelect: (Int?) -> Unit,
    onMenuClick: () -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth().background(IaGreen).padding(top = 40.dp, bottom = 12.dp, start = 16.dp, end = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onMenuClick) { Icon(Icons.Default.Menu, "Menu", tint = Color.White) }
            Text("ADJUSTMENTS", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            if (isAdmin) {
                Row(modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(Color(0xFFF5F5F5)).padding(4.dp)) {
                    // if (isOnline) {
                        BranchTab("All", selectedBranchId == null) { onBranchSelect(null) }
                        branches.forEach { BranchTab("B${it.branchId}", selectedBranchId == it.branchId) { onBranchSelect(it.branchId) } }
                    /* } else {
                        BranchTab("B$localBranchId", true) { onBranchSelect(localBranchId) }
                    } */
                }
            }
        }
    }
}

@Composable
private fun BranchTab(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(if (selected) IaGreen else Color.Transparent)
        .clickable { onClick() }.padding(horizontal = 12.dp, vertical = 6.dp), contentAlignment = Alignment.Center) {
        Text(label, color = if (selected) Color.White else Color(0xFF666E7A), fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun FilterCard(
    searchQuery: String, 
    onSearchChange: (String) -> Unit,
    selectedDateText: String, 
    onDateClick: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), color = Color.White, shadowElevation = 2.dp) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = searchQuery, 
                onValueChange = onSearchChange, 
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search ingredient, reason, or user", color = Color.LightGray, fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.LightGray) },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color(0xFFEEEEEE),
                    focusedBorderColor = IaGreen
                ),
                singleLine = true
            )
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = selectedDateText, 
                    onValueChange = {}, 
                    readOnly = true, 
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("mm/dd/yyyy", color = Color.LightGray, fontSize = 14.sp) },
                    leadingIcon = { Icon(Icons.Default.DateRange, null, tint = Color.LightGray) },
                    trailingIcon = { Icon(Icons.Default.CalendarMonth, null, tint = Color.Black) },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = Color(0xFFEEEEEE),
                        focusedBorderColor = IaGreen
                    )
                )
                Box(modifier = Modifier.matchParentSize().clickable { onDateClick() })
            }
        }
    }
}

@Composable
fun AdjustmentEntryDialog(
    ingredients: List<AdjustmentIngredientRow>,
    onDismiss: () -> Unit,
    onSubmit: (AdjustmentIngredientRow, String, String, String) -> Unit
) {
    var selectedIngredient by remember { mutableStateOf<AdjustmentIngredientRow?>(null) }
    var adjustmentType by remember { mutableStateOf("Add") }
    var quantity by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var dropdownWidth by remember { mutableStateOf(0.dp) }
    val density = LocalDensity.current
    val isFormValid = selectedIngredient != null && quantity.isNotBlank()

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(24.dp), color = Color.White, modifier = Modifier.fillMaxWidth().padding(8.dp)) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()), 
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("New Adjustment", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = IaTextMain)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                    }
                }
                
                Column {
                    Label("Select Ingredient")
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = selectedIngredient?.ingredientName ?: "", onValueChange = {}, readOnly = true,
                            modifier = Modifier.fillMaxWidth().onGloballyPositioned { dropdownWidth = with(density) { it.size.width.toDp() } },
                            placeholder = { Text("Choose an ingredient", color = Color.LightGray) },
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) }, 
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = Color(0xFFE2E8F0),
                                focusedBorderColor = IaGreen
                            )
                        )
                        Box(modifier = Modifier.matchParentSize().clickable { expanded = true })
                        DropdownMenu(expanded, { expanded = false }, modifier = Modifier.width(dropdownWidth).heightIn(max = 400.dp)) {
                            ingredients.forEach { ing ->
                                DropdownMenuItem(
                                    text = { Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                                        Text(ing.ingredientName, fontSize = 14.sp, color = IaTextMain)
                                        Text("${formatQuantity(ing.currentStock)} ${ing.unitType}", fontSize = 12.sp, color = Color.Gray)
                                    }},
                                    onClick = { selectedIngredient = ing; expanded = false }
                                )
                            }
                        }
                    }
                }

                if (selectedIngredient != null) {
                    CurrentStockCard(selectedIngredient!!)
                }

                Column {
                    Label("Adjustment Type")
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        AdjustmentTypeButton(
                            label = "Add Stock",
                            iconAdd = true,
                            selected = adjustmentType == "Add",
                            selectedColor = IaGreen,
                            onClick = { adjustmentType = "Add" },
                            modifier = Modifier.weight(1f)
                        )
                        AdjustmentTypeButton(
                            label = "Reduce Stock",
                            iconAdd = false,
                            selected = adjustmentType == "Reduce",
                            selectedColor = IaRed,
                            onClick = { adjustmentType = "Reduce" },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Column {
                    Label("Quantity ${selectedIngredient?.unitType?.let { "($it)" } ?: ""}")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = quantity, onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) quantity = it },
                        modifier = Modifier.fillMaxWidth(), 
                        placeholder = { Text("Enter quantity", color = Color.LightGray) },
                        shape = RoundedCornerShape(12.dp), 
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = Color(0xFFE2E8F0),
                            focusedBorderColor = IaGreen
                        ),
                        singleLine = true
                    )
                }

                Column {
                    Label("Reason")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = reason, onValueChange = { reason = it }, 
                        modifier = Modifier.fillMaxWidth().height(90.dp), 
                        placeholder = { Text("Enter reason for adjustment", color = Color.LightGray) },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = Color(0xFFE2E8F0),
                            focusedBorderColor = IaGreen
                        )
                    )
                }

                Button(
                    onClick = { selectedIngredient?.let { onSubmit(it, adjustmentType, quantity, reason) } },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    enabled = isFormValid,
                    shape = RoundedCornerShape(12.dp), 
                    colors = ButtonDefaults.buttonColors(
                        containerColor = IaGreen,
                        disabledContainerColor = Color(0xFFCBD5E1)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = if (isFormValid) Color.White else Color(0xFF94A3B8),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Submit Adjustment", 
                        color = if (isFormValid) Color.White else Color(0xFF94A3B8),
                        fontSize = 15.sp, 
                        fontWeight = FontWeight.Bold
                    ) 
                }
            }
        }
    }
}

@Composable
private fun Label(text: String) {
    Text(
        text = text,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color(0xFF475569)
    )
}

@Composable
private fun CurrentStockCard(ingredient: AdjustmentIngredientRow) {
    Surface(modifier = Modifier.fillMaxWidth(), color = Color(0xFFFFFBEB), shape = RoundedCornerShape(12.dp)) {
        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(text = "Current Stock", color = Color(0xFF92400E), fontSize = 14.sp)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(text = formatQuantity(ingredient.currentStock), fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color.Black)
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = ingredient.unitType, fontSize = 14.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 2.dp))
            }
        }
    }
}

@Composable
private fun AdjustmentTypeButton(
    label: String,
    iconAdd: Boolean,
    selected: Boolean,
    selectedColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.height(60.dp).clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = if (selected) selectedColor.copy(alpha = 0.08f) else Color.White,
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) selectedColor else Color(0xFFE2E8F0))
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Icon(
                imageVector = if (iconAdd) Icons.Default.Add else Icons.Default.Remove,
                contentDescription = null,
                tint = if (selected) selectedColor else Color.Gray,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label, textAlign = TextAlign.Center, fontSize = 13.sp, 
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) selectedColor else Color.Gray, lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun RecentAdjustmentsCard(items: List<AdjustmentHistoryRow>) {
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), color = Color.White, shadowElevation = 2.dp) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Adjustment History", fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))
            HorizontalDivider(color = Color(0xFFF1F5F9))
            if (items.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(24.dp), Alignment.Center) { Text("No records found", color = Color.Gray) }
            } else {
                items.forEachIndexed { index, item ->
                    AdjustmentRow(item)
                    if (index < items.size - 1) HorizontalDivider(color = Color(0xFFF1F5F9))
                }
            }
        }
    }
}

@Composable
private fun AdjustmentRow(item: AdjustmentHistoryRow) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Column(Modifier.weight(1f)) {
            Text(item.ingredientName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = IaTextMain)
            Text(item.reason, fontSize = 12.sp, color = Color.Gray)
            Text("By: ${item.userName} • ${item.branchName}", fontSize = 11.sp, color = Color(0xFF94A3B8))
        }
        Column(horizontalAlignment = Alignment.End) {
            val isAdd = item.adjustmentType == "Add"
            Surface(color = if (isAdd) IaGreen else IaRed, shape = RoundedCornerShape(6.dp)) {
                Text("${if (isAdd) "+" else "-"}${formatQuantity(item.quantity)}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
            }
            Spacer(Modifier.height(4.dp))
            Text(SimpleDateFormat("M/d/yy h:mm a", Locale.US).format(Date(item.dateTime)), fontSize = 11.sp, color = Color.Gray)
        }
    }
}

private fun formatQuantity(v: Double): String = if (v % 1.0 == 0.0) v.toInt().toString() else "%.2f".format(Locale.US, v)
