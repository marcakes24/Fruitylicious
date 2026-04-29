package com.example.fruitylicious

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

// ── DATA MODELS ──────────────────────────────────────────────────────────────

data class RecipeProduct(
    val name: String,
    val emoji: String,
    val color: Color = Color(0xFFFFFDE7) // Light yellowish
)

data class RecipeIngredient(
    val name: String,
    val quantity: String,
    val unit: String
)

// ── MAIN SCREEN ─────────────────────────────────────────────────────────────

@Composable
fun RecipeManagementScreen(
    navController: NavController,
    drawerState: DrawerState,
    scope: CoroutineScope
) {
    var selectedProduct by remember { mutableStateOf<RecipeProduct?>(null) }
    
    // Local "database" for recipes indexed by "ProductName-Size"
    val recipeStorage = remember { 
        mutableStateMapOf<String, List<RecipeIngredient>>() 
    }

    val products = listOf(
        RecipeProduct("Avocado", "🥑"),
        RecipeProduct("Mango", "🥭"),
        RecipeProduct("Dragon Fruit", "🐉"),
        RecipeProduct("Banana", "🍌"),
        RecipeProduct("Guyabano", "🍈"),
        RecipeProduct("Buko", "🥥"),
        RecipeProduct("Apple", "🍎"),
        RecipeProduct("Strawberry", "🍓"),
        RecipeProduct("Melon", "🍈"),
        RecipeProduct("Cheesecake", "🍰"),
        RecipeProduct("Oreo", "🍪"),
        RecipeProduct("Blueberry", "🫐")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFEAA0)) // Pale yellow background
    ) {
        // ── TOP HEADER ──────────────────────────────────────────────────────
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
                    text = "RECIPE MANAGEMENT",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Products Available",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF333333),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Surface(
                modifier = Modifier.fillMaxWidth().weight(1f),
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                shadowElevation = 2.dp
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.padding(12.dp),
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(products) { product ->
                        ProductGridItem(product) {
                            selectedProduct = product
                        }
                    }
                }
            }
        }
    }

    if (selectedProduct != null) {
        RecipeDetailDialog(
            product = selectedProduct!!,
            onDismiss = { selectedProduct = null },
            recipeStorage = recipeStorage
        )
    }
}

@Composable
fun ProductGridItem(product: RecipeProduct, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .aspectRatio(0.9f)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = product.color,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEEEEEE))
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(8.dp)
        ) {
            Text(text = product.emoji, fontSize = 32.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = product.name,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF333333),
                textAlign = TextAlign.Center
            )
        }
    }
}

// ── RECIPE DETAIL DIALOG ────────────────────────────────────────────────────

