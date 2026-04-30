package com.example.fruitylicious.ui.staff.pos

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

// ── Constants ─────────────────────────────────────────────────────────────────

val GreenPrimary = Color(0xFF2E7D32)
val GreenLight   = Color(0xFFE8F5E9)
val AmberAccent  = Color(0xFFFFC107)
val RedRemove    = Color(0xFFE53935)
val CheckboxBlue = Color(0xFF2196F3)

// ── Data ──────────────────────────────────────────────────────────────────────

data class POSProduct(
    val name: String,
    val emoji: String,
    val basePrice: Int
)

data class CartItem(
    val product: POSProduct,
    val size: String,
    val sizePrice: Int,
    val mixFlavor: String?,
    val addOns: List<String>,
    val optionalAddOns: List<String> = emptyList(),
    var quantity: Int
) {
    val total: Int get() {
        val addOnCost = addOns.size * 10
        val mixCost   = if (mixFlavor != null) 15 else 0
        return (sizePrice + addOnCost + mixCost) * quantity
    }
}

val posProducts = listOf(
    POSProduct("Avocado",     "🥑", 60),
    POSProduct("Mango",       "🥭", 60),
    POSProduct("Dragon Fruit","🐉", 65),
    POSProduct("Banana",      "🍌", 55),
    POSProduct("Guyabano",    "🍈", 60),
    POSProduct("Buko",        "🥥", 55),
    POSProduct("Apple",       "🍎", 60),
    POSProduct("Strawberry",  "🍓", 65),
    POSProduct("Melon",       "🍈", 55),
    POSProduct("Cheesecake",  "🍰", 75),
    POSProduct("Oreo",        "🍪", 70),
)

val flavorChoices = listOf(
    "Avocado", "Mango", "Dragon Fruit", "Banana",
    "Guyabano", "Buko", "Apple", "Strawberry", "Melon", "Cheesecake", "Oreo"
)

val addOnChoices = listOf("Pearls", "Nata", "Cheese")

val optionalAddOnChoices = listOf(
    "Crashed Graham", "Crashed Oreo", "Chocolate Syrup",
    "Strawberry Syrup", "Caramel Syrup", "Mango Syrup"
)

// ── Screen ────────────────────────────────────────────────────────────────────

@Composable
fun POSScreen(
    navController: NavController,
    drawerState: DrawerState,
    scope: CoroutineScope
) {
    var selectedProduct  by remember { mutableStateOf<POSProduct?>(null) }
    var cartExpanded     by remember { mutableStateOf(false) }
    var cartItems        by remember { mutableStateOf(listOf<CartItem>()) }
    var addedToCartMsg   by remember { mutableStateOf(false) }

    val cartTotal = cartItems.sumOf { it.total }
    val cartCount = cartItems.sumOf { it.quantity }

    // ── Product Customize Dialog ──────────────────────────────────────────────
    selectedProduct?.let { product ->
        ProductCustomizeDialog(
            product = product,
            onDismiss = { selectedProduct = null },
            onAddToCart = { item ->
                cartItems = cartItems + item
                selectedProduct = null
                addedToCartMsg = true
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFFFEAA0))) {

        Column(modifier = Modifier.fillMaxSize()) {

            // ── Green Header ──────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(GreenPrimary)
                    .padding(start = 20.dp, end = 20.dp, top = 48.dp, bottom = 16.dp)
            ) {
                // Hamburger
                IconButton(
                    onClick = { scope.launch { drawerState.open() } },
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Icon(Icons.Default.Menu, contentDescription = null, tint = Color.White)
                }
                Text(
                    text = "POS",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            // ── Product Grid ──────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Select Product",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1B1B1B)
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(AmberAccent)
                            .padding(horizontal = 12.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "${posProducts.size} Flavors",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(posProducts) { product ->
                        ProductGridItem(
                            product = product,
                            onClick = { selectedProduct = product }
                        )
                    }
                }
            }

            // ── Cart Bar ──────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp))
            ) {
                // "added to cart" message
                AnimatedVisibility(visible = addedToCartMsg) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFEEEEEE))
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "added to cart",
                            color = Color(0xFF888888),
                            fontSize = 13.sp
                        )
                    }
                    LaunchedEffect(addedToCartMsg) {
                        delay(2000)
                        addedToCartMsg = false
                    }
                }

                // Cart toggle bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(GreenPrimary)
                        .clickable { cartExpanded = !cartExpanded }
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🛒", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Cart",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (cartCount > 0) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(AmberAccent),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$cartCount",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = "₱ ${"%,.2f".format(cartTotal.toDouble())}",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = if (cartExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                }

                // Expanded cart panel
                AnimatedVisibility(
                    visible = cartExpanded,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    CartPanel(
                        cartItems = cartItems,
                        onQuantityChange = { item, delta ->
                            cartItems = cartItems.map {
                                if (it == item) it.copy(quantity = (it.quantity + delta).coerceAtLeast(1))
                                else it
                            }
                        },
                        onRemove = { item ->
                            cartItems = cartItems - item
                        },
                        onClear = { cartItems = emptyList() },
                        onCheckout = { 
                            navController.navigate("checkout")
                        }
                    )
                }
            }
        }
    }
}

