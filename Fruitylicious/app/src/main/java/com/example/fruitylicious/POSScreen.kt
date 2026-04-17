package com.example.fruitylicious

import androidx.compose.animation.*
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
import kotlinx.coroutines.launch

// ── Constants ─────────────────────────────────────────────────────────────────

val GreenPrimary = Color(0xFF2E7D32)
val GreenLight   = Color(0xFFE8F5E9)
val AmberAccent  = Color(0xFFFFC107)
val RedRemove    = Color(0xFFE53935)

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
    "Avocado","Mango","Dragon Fruit","Banana",
    "Guyabano","Buko","Apple","Strawberry","Melon","Cheesecake","Oreo"
)

val addOnChoices = listOf("Pearls", "Nata", "Cheese")

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
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .clickable { scope.launch { drawerState.open() } },
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    repeat(3) {
                        Box(
                            modifier = Modifier
                                .width(22.dp).height(2.dp)
                                .background(Color.White, RoundedCornerShape(1.dp))
                        )
                    }
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
                            text = "11 Flavors",
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
                        kotlinx.coroutines.delay(2000)
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
                        Text(
                            text = if (cartExpanded) "∧" else "∨",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
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
    var quantity         by remember { mutableStateOf(1) }

    val sizePrice = if (selectedSize == "Medium") product.basePrice else product.basePrice + 20
    val mixCost   = if (mixFlavor && selectedFlavor != null) 15 else 0
    val addOnCost = selectedAddOns.size * 10
    val itemTotal = (sizePrice + mixCost + addOnCost) * quantity

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
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
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1B1B1B)
                        )
                        Text(
                            text = "Customize your shake",
                            fontSize = 12.sp,
                            color = Color(0xFF888888)
                        )
                    }
                    Text(
                        text = "✕",
                        fontSize = 18.sp,
                        color = Color(0xFF888888),
                        modifier = Modifier.clickable { onDismiss() }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color(0xFFEEEEEE))
                Spacer(modifier = Modifier.height(14.dp))

                // ── Select Size ───────────────────────────────────────
                Text("Select Size", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1B1B1B))
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf("Medium" to product.basePrice, "Large" to product.basePrice + 20).forEach { (size, price) ->
                        val isSelected = selectedSize == size
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) GreenPrimary else Color(0xFFDDDDDD),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .background(if (isSelected) GreenPrimary else Color.White)
                                .clickable { selectedSize = size }
                                .padding(vertical = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = size,
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else Color(0xFF1B1B1B)
                                )
                                Text(
                                    text = "₱$price",
                                    fontSize = 12.sp,
                                    color = if (isSelected) Color.White.copy(alpha = 0.8f) else Color(0xFF888888)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color(0xFFEEEEEE))
                Spacer(modifier = Modifier.height(14.dp))

                // ── Mix Flavor ────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Mix Flavor", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1B1B1B))
                    if (mixFlavor && selectedFlavor != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(GreenLight)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text("+ ₱15", fontSize = 11.sp, color = GreenPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Checkbox row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFFDDDDDD), RoundedCornerShape(8.dp))
                        .clickable { mixFlavor = !mixFlavor; if (!mixFlavor) selectedFlavor = null }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (mixFlavor) GreenPrimary else Color.White)
                            .border(1.5.dp, if (mixFlavor) GreenPrimary else Color(0xFFAAAAAA), RoundedCornerShape(4.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (mixFlavor) Text("✓", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Add two-fruit combination", fontSize = 13.sp, color = Color(0xFF1B1B1B))
                }

                // Flavor dropdown (only when checked)
                if (mixFlavor) {
                    Spacer(modifier = Modifier.height(10.dp))

                    Box(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0xFFDDDDDD), RoundedCornerShape(8.dp))
                                .background(Color.White)
                                .clickable { dropdownExpanded = true }
                                .padding(horizontal = 14.dp, vertical = 14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = selectedFlavor ?: "Select second flavor...",
                                    fontSize = 13.sp,
                                    color = if (selectedFlavor != null) Color(0xFF1B1B1B) else Color(0xFFAAAAAA)
                                )
                                Text("∨", fontSize = 13.sp, color = Color(0xFF888888))
                            }
                        }

                        DropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.85f).background(Color.White)
                        ) {
                            flavorChoices.forEach { flavor ->
                                DropdownMenuItem(
                                    text = { Text(flavor, fontSize = 13.sp) },
                                    onClick = {
                                        selectedFlavor = flavor
                                        dropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color(0xFFEEEEEE))
                Spacer(modifier = Modifier.height(14.dp))

                // ── Add-ons ───────────────────────────────────────────
                Text("Add-ons", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1B1B1B))
                Spacer(modifier = Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    addOnChoices.forEach { addOn ->
                        val isSelected = addOn in selectedAddOns
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) GreenPrimary else Color.White)
                                .border(1.dp, if (isSelected) GreenPrimary else Color(0xFFDDDDDD), RoundedCornerShape(8.dp))
                                .clickable {
                                    selectedAddOns = if (isSelected) selectedAddOns - addOn else selectedAddOns + addOn
                                }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = addOn,
                                    fontSize = 12.sp,
                                    color = if (isSelected) Color.White else Color(0xFF1B1B1B),
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                                Text(
                                    text = "+ ₱10",
                                    fontSize = 10.sp,
                                    color = if (isSelected) Color.White.copy(alpha = 0.8f) else Color(0xFF888888)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color(0xFFEEEEEE))
                Spacer(modifier = Modifier.height(14.dp))

                // ── Quantity ──────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Quantity", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1B1B1B))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .border(1.dp, Color(0xFFDDDDDD), CircleShape)
                                .clickable { if (quantity > 1) quantity-- },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("−", fontSize = 18.sp, color = Color(0xFF333333))
                        }
                        Text(
                            text = "$quantity",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1B1B1B)
                        )
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .border(1.dp, Color(0xFFDDDDDD), CircleShape)
                                .clickable { quantity++ },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("+", fontSize = 18.sp, color = Color(0xFF333333))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // ── Footer: Total + Add to Order ──────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Item Total", fontSize = 11.sp, color = Color(0xFF888888))
                        Text(
                            text = "₱${"%.2f".format(itemTotal.toDouble())}",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
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
                                    quantity  = quantity
                                )
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text("⊕", fontSize = 16.sp, color = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add to Order", fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Bold)
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
