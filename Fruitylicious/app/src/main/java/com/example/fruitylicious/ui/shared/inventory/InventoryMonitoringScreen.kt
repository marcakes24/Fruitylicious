package com.example.fruitylicious.ui.shared.inventory

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.fruitylicious.data.local.entity.BranchEntity
import com.example.fruitylicious.ui.shared.SharedDrawerContent
import com.example.fruitylicious.ui.shared.SharedScreenMode
import com.example.fruitylicious.util.ImageStorage
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

private val ImGreen = Color(0xFF2E7D32)
private val ImPageBg = Color(0xFFFFEAA0)
private val ImTextMain = Color(0xFF1E293B)
private val ImTextSub = Color.Gray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryMonitoringScreen(
    navController: NavController,
    mode: SharedScreenMode = SharedScreenMode.ADMIN,
    userName: String = "User",
    branchName: String = "",
    onLogout: () -> Unit = {},
    viewModel: InventoryMonitoringViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedDateMillis by remember { mutableLongStateOf(0L) }

    val datePickerState = rememberDatePickerState()

    val selectedDateText = remember(selectedDateMillis) {
        if (selectedDateMillis == 0L) {
            ""
        } else {
            SimpleDateFormat("MM/dd/yyyy", Locale.US).format(Date(selectedDateMillis))
        }
    }

    val filteredRows = remember(
        uiState.rows,
        searchQuery,
        selectedDateMillis
    ) {
        uiState.rows.filter { row ->
            val matchesSearch =
                row.ingredientName.contains(searchQuery, ignoreCase = true) ||
                        row.branchName.contains(searchQuery, ignoreCase = true)

            val matchesDate = if (selectedDateMillis == 0L) {
                true
            } else {
                isSameDay(row.lastModified, selectedDateMillis)
            }

            matchesSearch && matchesDate
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
                .background(ImPageBg)
        ) {
            Header(
                selectedBranchId = uiState.selectedBranchId,
                branches = uiState.branches,
                isAdmin = uiState.isAdmin,
                isOnline = uiState.isOnline,
                localBranchId = uiState.localBranchId,
                onBranchSelect = { branchId ->
                    viewModel.selectBranch(branchId)
                },
                onMenuClick = {
                    scope.launch {
                        drawerState.open()
                    }
                }
            )

            if (uiState.isAdmin && !uiState.isOnline) {
                Text(
                    text = "Offline mode: showing local branch inventory only.",
                    color = ImTextSub,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            if (!uiState.error.isNullOrBlank()) {
                Text(
                    text = uiState.error ?: "",
                    color = Color.Red,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 20.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
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
                    Text(
                        text = "Ingredient List",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF333333),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        shadowElevation = 2.dp
                    ) {
                        Column(modifier = Modifier.padding(vertical = 8.dp)) {
                            when {
                                uiState.isLoading -> {
                                    EmptyInventoryText("Loading inventory...")
                                }

                                filteredRows.isEmpty() -> {
                                    EmptyInventoryText("No ingredients found")
                                }

                                else -> {
                                    filteredRows.forEachIndexed { index, item ->
                                        InventoryListItem(item)

                                        if (index < filteredRows.size - 1) {
                                            HorizontalDivider(
                                                modifier = Modifier.padding(horizontal = 16.dp),
                                                color = Color(0xFFF1F5F9),
                                                thickness = 1.dp
                                            )
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
            .background(ImGreen)
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
                text = "INVENTORY MONITORING",
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
                    // if (isOnline) {
                        InventoryBranchButton(
                            label = "All",
                            selected = selectedBranchId == null,
                            onClick = {
                                onBranchSelect(null)
                            }
                        )

                        branches.forEach { branch ->
                            InventoryBranchButton(
                                label = "B${branch.branchId}",
                                selected = selectedBranchId == branch.branchId,
                                onClick = {
                                    onBranchSelect(branch.branchId)
                                }
                            )
                        }
                    /* } else {
                        InventoryBranchButton(
                            label = "B$localBranchId",
                            selected = true,
                            onClick = {
                                onBranchSelect(localBranchId)
                            }
                        )
                    } */
                }
            }
        }
    }
}

@Composable
private fun InventoryBranchButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) ImGreen else Color.Transparent)
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
                        text = "Search ingredient or branch",
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
                    focusedBorderColor = ImGreen
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
                        focusedBorderColor = ImGreen
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
private fun EmptyInventoryText(
    text: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = ImTextSub
        )
    }
}

@Composable
private fun InventoryListItem(
    item: InventoryMonitoringRow
) {
    val context = LocalContext.current
    val imageFile = item.image?.let {
        ImageStorage.getImageFile(context, it)
    }

    val statusColor = when (item.status) {
        "Good" -> Color(0xFF4CAF50)
        "Normal" -> Color(0xFFFFB300)
        else -> Color(0xFFEF5350)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF5F5F5)),
                contentAlignment = Alignment.Center
            ) {
                if (imageFile != null && imageFile.exists()) {
                    AsyncImage(
                        model = imageFile,
                        contentDescription = item.ingredientName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Inventory2,
                        contentDescription = "Inventory",
                        tint = ImGreen,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.size(10.dp))

            Column {
                Text(
                    text = item.ingredientName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = ImTextMain
                )

                Text(
                    text = item.branchName.ifBlank {
                        if (item.branchId == 0) {
                            "All Branches"
                        } else {
                            "Branch ${item.branchId}"
                        }
                    },
                    fontSize = 12.sp,
                    color = ImTextSub
                )
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "${formatQuantity(item.currentStock)} ${item.unitType}",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = statusColor
            )

            Text(
                text = item.status,
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
    }
}

private fun formatQuantity(
    value: Double
): String {
    return if (value % 1.0 == 0.0) {
        value.toInt().toString()
    } else {
        String.format(Locale.US, "%.2f", value)
    }
}

private fun isSameDay(
    firstMillis: Long,
    secondMillis: Long
): Boolean {
    val first = Calendar.getInstance().apply {
        timeInMillis = firstMillis
    }

    val second = Calendar.getInstance().apply {
        timeInMillis = secondMillis
    }

    return first.get(Calendar.YEAR) == second.get(Calendar.YEAR) &&
            first.get(Calendar.DAY_OF_YEAR) == second.get(Calendar.DAY_OF_YEAR)
}
