package com.example.fruitylicious.ui.staff.waste

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.example.fruitylicious.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// ── DATA MODELS ──────────────────────────────────────────────────────────────

data class WasteHistoryItem(
    val ingredient: String,
    val staff: String,
    val date: String,
    val time: String,
    val quantity: String,
    val branch: String,
    val imageRes: Int? = null,
    val reason: String? = null
)

// ── MAIN SCREEN ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WasteManagementScreen(
    navController: NavController,
    drawerState: DrawerState,
    scope: CoroutineScope
) {
    // UI State for pop-ups and input fields
    var showWasteEntry by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedDateText by remember { mutableStateOf("") }
    var selectedBranch by remember { mutableStateOf("All") }
    var selectedEntryForReason by remember { mutableStateOf<WasteHistoryItem?>(null) }

    // State list for waste history to make it functional
    val wasteHistory = remember { 
        mutableStateListOf(
            WasteHistoryItem("Avocado", "Staff User", "04/14/2026", "12:38 AM", "- 20 pcs", "B1", R.drawable.avocado, "Overripe and bruised."),
            WasteHistoryItem("Evap", "Staff User", "04/15/2026", "12:38 AM", "- 12 can", "B2", R.drawable.evap, "Expired batch."),
            WasteHistoryItem("Pearl", "Admin User", "04/13/2026", "12:38 AM", "- 10 pack", "B1", R.drawable.pearl),
            WasteHistoryItem("Pearl", "Admin User", "04/13/2026", "12:38 AM", "- 10 pack", "B2", R.drawable.pearl)
        )
    }

    // Filtered list based on search query, branch, and date
    val filteredHistory = remember(searchQuery, selectedBranch, selectedDateText, wasteHistory.size) {
        wasteHistory.filter { item ->
            val matchesSearch = item.ingredient.contains(searchQuery, ignoreCase = true) || 
                              item.staff.contains(searchQuery, ignoreCase = true)
            val matchesBranch = if (selectedBranch == "All") true else item.branch == selectedBranch
            val matchesDate = if (selectedDateText.isEmpty()) true else item.date == selectedDateText
            
            matchesSearch && matchesBranch && matchesDate
        }.reversed() // Show newest first if we assume they are added at the end
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
                // Sidebar Menu Trigger
                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                    Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.White)
                }

                Text(
                    text = "WASTE MANAGEMENT",
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
            contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            
            // ── SECTION 2: WASTE ENTRY BUTTON ──────────────────────────────
            item {
                Button(
                    onClick = { showWasteEntry = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .shadow(2.dp, RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
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

            // ── SECTION 3: SEARCH & DATE FILTER CARD ──────────────────────────
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
                        // Search Field (Typeable)
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
                            Box(modifier = Modifier
                                .matchParentSize()
                                .clickable { showDatePicker = true })
                        }
                    }
                }
            }

            // ── SECTION 4: WASTE HISTORY LIST CARD ──────────────────────────
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
                                    text = "${filteredHistory.size} entries",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        HorizontalDivider(color = Color(0xFFF0F0F0), thickness = 1.dp)

                        // Render history items
                        if (filteredHistory.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No records found", color = Color.Gray, fontSize = 14.sp)
                            }
                        } else {
                            filteredHistory.forEachIndexed { index, entry ->
                                WasteRecordRow(entry, onClick = { selectedEntryForReason = it })
                                if (index < filteredHistory.size - 1) {
                                    HorizontalDivider(color = Color(0xFFF0F0F0), thickness = 0.5.dp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ── SECTION 5: POP-UPS (DIALOGS) ──────────────────────────────────────────

    // Waste Entry Popup
    if (showWasteEntry) {
        WasteEntryDialog(
            onDismiss = { showWasteEntry = false },
            onAddEntry = { newItem ->
                wasteHistory.add(newItem)
                showWasteEntry = false
            },
            currentBranch = if (selectedBranch == "All") "B1" else selectedBranch
        )
    }

    selectedEntryForReason?.let { entry ->
        WasteReasonDialog(
            entry = entry,
            onDismiss = { selectedEntryForReason = null }
        )
    }
}

// ── COMPONENT: WASTE HISTORY ITEM ROW ───────────────────────────────────────

@Composable
fun WasteRecordRow(entry: WasteHistoryItem, onClick: (WasteHistoryItem) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(entry) }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Ingredient Image
        Surface(
            modifier = Modifier.size(52.dp),
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFFF8FAFC)
        ) {
            if (entry.imageRes != null) {
                Image(
                    painter = painterResource(id = entry.imageRes),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
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
                    text = entry.ingredient,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color(0xFF1E293B)
                )
                Text(
                    text = entry.quantity,
                    color = Color(0xFF64748B),
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
                    text = "${entry.staff} (${entry.branch})",
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8)
                )
                Text(
                    text = "${entry.date}  ${entry.time}",
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8)
                )
            }
        }
    }
}

// ── COMPONENT: WASTE REASON DIALOG (POPUP) ───────────────────────────────────