// ── Product Grid Item ─────────────────────────────────────────────────────────

@Composable
fun ProductGridItem(product: POSProduct, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .aspectRatio(1f)
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = product.emoji, fontSize = 36.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = product.name,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF1B1B1B),
                textAlign = TextAlign.Center
            )
        }
    }
}

// ── Product Customize Dialog ──────────────────────────────────────────────────

@Composable
fun ProductCustomizeDialog(
    product: POSProduct,
    onDismiss: () -> Unit,
    onAddToCart: (CartItem) -> Unit
) {
    var selectedSize     by remember { mutableStateOf("Medium") }
    var mixFlavor        by remember { mutableStateOf(false) }
    var selectedFlavor   by remember { mutableStateOf<String?>(null) }
    var dropdownExpanded by remember { mutableStateOf(false) }
    var selectedAddOns   by remember { mutableStateOf(setOf<String>()) }
    var selectedOptionalAddOns by remember { mutableStateOf(setOf<String>()) }
    var quantity         by remember { mutableStateOf(1) }

    val sizePrice = if (selectedSize == "Medium") product.basePrice else product.basePrice + 20
    val mixCost   = if (mixFlavor && selectedFlavor != null) 15 else 0
    val addOnCost = selectedAddOns.size * 10
    val itemTotal = (sizePrice + mixCost + addOnCost) * quantity

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {

                // ── Title row ─────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(
                            text = product.name,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1B1B1B)
                        )
                        Text(
                            text = "Customize your shake",
                            fontSize = 14.sp,
                            color = Color(0xFF888888)
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

                // ── Select Size ───────────────────────────────────────
                Text("Select Size", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1B1B1B))
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    listOf("Medium" to product.basePrice, "Large" to product.basePrice + 20).forEach { (size, price) ->
                        val isSelected = selectedSize == size
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) GreenPrimary else Color(0xFFEEEEEE),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .background(Color.White)
                                .clickable { selectedSize = size }
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = size,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) GreenPrimary else Color(0xFF1B1B1B)
                                )
                                Text(
                                    text = "₱ $price",
                                    fontSize = 13.sp,
                                    color = if (isSelected) GreenPrimary else Color(0xFF888888)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // ── Mix Flavor ────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Mix Flavor", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1B1B1B))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFF0F0F0))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("+ ₱ 15", fontSize = 12.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(12.dp))
                        .clickable { mixFlavor = !mixFlavor; if (!mixFlavor) selectedFlavor = null }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = mixFlavor,
                        onCheckedChange = { mixFlavor = it; if (!it) selectedFlavor = null },
                        colors = CheckboxDefaults.colors(checkedColor = CheckboxBlue)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add two-fruit combination", fontSize = 14.sp, color = Color(0xFF1B1B1B))
                }

                if (mixFlavor) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedCard(
                            onClick = { dropdownExpanded = true },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFFEEEEEE)),
                            colors = CardDefaults.outlinedCardColors(containerColor = Color.White),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = selectedFlavor ?: "Select flavor",
                                    fontSize = 14.sp,
                                    color = if (selectedFlavor != null) Color.Black else Color.Gray
                                )
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.Gray)
                            }
                        }

                        DropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.8f).background(Color.White)
                        ) {
                            flavorChoices.forEach { flavor ->
                                DropdownMenuItem(
                                    text = { Text(flavor) },
                                    onClick = {
                                        selectedFlavor = flavor
                                        dropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // ── Add-ons ───────────────────────────────────────────
                Text("Add-ons", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1B1B1B))
                Spacer(modifier = Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    addOnChoices.forEach { addOn ->
                        val isSelected = addOn in selectedAddOns
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) GreenPrimary else Color.White)
                                .border(1.dp, if (isSelected) GreenPrimary else Color(0xFFEEEEEE), RoundedCornerShape(12.dp))
                                .clickable {
                                    selectedAddOns = if (isSelected) selectedAddOns - addOn else selectedAddOns + addOn
                                }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = addOn,
                                    fontSize = 13.sp,
                                    color = if (isSelected) Color.White else Color(0xFF1B1B1B),
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                                Text(
                                    text = "+ ₱ 10",
                                    fontSize = 11.sp,
                                    color = if (isSelected) Color.White.copy(alpha = 0.8f) else Color.Gray
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // ── Add-ons (optional) ────────────────────────────────
                Text("Add-ons (optional)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1B1B1B))
                Spacer(modifier = Modifier.height(12.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    optionalAddOnChoices.chunked(3).forEach { rowItems ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            rowItems.forEach { addOn ->
                                val isSelected = addOn in selectedOptionalAddOns
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(20.dp))
                                        .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(20.dp))
                                        .background(if (isSelected) GreenPrimary.copy(alpha = 0.1f) else Color.White)
                                        .clickable {
                                            selectedOptionalAddOns = if (isSelected) selectedOptionalAddOns - addOn else selectedOptionalAddOns + addOn
                                        }
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = addOn,
                                        fontSize = 11.sp,
                                        color = if (isSelected) GreenPrimary else Color.Black,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                            if (rowItems.size < 3) {
                                repeat(3 - rowItems.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(color = Color(0xFFEEEEEE))
                Spacer(modifier = Modifier.height(24.dp))

                // ── Quantity ──────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Quantity", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1B1B1B))

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF5F5F5))
                            .padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = { if (quantity > 1) quantity-- },
                            modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(Color.White)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                        Text(
                            text = "$quantity",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        IconButton(
                            onClick = { quantity++ },
                            modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(Color.White)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // ── Footer ────────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Item Total", fontSize = 12.sp, color = Color.Gray)
                        Text(
                            text = "₱${String.format(Locale.US, "%.2f", itemTotal.toDouble())}",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = GreenPrimary
                        )
                    }

                    Button(
                        onClick = {
                            onAddToCart(
                                CartItem(
                                    product   = product,
                                    size      = selectedSize,
                                    sizePrice = sizePrice,
                                    mixFlavor = if (mixFlavor && selectedFlavor != null) selectedFlavor else null,
                                    addOns    = selectedAddOns.toList(),
                                    optionalAddOns = selectedOptionalAddOns.toList(),
                                    quantity  = quantity
                                )
                            )
                        },
                        modifier = Modifier.height(54.dp).weight(1f).padding(start = 24.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add to Order", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ── Cart Panel ────────────────────────────────────────────────────────────────

@Composable
fun CartPanel(
    cartItems: List<CartItem>,
    onQuantityChange: (CartItem, Int) -> Unit,
    onRemove: (CartItem) -> Unit,
    onClear: () -> Unit,
    onCheckout: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 8.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            if (cartItems.isEmpty()) {
                // Empty state
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("🛒", fontSize = 36.sp, color = Color(0xFFCCCCCC))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Cart is empty",
                        fontSize = 13.sp,
                        color = Color(0xFFAAAAAA)
                    )
                }
            } else {
                // Cart items
                cartItems.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row {
                                Text(
                                    text = item.product.name,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1B1B1B)
                                )
                                if (item.mixFlavor != null) {
                                    Text(
                                        text = " + ${item.mixFlavor}",
                                        fontSize = 14.sp,
                                        color = AmberAccent,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Text(
                                text = buildString {
                                    append(item.size)
                                    if (item.addOns.isNotEmpty()) append(" • ${item.addOns.joinToString(", ")}")
                                    if (item.optionalAddOns.isNotEmpty()) append(" • ${item.optionalAddOns.joinToString(", ")}")
                                },
                                fontSize = 12.sp,
                                color = Color(0xFF888888)
                            )
                        }

                        // Qty controls
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .border(1.dp, Color(0xFFDDDDDD), CircleShape)
                                    .clickable { onQuantityChange(item, -1) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("−", fontSize = 14.sp, color = Color(0xFF333333))
                            }
                            Text("${item.quantity}", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .border(1.dp, Color(0xFFDDDDDD), CircleShape)
                                    .clickable { onQuantityChange(item, 1) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("+", fontSize = 14.sp, color = Color(0xFF333333))
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Price + remove
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "₱ ${"%,.2f".format(item.total.toDouble())}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = GreenPrimary
                            )
                            Text(
                                text = "Remove",
                                fontSize = 11.sp,
                                color = RedRemove,
                                modifier = Modifier.clickable { onRemove(item) }
                            )
                        }
                    }
                    HorizontalDivider(color = Color(0xFFEEEEEE))
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Clear + Checkout
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onClear,
                        modifier = Modifier.weight(1f).height(46.dp),
                        shape = RoundedCornerShape(10.dp),
                        border = ButtonDefaults.outlinedButtonBorder
                    ) {
                        Text("🗑", fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Clear", fontSize = 14.sp, color = Color(0xFF333333))
                    }
                    Button(
                        onClick = onCheckout,
                        modifier = Modifier.weight(1f).height(46.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
                    ) {
                        Text("Checkout", fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ── Preview ───────────────────────────────────────────────────────────────────

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun POSScreenPreview() {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    POSScreen(
        navController = rememberNavController(),
        drawerState = drawerState,
        scope = scope
    )
}