@Composable
fun RecipeDetailDialog(
    product: RecipeProduct, 
    onDismiss: () -> Unit,
    recipeStorage: MutableMap<String, List<RecipeIngredient>>
) {
    var selectedSize by remember { mutableStateOf("Medium") }
    
    // Key used to store/retrieve from local storage
    val recipeKey = "${product.name}-$selectedSize"
    
    // Ingredients state, initialized from storage or defaults
    val ingredients = remember(recipeKey) {
        val existing = recipeStorage[recipeKey]
        if (existing != null) {
            mutableStateListOf(*existing.toTypedArray())
        } else {
            mutableStateListOf(
                RecipeIngredient(product.name, "1", "pcs"),
                RecipeIngredient("Condensed", "50", "ml"),
                RecipeIngredient("Ice", "500", "g")
            )
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFFFFDE7),
                        modifier = Modifier.size(56.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFD54F))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(text = product.emoji, fontSize = 28.sp)
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = product.name,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF333333)
                        )
                        Text(text = "Create New Recipe", fontSize = 12.sp, color = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Size Selector
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SizeCard(
                        title = "Medium",
                        subtitle = "16 oz • P60",
                        isSelected = selectedSize == "Medium",
                        onClick = { selectedSize = "Medium" }
                    )
                    SizeCard(
                        title = "Large",
                        subtitle = "22 oz • P80",
                        isSelected = selectedSize == "Large",
                        onClick = { selectedSize = "Large" }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Ingredients Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Ingredients", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(
                        text = "+ Add ingredients",
                        color = Color(0xFF2E7D32),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { 
                            ingredients.add(RecipeIngredient("", "", "pcs"))
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Ingredients List
                ingredients.forEachIndexed { index, ingredient ->
                    IngredientRow(
                        ingredient = ingredient,
                        onRemove = { if(ingredients.size > 1) ingredients.removeAt(index) },
                        onUpdate = { updated -> ingredients[index] = updated }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Save Button
                Button(
                    onClick = {
                        // Persist to local "database"
                        recipeStorage[recipeKey] = ingredients.toList()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Save Changes", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun RowScope.SizeCard(title: String, subtitle: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.weight(1f).height(64.dp).clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) Color.White else Color.Transparent,
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) Color(0xFF2E7D32) else Color(0xFFDDDDDD)
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) Color(0xFF2E7D32) else Color.Gray
            )
            Text(text = subtitle, fontSize = 10.sp, color = Color.Gray)
        }
    }
}

@Composable
fun IngredientRow(
    ingredient: RecipeIngredient,
    onRemove: () -> Unit,
    onUpdate: (RecipeIngredient) -> Unit
) {
    var nameExpanded by remember { mutableStateOf(false) }
    var unitExpanded by remember { mutableStateOf(false) }

    val allIngredients = listOf(
        "Blueberry", "Mango", "Dragon Fruit", "Guyabano", "Strawberry",
        "Buko", "Avocado", "Melon", "Banana", "Apple", "Cheesecake",
        "Oreo", "Evap", "Condense", "Sugar", "Ice"
    )
    val allUnits = listOf("pcs", "ml", "g")

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Ingredient Name Dropdown
        Box(modifier = Modifier.weight(1.5f)) {
            OutlinedTextField(
                value = ingredient.name,
                onValueChange = { onUpdate(ingredient.copy(name = it)) },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                readOnly = true,
                shape = RoundedCornerShape(8.dp),
                trailingIcon = {
                    IconButton(onClick = { nameExpanded = true }) {
                        Icon(Icons.Default.ArrowDropDown, null)
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color(0xFFDDDDDD))
            )
            Box(modifier = Modifier.matchParentSize().clickable { nameExpanded = true })
            DropdownMenu(
                expanded = nameExpanded,
                onDismissRequest = { nameExpanded = false },
                modifier = Modifier.background(Color.White)
            ) {
                allIngredients.forEach { name ->
                    DropdownMenuItem(
                        text = { Text(name) },
                        onClick = {
                            onUpdate(ingredient.copy(name = name))
                            nameExpanded = false
                        }
                    )
                }
            }
        }

        // Quantity
        OutlinedTextField(
            value = ingredient.quantity,
            onValueChange = { onUpdate(ingredient.copy(quantity = it)) },
            modifier = Modifier.width(60.dp).height(48.dp),
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color(0xFFDDDDDD))
        )

        // Unit Dropdown
        Box(modifier = Modifier.width(80.dp)) {
            OutlinedTextField(
                value = ingredient.unit,
                onValueChange = { onUpdate(ingredient.copy(unit = it)) },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                readOnly = true,
                shape = RoundedCornerShape(8.dp),
                trailingIcon = {
                    IconButton(onClick = { unitExpanded = true }) {
                        Icon(Icons.Default.ArrowDropDown, null)
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color(0xFFDDDDDD))
            )
            Box(modifier = Modifier.matchParentSize().clickable { unitExpanded = true })
            DropdownMenu(
                expanded = unitExpanded,
                onDismissRequest = { unitExpanded = false },
                modifier = Modifier.background(Color.White)
            ) {
                allUnits.forEach { unit ->
                    DropdownMenuItem(
                        text = { Text(unit) },
                        onClick = {
                            onUpdate(ingredient.copy(unit = unit))
                            unitExpanded = false
                        }
                    )
                }
            }
        }

        IconButton(onClick = onRemove, modifier = Modifier.size(24.dp)) {
            Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Remove", tint = Color.Red)
        }
    }
}
