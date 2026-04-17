package com.example.fruitylicious

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

// ── DATA MODELS ──────────────────────────────────────────────────────────────

data class AdjustmentHistoryItem(
    val ingredient: String,
    val reason: String,
    val staff: String,
    val date: String,
    val time: String,
    val change: String,
    val flow: String // e.g. "1997 -> 1897"
)

data class IngredientStock(
    val name: String,
    val stock: Int,
    val unit: String
)

// ── MAIN SCREEN ─────────────────────────────────────────────────────────────

@Composable
fun InventoryAdjustmentScreen(
    navController: NavController,
    drawerState: DrawerState,
    scope: CoroutineScope
) {
    // UI States for the adjustment form
    var selectedIngredient by remember { mutableStateOf<IngredientStock?>(null) }
    var adjustmentType by remember { mutableStateOf("Add") } // "Add" or "Reduce"
    var quantity by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    var dropdownExpanded by remember { mutableStateOf(false) }

    // Mock history data
    val recentAdjustments = listOf(
        AdjustmentHistoryItem("Strawberry", "spoilage", "Admin User", "4/15/2026", "10:44 PM", "-100", "1997 -> 1897"),
        AdjustmentHistoryItem("Dragon Fruit", "spoilage", "Staff User", "4/14/2026", "10:38 PM", "-7", "15 -> 8"),
        AdjustmentHistoryItem("Melon", "spoilage", "Admin User", "4/13/2026", "10:38 PM", "-5", "10 -> 5")
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

                Text(
                    text = "INVENTORY ADJUSTMENT",
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
            contentPadding = PaddingValues(top = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            
            // ── SECTION 2: NEW ADJUSTMENT CARD ───────────────────────────────
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = Color.White,
                    shadowElevation = 2.dp
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "New Adjustment",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )

                        // 1. Select Ingredient
                        Column {
                            Text("Select Ingredient", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF475569))
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = selectedIngredient?.name ?: "",
                                    onValueChange = {},
                                    readOnly = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    placeholder = { Text("Choose an Ingredients...", color = Color.LightGray) },
                                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        unfocusedBorderColor = Color(0xFFE2E8F0)
                                    )
                                )
                                Box(modifier = Modifier.matchParentSize().clickable { dropdownExpanded = true })
                                
                                DropdownMenu(
                                    expanded = dropdownExpanded,
                                    onDismissRequest = { dropdownExpanded = false },
                                    modifier = Modifier.fillMaxWidth(0.85f).background(Color.White).heightIn(max = 400.dp)
                                ) {
                                    CategorizedAdjustmentDropdown { ingredient ->
                                        selectedIngredient = ingredient
                                        dropdownExpanded = false
                                    }
                                }
                            }
                        }

                        // 2. Current Stock Display (Shows only when ingredient is selected)
                        if (selectedIngredient != null) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = Color(0xFFFFFBEB), // Light yellow highlight
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Current Stock", color = Color(0xFF92400E), fontSize = 14.sp)
                                    Row(verticalAlignment = Alignment.Bottom) {
                                        Text(
                                            text = "${selectedIngredient!!.stock}",
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.Black
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = selectedIngredient!!.unit,
                                            fontSize = 14.sp,
                                            color = Color.Gray,
                                            modifier = Modifier.padding(bottom = 2.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // 3. Adjustment Type
                        Column {
                            Text("Adjustment Type", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF475569))
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                // Add Stock Button
                                val isAdd = adjustmentType == "Add"
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(60.dp)
                                        .clickable { adjustmentType = "Add" }
                                        .border(
                                            width = if (isAdd) 2.dp else 1.dp,
                                            color = if (isAdd) Color(0xFF2E7D32) else Color(0xFFE2E8F0),
                                            shape = RoundedCornerShape(12.dp)
                                        ),
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isAdd) Color(0xFFF0FDF4) else Color.White
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, tint = if (isAdd) Color(0xFF2E7D32) else Color.Gray, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            "Add\nStock",
                                            textAlign = TextAlign.Center,
                                            fontSize = 13.sp,
                                            fontWeight = if (isAdd) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isAdd) Color(0xFF2E7D32) else Color.Gray,
                                            lineHeight = 16.sp
                                        )
                                    }
                                }

                                // Reduce Stock Button
                                val isReduce = adjustmentType == "Reduce"
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(60.dp)
                                        .clickable { adjustmentType = "Reduce" }
                                        .border(
                                            width = if (isReduce) 2.dp else 1.dp,
                                            color = if (isReduce) Color(0xFFB91C1C) else Color(0xFFE2E8F0),
                                            shape = RoundedCornerShape(12.dp)
                                        ),
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isReduce) Color(0xFFFEF2F2) else Color.White
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(Icons.Default.Remove, contentDescription = null, tint = if (isReduce) Color(0xFFB91C1C) else Color.Gray, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            "Reduce\nStock",
                                            textAlign = TextAlign.Center,
                                            fontSize = 13.sp,
                                            fontWeight = if (isReduce) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isReduce) Color(0xFFB91C1C) else Color.Gray,
                                            lineHeight = 16.sp
                                        )
                                    }
                                }
                            }
                        }

                        // 4. Quantity
                        Column {
                            val unitLabel = if (selectedIngredient != null) "(${selectedIngredient!!.unit})" else "(pcs)"
                            Text("Quantity $unitLabel", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF475569))
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = quantity,
                                onValueChange = { if (it.all { c -> c.isDigit() }) quantity = it },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("Enter quantity", color = Color.LightGray) },
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedBorderColor = Color(0xFFE2E8F0)
                                )
                            )
                        }

                        // 5. Reason
                        Column {
                            Text("Reason", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF475569))
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = reason,
                                onValueChange = { reason = it },
                                modifier = Modifier.fillMaxWidth().height(90.dp),
                                placeholder = { Text("Enter reason for adjustment...", color = Color.LightGray) },
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedBorderColor = Color(0xFFE2E8F0)
                                )
                            )
                        }

                        // 6. Submit Button
                        val isFormValid = selectedIngredient != null && quantity.isNotEmpty()
                        Button(
                            onClick = { /* Handle Submit */ },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            enabled = isFormValid,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2E7D32),
                                disabledContainerColor = Color(0xFFCBD5E1)
                            )
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = if (isFormValid) Color.White else Color(0xFF94A3B8), modifier = Modifier.size(20.dp))
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

            // ── SECTION 3: RECENT ADJUSTMENT LIST ────────────────────────────
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = Color.White,
                    shadowElevation = 2.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 16.dp)
                        ) {
                            Icon(Icons.Default.History, contentDescription = null, tint = Color(0xFF475569), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Recent Adjustment",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF334155)
                            )
                        }

                        HorizontalDivider(color = Color(0xFFF1F5F9))

                        recentAdjustments.forEachIndexed { index, item ->
                            AdjustmentItemUI(item)
                            if (index < recentAdjustments.size - 1) {
                                HorizontalDivider(color = Color(0xFFF1F5F9))
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── COMPONENT: ADJUSTMENT LIST ITEM UI ───────────────────────────────────────

@Composable
fun AdjustmentItemUI(item: AdjustmentHistoryItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(item.ingredient, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF1E293B))
            Text(item.reason, fontSize = 12.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(4.dp))
            Text(item.staff, fontSize = 12.sp, color = Color.Gray)
            Text(item.flow, fontSize = 11.sp, color = Color.LightGray)
        }
        Column(horizontalAlignment = Alignment.End) {
            Surface(
                color = Color(0xFF1E293B), // Dark background for the badge
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = item.change,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(item.date, fontSize = 11.sp, color = Color.Gray)
            Text(item.time, fontSize = 11.sp, color = Color.Gray)
        }
    }
}

