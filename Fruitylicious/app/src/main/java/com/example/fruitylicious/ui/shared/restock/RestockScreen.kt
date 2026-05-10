package com.example.fruitylicious.ui.shared.restock

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.fruitylicious.data.local.entity.BranchEntity
import com.example.fruitylicious.ui.shared.BranchSelector
import com.example.fruitylicious.ui.shared.ErrorWarning
import com.example.fruitylicious.ui.shared.FruityDateFilterField
import com.example.fruitylicious.ui.shared.FruityDatePicker
import com.example.fruitylicious.ui.shared.FruitySearchField
import com.example.fruitylicious.ui.shared.FruitySearchableDropdown
import com.example.fruitylicious.ui.shared.SharedDrawerContent
import com.example.fruitylicious.ui.shared.SharedScreenMode
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

private val RsGreen = Color(0xFF2E7D32)
private val RsPageBg = Color(0xFFFFEAA0)
private val RsTextMain = Color(0xFF1E293B)
private val RsTextSub = Color(0xFF64748B)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestockScreen(
    navController: NavController,
    mode: SharedScreenMode = SharedScreenMode.OWNER,
    userName: String = "User",
    branchName: String = "",
    onLogout: () -> Unit = {},
    viewModel: RestockViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var showRestockEntry by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedDateMillis by remember { mutableLongStateOf(0L) }

    val datePickerState = rememberDatePickerState()

    val selectedDateText = remember(selectedDateMillis) {
        if (selectedDateMillis == 0L) {
            ""
        } else {
            SimpleDateFormat("M/d/yyyy", Locale.US).format(Date(selectedDateMillis))
        }
    }

    val filteredHistory = remember(
        uiState.history,
        searchQuery,
        selectedDateMillis
    ) {
        uiState.history.filter { item ->
            val matchesSearch =
                item.ingredientName.contains(searchQuery, ignoreCase = true) ||
                        item.supplier.contains(searchQuery, ignoreCase = true) ||
                        item.branchName.contains(searchQuery, ignoreCase = true)

            val matchesDate = if (selectedDateMillis == 0L) {
                true
            } else {
                isSameDay(item.dateTime, selectedDateMillis)
            }

            matchesSearch && matchesDate
        }
    }

    if (showDatePicker) {
        FruityDatePicker(
            state = datePickerState,
            onDismiss = { showDatePicker = false },
            onConfirm = { millis ->
                selectedDateMillis = millis ?: 0L
            },
            onClear = {
                selectedDateMillis = 0L
            }
        )
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
                    isClockedIn = uiState.isClockedIn,
                    onLogout = onLogout
                )
            }
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(RsPageBg)
            ) {
                Header(
                    selectedBranchId = uiState.selectedBranchId,
                    branches = uiState.branches,
                    isAdmin = uiState.isAdmin,
                    isOnline = uiState.isOnline,
                    localBranchId = uiState.localBranchId,
                    onBranchSelect = { branchId ->
                        viewModel.onBranchSelected(branchId)
                    },
                    onMenuClick = {
                        scope.launch {
                            drawerState.open()
                        }
                    }
                )

                ErrorWarning(message = uiState.error)

                if (uiState.isAdmin && !uiState.isOnline) {
                    Text(
                        text = "Offline mode: Only local branch restock history is visible.",
                        color = RsTextSub,
                        fontSize = 12.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, top = 4.dp)
                    )
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    item {
                        Button(
                            onClick = {
                                if (uiState.isClockedIn) {
                                    viewModel.clearMessages()
                                    showRestockEntry = true
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .shadow(2.dp, RoundedCornerShape(12.dp)),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (uiState.isClockedIn) {
                                    RsGreen
                                } else {
                                    Color.LightGray
                                }
                            )
                        ) {
                            Icon(
                                imageVector = if (uiState.isClockedIn) {
                                    Icons.Default.Add
                                } else {
                                    Icons.Default.History
                                },
                                contentDescription = null,
                                tint = Color.White
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(
                                text = if (uiState.isClockedIn) {
                                    "Restock Entry"
                                } else {
                                    "Clock in required"
                                },
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (!uiState.successMessage.isNullOrBlank()) {
                        item {
                            Text(
                                text = uiState.successMessage ?: "",
                                color = RsGreen,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }

                    item { Spacer(modifier = Modifier.height(16.dp)) }

                    item {
                        FilterCard(
                            searchQuery = searchQuery,
                            onSearchChange = {
                                searchQuery = it
                            },
                            selectedDateText = selectedDateText,
                            onDateClick = {
                                showDatePicker = true
                            },
                            onDateClear = {
                                selectedDateMillis = 0L
                            }
                        )
                    }

                    item { Spacer(modifier = Modifier.height(16.dp)) }

                    // --- History Card Start ---
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                            color = Color.White,
                            shadowElevation = 2.dp
                        ) {
                            Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.History,
                                            contentDescription = null,
                                            tint = RsTextSub,
                                            modifier = Modifier.size(18.dp)
                                        )

                                        Spacer(modifier = Modifier.width(8.dp))

                                        Text(
                                            text = "Restock History",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF333333)
                                        )
                                    }

                                    Surface(
                                        color = Color(0xFFFFB300),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = "${filteredHistory.size} entries",
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                HorizontalDivider(color = Color(0xFFF0F0F0), thickness = 1.dp)
                            }
                        }
                    }

                    if (uiState.isLoading && filteredHistory.isEmpty()) {
                        item {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
                                color = Color.White,
                                shadowElevation = 2.dp
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    androidx.compose.material3.CircularProgressIndicator(color = RsGreen)
                                }
                            }
                        }
                    } else if (filteredHistory.isEmpty()) {
                        item {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
                                color = Color.White,
                                shadowElevation = 2.dp
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = "No restock records found", color = Color.Gray)
                                }
                            }
                        }
                    } else {
                        itemsIndexed(
                            items = filteredHistory,
                            key = { _, entry -> entry.restockId }
                        ) { index, entry ->
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = Color.White,
                                shadowElevation = 2.dp,
                                shape = if (index == filteredHistory.lastIndex && !uiState.hasMore) {
                                    RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
                                } else {
                                    RoundedCornerShape(0.dp)
                                }
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                                    RestockRecordRow(entry)
                                    if (index < filteredHistory.size - 1 || uiState.hasMore) {
                                        HorizontalDivider(color = Color(0xFFF0F0F0), thickness = 0.5.dp)
                                    }
                                }
                            }
                        }

                        if (uiState.hasMore) {
                            item {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
                                    color = Color.White,
                                    shadowElevation = 2.dp
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Button(
                                            onClick = { viewModel.loadMore() },
                                            modifier = Modifier.fillMaxWidth(),
                                            enabled = !uiState.isLoadingMore,
                                            colors = ButtonDefaults.buttonColors(containerColor = RsGreen)
                                        ) {
                                            Text(if (uiState.isLoadingMore) "Loading..." else "Load More")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showRestockEntry) {
        RestockEntryDialog(
            ingredients = uiState.ingredients,
            onDismiss = {
                showRestockEntry = false
            },
            onSubmit = { ingredient, quantity, supplier ->
                viewModel.submitRestock(
                    ingredient = ingredient,
                    quantityText = quantity,
                    supplier = supplier
                )
                showRestockEntry = false
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
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(RsGreen)
            .padding(top = 40.dp, bottom = 12.dp, start = 16.dp, end = 16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = onMenuClick) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Menu",
                    tint = Color.White
                )
            }

            Text(
                text = "RESTOCK",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )

            if (isAdmin) {
                BranchSelector(
                    selectedBranchId = selectedBranchId,
                    branches = branches,
                    isOnline = isOnline,
                    onBranchSelected = onBranchSelect,
                    activeColor = RsGreen,
                    containerColor = Color(0xFFF5F5F5),
                    localBranchId = localBranchId
                )
            }
        }
    }
}

