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

data class RestockEntry(
    val ingredient: String,
    val supplier: String?,
    val staff: String,
    val date: String,
    val time: String,
    val quantity: String
)

// ── MAIN SCREEN ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestockScreen(
    navController: NavController,
    drawerState: DrawerState,
    scope: CoroutineScope
) {
    // UI State for managing pop-ups and filters
    var showRestockEntry by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedDateText by remember { mutableStateOf("") }

    // Sample data for the restock history list
    val restockHistory = listOf(
        RestockEntry("Avocado", "Sackma Diy", "Staff User", "4/14/2026", "12:38 AM", "+ 20 pcs"),
        RestockEntry("Avocado", null, "Staff User", "4/14/2026", "12:38 AM", "+ 20 pcs"),
        RestockEntry("Evap", null, "Staff User", "4/15/2026", "12:38 AM", "+ 12 can"),
        RestockEntry("Pearl", null, "Admin User", "4/13/2026", "12:38 AM", "+ 10 pack")
    )

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
                // Burger Menu Trigger (Connected to Sidebar)
                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                    Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.White)
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Page Title
                Text(
                    text = "RESTOCK",
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
                        // Search Field (Functional)
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

                        // Date Selector Field (Triggers Date Picker)
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
                            // Catch clicks for picker
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
                        // List Header
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
                                    text = "${restockHistory.size} entries",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        HorizontalDivider(color = Color(0xFFF0F0F0), thickness = 1.dp)

                        // Restock History Items
                        restockHistory.forEachIndexed { index, entry ->
                            RestockHistoryItem(entry)
                            if (index < restockHistory.size - 1) {
                                HorizontalDivider(color = Color(0xFFF0F0F0), thickness = 0.5.dp)
                            }
                        }
                    }
                }
            }
        }
    }

    // ── SECTION 5: POP-UPS (DIALOGS) ──────────────────────────────────────────

    // Restock Entry Popup
    if (showRestockEntry) {
        RestockEntryDialog(onDismiss = { showRestockEntry = false })
    }
}

// ── COMPONENT: RESTOCK HISTORY ITEM ──────────────────────────────────────────

@Composable
fun RestockHistoryItem(entry: RestockEntry) {
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
            if (entry.supplier != null) {
                Text(
                    text = "Supplier: ${entry.supplier}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
            Text(
                text = entry.staff,
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

// ── COMPONENT: RESTOCK ENTRY POPUP ───────────────────────────────────────────

@Composable
fun RestockEntryDialog(onDismiss: () -> Unit) {
    var selectedIngredient by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var supplier by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var dropdownExpanded by remember { mutableStateOf(false) }

    // Validation logic for "Add Stock" button
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
                            text = "Restock Entry",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                        Text(
                            text = "Add stock for an ingredient",
                            fontSize = 13.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Ingredient Input (Typeable Dropdown)
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
                    // Clicking open the dropdown
                    Box(modifier = Modifier.matchParentSize().clickable { dropdownExpanded = true })
                    
                    DropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false },
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .background(Color.White)
                            .heightIn(max = 400.dp)
                    ) {
                        IngredientDropdownContent { ingredient ->
                            selectedIngredient = ingredient
                            dropdownExpanded = false
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Quantity Input (Typeable)
                Text("Quantity to Add", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF334155))
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { if (it.all { c -> c.isDigit() }) quantity = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Enter quantity", color = Color.LightGray) },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = Color(0xFFCBD5E1)
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color(0xFFE2E8F0))
                Spacer(modifier = Modifier.height(16.dp))

                // Supplier Input (Typeable)
                Row {
                    Text("Supplier", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF334155))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("(optional)", fontSize = 13.sp, color = Color.Gray)
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = supplier,
                    onValueChange = { supplier = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("e.g. Farm Fresh, Dairy Best", color = Color.LightGray) },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = Color(0xFFCBD5E1)
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Note Input (Typeable)
                Row {
                    Text("Note", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF334155))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("(optional)", fontSize = 13.sp, color = Color.Gray)
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    placeholder = { Text("Any additional notes...", color = Color.LightGray) },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = Color(0xFFCBD5E1)
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Add Stock Button (Enabled only if Ingredient and Quantity are filled)
                Button(
                    onClick = { if (isFormValid) onDismiss() },
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
                        imageVector = Icons.Default.Inventory, 
                        contentDescription = null, 
                        tint = if (isFormValid) Color.White else Color(0xFF94A3B8)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Add Stock", 
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
fun IngredientDropdownContent(onSelected: (String) -> Unit) {
    val categories = mapOf(
        "Fruits" to listOf("Mango", "Dragon Fruit", "Guyabano", "Strawberry", "Buko", "Avocado", "Melon", "Banana", "Apple"),
        "Toppings & Mix-ins" to listOf("Oreo", "Crashed Graham", "Cheese", "Lemon Square Cheesecake", "Nata de Coco", "Pearl"),
        "Syrups" to listOf("Syrup - Caramel", "Syrup - Mango", "Syrup - Chocolate", "Syrup - Strawberry"),
        "Dairy & Sweeteners" to listOf("Evap", "Condense", "Sugar"),
        "Others" to listOf("Ice")
    )

    Column {
        categories.forEach { (category, items) ->
            // Category Header
            Text(
                text = category,
                modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp),
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF2E7D32)
            )
            // Ingredient Items
            items.forEach { item ->
                DropdownMenuItem(
                    text = { Text(item, fontSize = 14.sp, color = Color(0xFF334155)) },
                    onClick = { onSelected(item) },
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp)
                )
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = Color(0xFFF1F5F9))
        }
    }
}