@Composable
fun WasteReasonDialog(
    entry: WasteHistoryItem,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth().padding(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "Waste Reason", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                        Text(text = "Additional details for this waste entry", fontSize = 13.sp, color = Color(0xFF64748B))
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text("Ingredient", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF334155))
                Spacer(modifier = Modifier.height(4.dp))
                Text(entry.ingredient, fontSize = 15.sp, color = Color(0xFF1E293B), fontWeight = FontWeight.Medium)

                Spacer(modifier = Modifier.height(16.dp))

                Text("Reason", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF334155))
                Spacer(modifier = Modifier.height(8.dp))
                
                Surface(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp),
                    color = Color(0xFFF8FAFC),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Box(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = if (entry.reason.isNullOrBlank()) "no notes" else entry.reason,
                            fontSize = 14.sp,
                            color = if (entry.reason.isNullOrBlank()) Color.LightGray else Color(0xFF334155)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(52.dp).shadow(2.dp, RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                ) {
                    Text(text = "Close", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ── COMPONENT: WASTE ENTRY DIALOG (POPUP) ───────────────────────────────────

@Composable
fun WasteEntryDialog(
    onDismiss: () -> Unit,
    onAddEntry: (WasteHistoryItem) -> Unit,
    currentBranch: String
) {
    var selectedIngredient by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("pcs") } // Default unit
    var reason by remember { mutableStateOf("") }
    var dropdownExpanded by remember { mutableStateOf(false) }

    // Validation logic for the Submit button
    val isFormValid = selectedIngredient.isNotEmpty() && quantity.isNotEmpty()

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
                // Header with X close button
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
                            color = Color(0xFF1E293B)
                        )
                        Text(
                            text = "Record waste of an ingredient",
                            fontSize = 13.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Ingredient Selection
                Text("Ingredient", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF334155))
                Spacer(modifier = Modifier.height(8.dp))
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedIngredient,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Choose an ingredient...", color = Color.LightGray) },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = Color(0xFFCBD5E1)
                        )
                    )
                    Box(modifier = Modifier.matchParentSize().clickable { dropdownExpanded = true })
                    
                    DropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false },
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .background(Color.White)
                            .heightIn(max = 400.dp)
                    ) {
                        WasteIngredientDropdownList { name, qtyUnit ->
                            selectedIngredient = name
                            // Extract unit from qty string (e.g. "30 pcs" -> "pcs")
                            unit = qtyUnit.split(" ").lastOrNull() ?: "pcs"
                            dropdownExpanded = false
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Quantity Input (Typeable)
                Text("Quantity", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF334155))
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = quantity,
                        onValueChange = { if (it.all { c -> c.isDigit() }) quantity = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Enter quantity", color = Color.LightGray) },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = Color(0xFFCBD5E1)
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = unit, color = Color.Gray, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color(0xFFE2E8F0))
                Spacer(modifier = Modifier.height(16.dp))

                // Reason (Optional)
                Row {
                    Text("Reason", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF334155))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("(optional)", fontSize = 13.sp, color = Color.Gray)
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    placeholder = { Text("Add reason...", color = Color.LightGray) },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = Color(0xFFCBD5E1)
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Add Image
                Text("Add Image", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF334155))
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .clickable { /* TODO: Image Picker */ },
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate, 
                            contentDescription = null, 
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Upload Image", fontSize = 12.sp, color = Color(0xFF94A3B8))
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Submit Waste Button
                Button(
                    onClick = { 
                        if (isFormValid) {
                            val now = Calendar.getInstance().time
                            val dateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
                            val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
                            
                            val newItem = WasteHistoryItem(
                                ingredient = selectedIngredient,
                                staff = "Current User", // Mock user
                                date = dateFormat.format(now),
                                time = timeFormat.format(now),
                                quantity = "- $quantity $unit",
                                branch = currentBranch,
                                imageRes = when (selectedIngredient) {
                                    "Avocado" -> R.drawable.avocado
                                    "Evap" -> R.drawable.evap
                                    "Pearl" -> R.drawable.pearl
                                    else -> null
                                },
                                reason = reason.takeIf { it.isNotBlank() }
                            )
                            onAddEntry(newItem)
                        } 
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .shadow(2.dp, RoundedCornerShape(12.dp)),
                    enabled = isFormValid,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2E7D32),
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

// ── COMPONENT: CATEGORIZED INGREDIENT LIST ───────────────────────────────────

@Composable
fun WasteIngredientDropdownList(onSelected: (String, String) -> Unit) {
    val categories = mapOf(
        "Fruits" to listOf(
            "Mango" to "30 pcs",
            "Dragon Fruit" to "8 pcs",
            "Guyabano" to "12 pcs",
            "Strawberry" to "10 pack",
            "Buko" to "20 pcs",
            "Avocado" to "25 pcs",
            "Melon" to "5 pcs",
            "Banana" to "40 pcs",
            "Apple" to "18 pcs"
        ),
        "Toppings & Mix-ins" to listOf(
            "Oreo" to "20 pack",
            "Crashed Graham" to "15 pack",
            "Cheese" to "12 pack",
            "Lemon Square Cheesecake" to "10 pack",
            "Nata de Coco" to "18 pack",
            "Pearl" to "14 pack"
        ),
        "Syrups" to listOf(
            "Syrup - Caramel" to "8 pack",
            "Syrup - Mango" to "8 pack",
            "Syrup - Chocolate" to "8 pack",
            "Syrup - Strawberry" to "6 pack"
        ),
        "Dairy & Sweeteners" to listOf(
            "Evap" to "24 can",
            "Condense" to "20 can",
            "Sugar" to "15 pack"
        ),
        "Others" to listOf(
            "Ice" to "10 sack"
        )
    )

    Column {
        categories.forEach { (category, items) ->
            Text(
                text = category,
                modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp),
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF2E7D32)
            )
            items.forEach { (name, qty) ->
                DropdownMenuItem(
                    text = { 
                        Text(
                            text = "$name ($qty)", 
                            fontSize = 14.sp, 
                            color = Color(0xFF334155)
                        ) 
                    },
                    onClick = { onSelected(name, qty) },
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp)
                )
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = Color(0xFFF1F5F9))
        }
    }
}
