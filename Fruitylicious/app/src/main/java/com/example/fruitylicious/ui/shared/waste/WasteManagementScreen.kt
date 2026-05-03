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
import androidx.compose.material.icons.filled.History
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.fruitylicious.data.local.entity.BranchEntity
import com.example.fruitylicious.ui.shared.SharedDrawerContent
import com.example.fruitylicious.ui.shared.SharedScreenMode
import com.example.fruitylicious.util.DateTimeUtil
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import com.example.fruitylicious.util.ImageStorage

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
    var selectedDateMillis by remember { mutableLongStateOf(0L) }
    var expandedImageFile by remember { mutableStateOf<File?>(null) }

    val datePickerState = rememberDatePickerState()

    val dateFormatter = remember { SimpleDateFormat("MM/dd/yyyy", Locale.US) }
    val selectedDateText = remember(selectedDateMillis) {
        if (selectedDateMillis == 0L) "" else dateFormatter.format(Date(selectedDateMillis))
    }

    val filteredHistory = remember(uiState.history, searchQuery, selectedDateMillis) {
        val query = searchQuery.trim()
        val hasDateFilter = selectedDateMillis != 0L
        
        val range = if (hasDateFilter) {
            val cal = Calendar.getInstance().apply {
                timeInMillis = selectedDateMillis
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val start = cal.timeInMillis
            cal.add(Calendar.DAY_OF_YEAR, 1)
            start until cal.timeInMillis
        } else null

        uiState.history.filter { item ->
            val matchesSearch = query.isEmpty() ||
                    item.ingredientName.contains(query, ignoreCase = true) ||
                    item.reason.contains(query, ignoreCase = true) ||
                    item.branchName.contains(query, ignoreCase = true)

            val matchesDate = range?.let { item.dateTime in it } ?: true

            matchesSearch && matchesDate
        }
    }

    val onBranchSelect = remember(viewModel) { { id: Int? -> viewModel.selectBranch(id) } }
    val onMenuClick = remember(scope, drawerState) { { scope.launch { drawerState.open() }; Unit } }
    val onEntryClick = {
        if (uiState.isClockedIn) {
            showWasteEntry = true
            viewModel.clearMessages()
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = {
                showDatePicker = false
            },
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
                selectedBranchId = uiState.selectedBranchId,
                branches = uiState.branches,
                isAdmin = uiState.isAdmin,
                isOnline = uiState.isOnline,
                localBranchId = uiState.localBranchId,
                onBranchSelect = onBranchSelect,
                onMenuClick = onMenuClick
            )

            if (uiState.isAdmin && !uiState.isOnline) {
                Text(
                    text = "Offline mode: showing local branch waste history only.",
                    color = WsTextSub,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    val containerColor = if (uiState.isClockedIn) WsGreen else Color.LightGray
                    Button(
                        onClick = onEntryClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .shadow(2.dp, RoundedCornerShape(12.dp)),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = containerColor)
                    ) {
                        Icon(
                            imageVector = if (uiState.isClockedIn) Icons.Default.Add else Icons.Default.History,
                            contentDescription = null,
                            tint = Color.White
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = if (uiState.isClockedIn) "Waste Entry" else "Clock in required",
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
                        onSearchChange = {
                            searchQuery = it
                        },
                        selectedDateText = selectedDateText,
                        onDateClick = {
                            showDatePicker = true
                        }
                    )
                }

                item {
                    WasteHistoryCard(
                        items = filteredHistory,
                        onImageClick = { file -> expandedImageFile = file }
                    )
                }
            }
        }
    }

    if (expandedImageFile != null) {
        Dialog(onDismissRequest = { expandedImageFile = null }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color.Black
            ) {
                Box(contentAlignment = Alignment.TopEnd) {
                    AsyncImage(
                        model = expandedImageFile,
                        contentDescription = "Expanded waste image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 600.dp),
                        contentScale = androidx.compose.ui.layout.ContentScale.Fit
                    )
                    
                    IconButton(
                        onClick = { expandedImageFile = null },
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White,
                            modifier = Modifier.background(
                                Color.Black.copy(alpha = 0.4f),
                                RoundedCornerShape(20.dp)
                            )
                        )
                    }
                }
            }
        }
    }

    if (showWasteEntry) {
        WasteEntryDialog(
            ingredients = uiState.ingredients,
            onDismiss = {
                showWasteEntry = false
            },
            onSubmit = { ingredient, quantity, reason, imagePath ->
                viewModel.submitWaste(
                    ingredient = ingredient,
                    quantityText = quantity,
                    reason = reason,
                    imagePath = imagePath
                )
                showWasteEntry = false
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
            .background(WsGreen)
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
                    if (isOnline) {
                        WasteBranchButton(
                            label = "All",
                            selected = selectedBranchId == null,
                            onClick = {
                                onBranchSelect(null)
                            }
                        )

                        branches.forEach { branch ->
                            WasteBranchButton(
                                label = "B${branch.branchId}",
                                selected = selectedBranchId == branch.branchId,
                                onClick = {
                                    onBranchSelect(branch.branchId)
                                }
                            )
                        }
                    } else {
                        WasteBranchButton(
                            label = "B$localBranchId",
                            selected = true,
                            onClick = {
                                onBranchSelect(localBranchId)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WasteBranchButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) WsGreen else Color.Transparent)
            .clickable {
                onClick()
            }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) Color.White else Color(0xFF666E7A),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
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
                        text = "Search ingredient, reason, or branch",
                        color = Color.LightGray,
                        fontSize = 14.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = Color.LightGray
                    )
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
                        Text(
                            text = "mm/dd/yyyy",
                            color = Color.LightGray,
                            fontSize = 14.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = null,
                            tint = Color.LightGray
                        )
                    },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = Color.Black
                        )
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
                        .clickable {
                            onDateClick()
                        }
                )
            }
        }
    }
}

