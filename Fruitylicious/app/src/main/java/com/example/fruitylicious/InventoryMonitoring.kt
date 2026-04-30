package com.example.fruitylicious

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.fruitylicious.ui.admin.system.AuditLogScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// ── DATA MODELS ──────────────────────────────────────────────────────────────

data class MonitoringItem(
    val name: String,
    val quantity: Int,
    val unit: String,
    val branch: String,
    val date: String // Last updated date for filtering
) {
    val status: String get() = when {
        quantity >= 50 -> "Good"
        quantity >= 20 -> "Normal"
        else -> "Low"
    }

    val statusColor: Color get() = when (status) {
        "Good" -> Color(0xFF4CAF50) // Green
        "Normal" -> Color(0xFFFFB300) // Orange/Yellow
        else -> Color(0xFFEF5350) // Red
    }
}

// ── MAIN SCREEN ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryMonitoringScreen(
    navController: NavController,
    drawerState: DrawerState,
    scope: CoroutineScope
) {
    // UI State
    var searchQuery by remember { mutableStateOf("") }
    var selectedDateText by remember { mutableStateOf("") }
    var selectedBranch by remember { mutableStateOf("B1") }
    var showDatePicker by remember { mutableStateOf(false) }

    // Sample data
    val monitoringItems = remember {
        mutableStateListOf(
            MonitoringItem("Bananas", 90, "pcs", "B1", "04/07/2026"),
            MonitoringItem("Oreo", 7, "packs", "B1", "04/07/2026"),
            MonitoringItem("Condensed Milk", 4, "cans", "B1", "04/07/2026"),
            MonitoringItem("Mango", 15, "pcs", "B1", "04/07/2026"),
            MonitoringItem("Apple", 55, "pcs", "B2", "04/07/2026"),
            MonitoringItem("Strawberry", 10, "packs", "B2", "04/07/2026"),
            MonitoringItem("Buko", 25, "pcs", "All", "04/07/2026")
        )
    }

    // Filtering Logic
    val filteredItems = remember(searchQuery, selectedBranch, selectedDateText) {
        monitoringItems.filter { item ->
            val matchesSearch = item.name.contains(searchQuery, ignoreCase = true)
            val matchesBranch = if (selectedBranch == "All") true else item.branch == selectedBranch || item.branch == "All"
            val matchesDate = if (selectedDateText.isEmpty()) true else item.date == selectedDateText
            
            matchesSearch && matchesBranch && matchesDate
        }
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
                    text = "INVENTORY MONITORING",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                // Branch Selector Toggle (Updated to match AuditLogScreen)
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
                                .background(if (isSelected) Color(0xFF2E7D32) else Color.Transparent)
                                .clickable { selectedBranch = branch }
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
                            placeholder = { Text("Search ingredient, staff, etc.", color = Color.LightGray, fontSize = 14.sp) },
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
                                onValueChange = {},
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
                            Box(modifier = Modifier.matchParentSize().clickable { showDatePicker = true })
                        }
                    }
                }
            }

            // ── SECTION 3: INGREDIENT LIST ──────────────────────────────────
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
                        if (filteredItems.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No ingredients found", color = Color.Gray)
                            }
                        } else {
                            filteredItems.forEachIndexed { index, item ->
                                InventoryListItem(item)
                                if (index < filteredItems.size - 1) {
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

@Composable
fun InventoryListItem(item: MonitoringItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = item.name,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF1E293B)
        )

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "${item.quantity} ${item.unit}",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = item.statusColor
            )
            Text(
                text = item.status,
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
    }
}
@Preview(showBackground = true)
@Composable
fun InventoryMonitoringScreenPreview() {
    InventoryMonitoringScreen(
        navController = NavController(LocalContext.current),
        drawerState = rememberDrawerState(initialValue = DrawerValue.Closed),
        scope = rememberCoroutineScope()
    )
}