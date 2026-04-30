package com.example.fruitylicious

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// ── DATA MODELS ──────────────────────────────────────────────────────────────

data class RestockEntryItem(
    val ingredient: String,
    val supplier: String?,
    val staff: String,
    val date: String,
    val time: String,
    val quantity: String,
    val branch: String
)

data class RestockIngredient(
    val name: String,
    val stock: String
)

// ── MAIN SCREEN ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestockScreen(
    navController: NavController,
    drawerState: DrawerState,
    scope: CoroutineScope
) {
    // UI State
    var showRestockEntry by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedDateText by remember { mutableStateOf("") }
    var selectedBranch by remember { mutableStateOf("B1") }

    // Functional State for History
    val restockHistory = remember {
        mutableStateListOf(
            RestockEntryItem("Avocado", "Sackma Diy", "Staff User", "4/14/2026", "12:38 AM", "+ 20 pcs", "B1"),
            RestockEntryItem("Avocado", null, "Staff User", "4/14/2026", "12:38 AM", "+ 20 pcs", "B2"),
            RestockEntryItem("Evap", null, "Staff User", "4/15/2026", "12:38 AM", "+ 12 can", "B1"),
            RestockEntryItem("Pearl", null, "Admin User", "4/13/2026", "12:38 AM", "+ 10 pack", "B1")
        )
    }

    // Filtering logic
    val filteredHistory = remember(searchQuery, selectedBranch, selectedDateText, restockHistory.size) {
        restockHistory.filter { item ->
            val matchesSearch = item.ingredient.contains(searchQuery, ignoreCase = true) ||
                              (item.supplier?.contains(searchQuery, ignoreCase = true) ?: false) ||
                              item.staff.contains(searchQuery, ignoreCase = true)
            val matchesBranch = if (selectedBranch == "All") true else item.branch == selectedBranch
            val matchesDate = if (selectedDateText.isEmpty()) true else item.date == selectedDateText
            matchesSearch && matchesBranch && matchesDate
        }.reversed()
    }

    // Date Picker State
    val datePickerState = rememberDatePickerState()

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val sdf = SimpleDateFormat("M/d/yyyy", Locale.getDefault())
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
                    text = "RESTOCK",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                // Branch Selector Toggle
                Surface(
                    color = Color(0xFF1B5E20),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf("B1", "B2", "All").forEach { branch ->
                            val isSelected = selectedBranch == branch
                            Surface(
                                color = if (isSelected) Color.White else Color.Transparent,
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .clickable { selectedBranch = branch }
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(horizontal = 12.dp)
                                ) {
                                    Text(
                                        text = branch,
                                        color = if (isSelected) Color(0xFF2E7D32) else Color.White,
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

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            
            // ── SECTION 2: RESTOCK ENTRY BUTTON ──────────────────────────────
            item {
                Button(
                    onClick = { showRestockEntry = true },
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
                        text = "Restock Entry",
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

            // ── SECTION 4: RESTOCK HISTORY LIST CARD ──────────────────────────
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
                                text = "Restock History",
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

                        if (filteredHistory.isEmpty()) {
                            Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                Text("No restock records found", color = Color.Gray)
                            }
                        } else {
                            filteredHistory.forEachIndexed { index, entry ->
                                RestockRecordRow(entry)
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

    if (showRestockEntry) {
        RestockEntryDialog(
            onDismiss = { showRestockEntry = false },
            onAdd = { entry ->
                restockHistory.add(entry)
                showRestockEntry = false
            },
            currentBranch = if (selectedBranch == "All") "B1" else selectedBranch
        )
    }
}

@Composable
fun RestockRecordRow(entry: RestockEntryItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.ingredient,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = Color(0xFF333333)
            )
            if (!entry.supplier.isNullOrBlank()) {
                Text(
                    text = "Supplier: ${entry.supplier}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
            Text(
                text = "${entry.staff} (${entry.branch})",
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
                    text = entry.quantity,
                    color = Color(0xFF475569),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${entry.date}  ${entry.time}",
                fontSize = 11.sp,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun RestockEntryDialog(
    onDismiss: () -> Unit,
    onAdd: (RestockEntryItem) -> Unit,
    currentBranch: String
) {
    var selectedIngredient by remember { mutableStateOf<RestockIngredient?>(null) }
    var quantity by remember { mutableStateOf("") }
    var supplier by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var dropdownExpanded by remember { mutableStateOf(false) }

    val isFormValid = selectedIngredient != null && quantity.isNotEmpty()

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
                        Text(text = "Restock Entry", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                        Text(text = "Add stock for an ingredient", fontSize = 13.sp, color = Color(0xFF64748B))
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text("Ingredient", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF334155))
                Spacer(modifier = Modifier.height(8.dp))
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedIngredient?.name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Choose an ingredient...", color = Color.LightGray) },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color(0xFFCBD5E1))
                    )
                    Box(modifier = Modifier.matchParentSize().clickable { dropdownExpanded = true })
                    
                    DropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false },
                        modifier = Modifier.fillMaxWidth(0.85f).background(Color.White).heightIn(max = 400.dp)
                    ) {
                        RestockIngredientDropdownList { ingredient ->
                            selectedIngredient = ingredient
                            dropdownExpanded = false
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("Quantity to Add", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF334155))
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { if (it.all { c -> c.isDigit() }) quantity = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Enter quantity", color = Color.LightGray) },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color(0xFFCBD5E1))
                )

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color(0xFFE2E8F0))
                Spacer(modifier = Modifier.height(16.dp))

                Text("Supplier (optional)", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF334155))
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = supplier,
                    onValueChange = { supplier = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("e.g. Farm Fresh, Dairy Best", color = Color.LightGray) },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color(0xFFCBD5E1))
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text("Note (optional)", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF334155))
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    placeholder = { Text("Any additional notes...", color = Color.LightGray) },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color(0xFFCBD5E1))
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { 
                        if (isFormValid) {
                            val now = Calendar.getInstance().time
                            val unit = selectedIngredient?.stock?.split(" ")?.last() ?: "pcs"
                            onAdd(RestockEntryItem(
                                ingredient = selectedIngredient!!.name,
                                supplier = supplier.takeIf { it.isNotBlank() },
                                staff = "Admin User", // Mock
                                date = SimpleDateFormat("M/d/yyyy", Locale.getDefault()).format(now),
                                time = SimpleDateFormat("h:mm a", Locale.getDefault()).format(now),
                                quantity = "+ $quantity $unit",
                                branch = currentBranch
                            ))
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp).shadow(2.dp, RoundedCornerShape(12.dp)),
                    enabled = isFormValid,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2E7D32),
                        disabledContainerColor = Color(0xFFCBD5E1)
                    )
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = if (isFormValid) Color.White else Color(0xFF94A3B8))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Add Stock", color = if (isFormValid) Color.White else Color(0xFF94A3B8), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun RestockIngredientDropdownList(onSelected: (RestockIngredient) -> Unit) {
    val categories = mapOf(
        "Fruits" to listOf(
            RestockIngredient("Mango", "30 pcs"),
            RestockIngredient("Dragon Fruit", "8 pcs"),
            RestockIngredient("Guyabano", "12 pcs"),
            RestockIngredient("Strawberry", "10 pack"),
            RestockIngredient("Buko", "20 pcs"),
            RestockIngredient("Avocado", "25 pcs"),
            RestockIngredient("Melon", "5 pcs"),
            RestockIngredient("Banana", "40 pcs"),
            RestockIngredient("Apple", "18 pcs")
        ),
        "Toppings & Mix-ins" to listOf(
            RestockIngredient("Oreo", "20 pack"),
            RestockIngredient("Crashed Graham", "15 pack"),
            RestockIngredient("Cheese", "12 pack"),
            RestockIngredient("Lemon Square Cheesecake", "10 pack"),
            RestockIngredient("Nata de Coco", "18 pack"),
            RestockIngredient("Pearl", "14 pack")
        ),
        "Syrups" to listOf(
            RestockIngredient("Syrup - Caramel", "8 pack"),
            RestockIngredient("Syrup - Mango", "8 pack"),
            RestockIngredient("Syrup - Chocolate", "8 pack"),
            RestockIngredient("Syrup - Strawberry", "6 pack")
        ),
        "Dairy & Sweeteners" to listOf(
            RestockIngredient("Evap", "24 can"),
            RestockIngredient("Condense", "20 can"),
            RestockIngredient("Sugar", "15 pack")
        ),
        "Others" to listOf(
            RestockIngredient("Ice", "10 sack"),
            RestockIngredient("Medium Cups", "10 packs"),
            RestockIngredient("Large Cups", "10 packs"),
            RestockIngredient("Lids", "10 packs"),
            RestockIngredient("Straws", "15 packs")
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
            items.forEach { item ->
                DropdownMenuItem(
                    text = { 
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(item.name, fontSize = 14.sp, color = Color(0xFF334155))
                            Text("(${item.stock})", fontSize = 12.sp, color = Color.Gray)
                        }
                    },
                    onClick = { onSelected(item) },
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp)
                )
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = Color(0xFFF1F5F9))
        }
    }
}