@Composable
private fun WasteHistoryCard(
    items: List<WasteHistoryRow>,
    onImageClick: (File) -> Unit
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
                    Text(
                        text = "No records found",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
            } else {
                items.forEachIndexed { index, entry ->
                    key(entry.wasteId) {
                        WasteRecordRow(
                            entry = entry,
                            onImageClick = onImageClick
                        )

                        if (index < items.size - 1) {
                            HorizontalDivider(color = Color(0xFFF0F0F0), thickness = 0.5.dp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WasteRecordRow(
    entry: WasteHistoryRow,
    onImageClick: (File) -> Unit
) {
    val context = LocalContext.current
    val imageFile = entry.imagePath?.let {
        ImageStorage.getImageFile(context, it)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier
                .size(52.dp)
                .clickable(enabled = imageFile != null && imageFile.exists()) {
                    imageFile?.let { onImageClick(it) }
                },
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFFF8FAFC)
        ) {
            if (imageFile != null && imageFile.exists()) {
                AsyncImage(
                    model = imageFile,
                    contentDescription = "Waste image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            } else {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = null,
                        tint = Color(0xFFCBD5E1),
                        modifier = Modifier.size(24.dp)
                    )
                }
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
                    text = entry.branchName.ifBlank {
                        "Branch ${entry.branchId}"
                    },
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8)
                )

                Text(
                    text = DateTimeUtil.formatDateTime(entry.dateTime),
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
    onSubmit: (WasteIngredientRow, String, String, String) -> Unit
) {
    var selectedIngredient by remember { mutableStateOf<WasteIngredientRow?>(null) }
    var quantity by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    var dropdownExpanded by remember { mutableStateOf(false) }
    var dropdownWidth by remember { mutableStateOf(0.dp) }
    val context = LocalContext.current
    val density = LocalDensity.current

    var selectedImageUri by remember {
        mutableStateOf<Uri?>(null)
    }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        selectedImageUri = uri
    }

    val isFormValid by remember { 
        derivedStateOf { 
            selectedIngredient != null && 
            quantity.isNotBlank() && 
            selectedImageUri != null 
        } 
    }

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
                            text = "Waste Entry",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = WsTextMain
                        )

                        Text(
                            text = "Record waste of an ingredient",
                            fontSize = 13.sp,
                            color = WsTextSub
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

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Ingredient",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF334155)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedIngredient?.ingredientName ?: "",
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { coordinates ->
                                dropdownWidth = with(density) { coordinates.size.width.toDp() }
                            },
                        placeholder = {
                            Text(
                                text = "Choose an ingredient",
                                color = Color.LightGray
                            )
                        },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null
                            )
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
                            .clickable {
                                dropdownExpanded = true
                            }
                    )

                    DropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = {
                            dropdownExpanded = false
                        },
                        modifier = Modifier
                            .width(dropdownWidth)
                            .background(Color.White)
                            .heightIn(max = 400.dp)
                    ) {
                        ingredients.forEach { ingredient ->
                            key(ingredient.ingredientId) {
                                DropdownMenuItem(
                                    text = {
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
                                    },
                                    onClick = {
                                        selectedIngredient = ingredient
                                        dropdownExpanded = false
                                    }
                                )
                            }
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
                        Text(
                            text = "Enter quantity",
                            color = Color.LightGray
                        )
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

                Text(
                    text = "Reason",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF334155)
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = reason,
                    onValueChange = {
                        reason = it
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    placeholder = {
                        Text(
                            text = "Add reason",
                            color = Color.LightGray
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = Color(0xFFCBD5E1),
                        focusedBorderColor = WsGreen
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Image",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF334155)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clickable {
                            imagePicker.launch("image/*")
                        },
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF8FAFC),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = Color(0xFFCBD5E1)
                    )
                ) {
                    if (selectedImageUri != null) {
                        AsyncImage(
                            model = selectedImageUri,
                            contentDescription = "Selected waste image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Tap to choose image",
                                color = Color.Gray,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        val ingredient = selectedIngredient
                        val imageUri = selectedImageUri

                        if (ingredient != null && imageUri != null) {
                            val imagePath = ImageStorage.saveImageFromUri(
                                context = context,
                                sourceUri = imageUri,
                                folder = "waste"
                            )

                            onSubmit(
                                ingredient,
                                quantity,
                                reason,
                                imagePath
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