@Composable
private fun FilterCard(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    selectedDateText: String,
    onDateClick: () -> Unit,
    onDateClear: () -> Unit
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
            FruitySearchField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = "Search ingredient, supplier, or branch"
            )

            FruityDateFilterField(
                selectedDateText = selectedDateText,
                onClick = onDateClick,
                onClear = onDateClear
            )
        }
    }
}

@Composable
private fun RestockRecordRow(entry: RestockHistoryRow) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.ingredientName,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = Color(0xFF333333)
            )

            Text(
                text = "Supplier: ${entry.supplier}",
                fontSize = 12.sp,
                color = Color.Gray
            )

            Text(
                text = entry.branchName.ifBlank {
                    "Branch ${entry.branchId}"
                },
                fontSize = 12.sp,
                color = Color.Gray
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Surface(
                color = Color(0xFFF1F5F9),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = "+ ${formatQuantity(entry.quantityAdded)} ${entry.unitType}",
                    color = Color(0xFF475569),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = formatDateTime(entry.dateTime),
                fontSize = 11.sp,
                color = Color.Gray
            )
        }
    }
}

@Composable
private fun RestockEntryDialog(
    ingredients: List<RestockIngredientRow>,
    onDismiss: () -> Unit,
    onSubmit: (RestockIngredientRow, String, String) -> Unit
) {
    var selectedIngredient by remember { mutableStateOf<RestockIngredientRow?>(null) }
    var quantity by remember { mutableStateOf("") }
    var supplier by remember { mutableStateOf("") }
    var dropdownExpanded by remember { mutableStateOf(false) }
    var ingredientSearchQuery by remember { mutableStateOf("") }

    val filteredIngredients = remember(ingredients, ingredientSearchQuery) {
        if (ingredientSearchQuery.isEmpty()) {
            ingredients
        } else {
            ingredients.filter {
                it.ingredientName.contains(ingredientSearchQuery, ignoreCase = true)
            }
        }
    }

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
                        Text(
                            text = "Restock Entry",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = RsTextMain
                        )

                        Text(
                            text = "Add stock for an ingredient",
                            fontSize = 13.sp,
                            color = RsTextSub
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                FruitySearchableDropdown(
                    value = ingredientSearchQuery.ifEmpty { selectedIngredient?.ingredientName ?: "" },
                    onValueChange = {
                        ingredientSearchQuery = it
                        if (it.isEmpty()) selectedIngredient = null
                    },
                    options = filteredIngredients,
                    onOptionClick = {
                        selectedIngredient = it
                        ingredientSearchQuery = it.ingredientName
                    },
                    label = "Ingredient",
                    expanded = dropdownExpanded,
                    onExpandedChange = { dropdownExpanded = it },
                    placeholder = "Choose an ingredient",
                    itemContent = { ingredient ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = ingredient.ingredientName,
                                fontSize = 14.sp,
                                color = Color(0xFF334155)
                            )

                            Text(
                                text = "${formatQuantity(ingredient.currentStock)} ${ingredient.unitType}",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Quantity to Add ${selectedIngredient?.unitType?.let { "($it)" } ?: ""}",
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
                        Text(
                            text = "Enter quantity",
                            color = Color.LightGray
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = Color(0xFFCBD5E1),
                        focusedBorderColor = RsGreen
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Supplier",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF334155)
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = supplier,
                    onValueChange = {
                        supplier = it
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            text = "Supplier name",
                            color = Color.LightGray
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = Color(0xFFCBD5E1),
                        focusedBorderColor = RsGreen
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        val ingredient = selectedIngredient

                        if (ingredient != null) {
                            onSubmit(
                                ingredient,
                                quantity,
                                supplier
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .shadow(2.dp, RoundedCornerShape(12.dp)),
                    enabled = isFormValid,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RsGreen,
                        disabledContainerColor = Color(0xFFCBD5E1)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = if (isFormValid) {
                            Color.White
                        } else {
                            Color(0xFF94A3B8)
                        }
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "Add Stock",
                        color = if (isFormValid) {
                            Color.White
                        } else {
                            Color(0xFF94A3B8)
                        },
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
    return SimpleDateFormat("M/d/yyyy h:mm a", Locale.US).format(Date(timestamp))
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