// ── COMPONENT: CATEGORIZED DROPDOWN CONTENT ──────────────────────────────────

@Composable
fun CategorizedAdjustmentDropdown(onSelected: (IngredientStock) -> Unit) {
    val categories = mapOf(
        "Fruits" to listOf(
            IngredientStock("Mango", 30, "pcs"),
            IngredientStock("Dragon Fruit", 8, "pcs"),
            IngredientStock("Guyabano", 12, "pcs"),
            IngredientStock("Strawberry", 10, "pack"),
            IngredientStock("Buko", 20, "pcs"),
            IngredientStock("Avocado", 25, "pcs"),
            IngredientStock("Melon", 5, "pcs"),
            IngredientStock("Banana", 40, "pcs"),
            IngredientStock("Apple", 18, "pcs")
        ),
        "Toppings & Mix-ins" to listOf(
            IngredientStock("Oreo", 20, "pack"),
            IngredientStock("Crashed Graham", 15, "pack"),
            IngredientStock("Cheese", 12, "pack"),
            IngredientStock("Lemon Square Cheesecake", 10, "pack"),
            IngredientStock("Nata de Coco", 18, "pack"),
            IngredientStock("Pearl", 14, "pack")
        ),
        "Syrups" to listOf(
            IngredientStock("Syrup - Caramel", 8, "pack"),
            IngredientStock("Syrup - Mango", 8, "pack"),
            IngredientStock("Syrup - Chocolate", 8, "pack"),
            IngredientStock("Syrup - Strawberry", 8, "pack")
        ),
        "Dairy & Sweeteners" to listOf(
            IngredientStock("Evap", 24, "can"),
            IngredientStock("Condense", 20, "can"),
            IngredientStock("Sugar", 15, "pack")
        ),
        "Others" to listOf(
            IngredientStock("Ice", 10, "sack")
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
                            Text("(${item.stock} ${item.unit})", fontSize = 12.sp, color = Color.Gray)
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
