package com.example.fruitylicious.ui.staff.waste

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

// ── DATA MODELS ──────────────────────────────────────────────────────────────

data class WasteHistoryItem(
    val ingredient: String,
    val staff: String,
    val date: String,
    val time: String,
    val quantity: String
)

// ── MAIN SCREEN ─────────────────────────────────────────────────────────────

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

    // Sample data for the waste history list
    val wasteHistory = listOf(
        WasteHistoryItem("Avocado", "Staff User", "4/14/2026", "12:38 AM", "- 20 pcs"),
        WasteHistoryItem("Evap", "Staff User", "4/15/2026", "12:38 AM", "- 12 can"),
        WasteHistoryItem("Pearl", "Admin User", "4/13/2026", "12:38 AM", "- 10 pack"),
        WasteHistoryItem("Pearl", "Admin User", "4/13/2026", "12:38 AM", "- 10 pack")
    )

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
                Column(
                    modifier = Modifier
                        .clickable { scope.launch { drawerState.open() } }
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    repeat(3) {
                        Box(
                            modifier = Modifier
                                .width(20.dp)
                                .height(2.dp)
                                .background(Color.White)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Page Title
                Text(
                    text = "WASTE MANAGEMENT",
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
                            Box(modifier = Modifier.matchParentSize().clickable { showDatePicker = true })
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
                                    text = "${wasteHistory.size} entries",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        HorizontalDivider(color = Color(0xFFF0F0F0), thickness = 1.dp)

                        // Render history items
                        wasteHistory.forEachIndexed { index, entry ->
                            WasteRecordRow(entry)
                            if (index < wasteHistory.size - 1) {
                                HorizontalDivider(color = Color(0xFFF0F0F0), thickness = 0.5.dp)
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
        WasteEntryDialog(onDismiss = { showWasteEntry = false })
    }
}

// ── COMPONENT: WASTE HISTORY ITEM ROW ───────────────────────────────────────

@Composable
fun WasteRecordRow(entry: WasteHistoryItem) {
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

// ── COMPONENT: WASTE ENTRY DIALOG (POPUP) ───────────────────────────────────

@Composable
fun WasteEntryDialog(onDismiss: () -> Unit) {
    var selectedIngredient by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
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

                Spacer(modifier = Modifier.height(24.dp))

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
                        WasteIngredientDropdownList { ingredient ->
                            selectedIngredient = ingredient
                            dropdownExpanded = false
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Quantity Input (Typeable)
                Text("Quantity", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF334155))
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

                Spacer(modifier = Modifier.height(24.dp))

                // Submit Waste Button
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
fun WasteIngredientDropdownList(onSelected: (String) -> Unit) {
    val categories = mapOf(
        "Fruits" to listOf("Mango", "Dragon Fruit", "Guyabano", "Strawberry", "Buko", "Avocado", "Melon", "Banana", "Apple"),
        "Toppings & Mix-ins" to listOf("Oreo", "Crashed Graham", "Cheese", "Lemon Square Cheesecake", "Nata de Coco", "Pearl"),
        "Syrups" to listOf("Syrup - Caramel", "Syrup - Mango", "Syrup - Chocolate", "Syrup - Strawberry"),
        "Dairy & Sweeteners" to listOf("Evap", "Condense", "Sugar"),
        "Others" to listOf("Ice")
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
                    text = { Text(item, fontSize = 14.sp, color = Color(0xFF334155)) },
                    onClick = { onSelected(item) },
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp)
                )
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = Color(0xFFF1F5F9))
        }
    }
}
