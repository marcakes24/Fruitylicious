package com.example.fruitylicious.ui.staff.pos

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.fruitylicious.STAFF_CHECKOUT
import com.example.fruitylicious.data.local.entity.IngredientEntity
import com.example.fruitylicious.data.local.entity.InventoryEntity
import com.example.fruitylicious.data.local.entity.ProductEntity
import com.example.fruitylicious.data.local.entity.ProductRecipeEntity
import com.example.fruitylicious.data.local.entity.ProductVariantEntity
import com.example.fruitylicious.data.repository.CartItem
import com.example.fruitylicious.util.ImageStorage
import java.util.Locale
import kotlinx.coroutines.delay

private val GreenPrimary = Color(0xFF2E7D32)
private val AmberAccent = Color(0xFFFFC107)
private val RedRemove = Color(0xFFE53935)
private val CheckboxBlue = Color(0xFF2196F3)
private val PageBg = Color(0xFFFFEAA0)

@Composable
fun PosScreen(
    onNavigate: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: PosViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var selectedProduct by remember { mutableStateOf<ProductEntity?>(null) }
    var cartExpanded by remember { mutableStateOf(false) }
    var addedToCartMsg by remember { mutableStateOf(false) }

    val cartItems = uiState.cartItems
    val cartTotal = uiState.totalAmount
    val cartCount = cartItems.sumOf { it.quantity }

    selectedProduct?.let { product ->
        val variants = uiState.variantsByProductId[product.productId].orEmpty()

        ProductCustomizeDialog(
            product = product,
            variants = variants,
            mainProducts = uiState.products,
            addons = uiState.addons,
            recipes = uiState.recipes,
            inventory = uiState.inventory,
            ingredients = uiState.ingredients,
            onDismiss = {
                selectedProduct = null
            },
            onAddToCart = { variant, mixAddon, selectedAddons, quantity ->
                viewModel.addCustomizedItem(
                    product = product,
                    variant = variant,
                    mixAddon = mixAddon,
                    selectedAddons = selectedAddons,
                    quantity = quantity
                )

                selectedProduct = null
                addedToCartMsg = true
            }
        )
    }

    if (!uiState.isClockedIn) {
        Dialog(onDismissRequest = { onBack() }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(text = "🕒", fontSize = 48.sp)

                    Text(
                        text = "Not Clocked In",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = uiState.error ?: "You must clock in before using the POS.",
                        textAlign = TextAlign.Center,
                        color = Color.Gray
                    )

                    Button(
                        onClick = { onBack() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Go Back")
                    }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PageBg)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            PosHeader(onBack = onBack)

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
                            text = "${uiState.products.size} Products",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (uiState.products.isEmpty() && !uiState.isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No products found.\nAdd products first in admin.",
                            textAlign = TextAlign.Center,
                            color = Color.Gray
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(
                            items = uiState.products,
                            key = { it.productId }
                        ) { product ->
                            ProductGridItem(
                                product = product,
                                onClick = {
                                    selectedProduct = product
                                }
                            )
                        }
                    }
                }
            }

            CartBottomSection(
                addedToCartMsg = addedToCartMsg,
                onAddedMessageConsumed = {
                    addedToCartMsg = false
                },
                cartExpanded = cartExpanded,
                onCartToggle = {
                    cartExpanded = !cartExpanded
                },
                cartCount = cartCount,
                cartTotal = cartTotal,
                cartItems = cartItems,
                onQuantityChange = { item, delta ->
                    viewModel.updateQuantity(item.cartLineId, delta)
                },
                onRemove = { item ->
                    viewModel.removeItem(item.cartLineId)
                },
                onClear = {
                    viewModel.clearCart()
                },
                onCheckout = {
                    onNavigate(STAFF_CHECKOUT)
                }
            )
        }
    }
}

@Composable
private fun PosHeader(
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(GreenPrimary)
            .padding(start = 20.dp, end = 20.dp, top = 48.dp, bottom = 16.dp)
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White
            )
        }

        Text(
            text = "POS",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
private fun ProductGridItem(
    product: ProductEntity,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .aspectRatio(1f)
            .clickable {
                onClick()
            },
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            ProductIconImage(
                product = product,
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = product.productName,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF1B1B1B),
                textAlign = TextAlign.Center,
                maxLines = 2
            )
        }
    }
}

@Composable
private fun ProductIconImage(
    product: ProductEntity,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val imageFile = product.image
        ?.takeIf { it.isNotBlank() }
        ?.let { imagePath ->
            ImageStorage.getImageFile(
                context = context,
                relativePath = imagePath
            )
        }

    Box(
        modifier = modifier.background(Color(0xFFE8F5E9)),
        contentAlignment = Alignment.Center
    ) {
        if (imageFile != null && imageFile.exists()) {
            AsyncImage(
                model = imageFile,
                contentDescription = product.productName,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(
                imageVector = Icons.Default.Image,
                contentDescription = product.productName,
                tint = GreenPrimary,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
private fun ProductCustomizeDialog(
    product: ProductEntity,
    variants: List<ProductVariantEntity>,
    mainProducts: List<ProductEntity>,
    addons: List<ProductEntity>,
    recipes: List<ProductRecipeEntity>,
    inventory: List<InventoryEntity>,
    ingredients: List<IngredientEntity>,
    onDismiss: () -> Unit,
    onAddToCart: (
        variant: ProductVariantEntity,
        mixAddon: ProductEntity?,
        selectedAddons: List<ProductEntity>,
        quantity: Int
    ) -> Unit
) {
    var selectedVariant by remember(variants) {
        mutableStateOf(variants.firstOrNull())
    }

    var mixFlavor by remember { mutableStateOf(false) }
    var selectedFlavor by remember { mutableStateOf<ProductEntity?>(null) }
    var dropdownExpanded by remember { mutableStateOf(false) }
    var dropdownWidth by remember { mutableStateOf(0.dp) }
    var selectedAddOns by remember { mutableStateOf(setOf<ProductEntity>()) }
    var quantity by remember { mutableStateOf(1) }

    val density = LocalDensity.current

    val variant = selectedVariant

    val mixCost = if (mixFlavor && selectedFlavor != null) 15.0 else 0.0

    val addOnCost = selectedAddOns.sumOf { it.price }
    val basePrice = variant?.price ?: 0.0
    val itemTotal = (basePrice + mixCost + addOnCost) * quantity

    val availableMixFlavors = mainProducts.filter { it.productId != product.productId }
    val availableAddons = addons.filter { it.productId != selectedFlavor?.productId }

    val variantRecipe = recipes.filter {
        it.productId == product.productId && it.variantId == selectedVariant?.variantId
    }

    val productRecipe = recipes.filter {
        it.productId == product.productId && it.variantId == null
    }

    val activeRecipe = if (variantRecipe.isNotEmpty()) {
        variantRecipe
    } else {
        productRecipe
    }

    val selectedAddonRecipes = recipes.filter { recipe ->
        selectedAddOns.any {
            it.productId == recipe.productId
        } && recipe.variantId == null &&
        ingredients.find { it.ingredientId == recipe.ingredientId }?.isPackaging != true
    }

    val hasRecipe = activeRecipe.isNotEmpty()

    val insufficientIngredients = remember(
        selectedVariant,
        selectedFlavor,
        mixFlavor,
        selectedAddOns,
        quantity,
        recipes,
        inventory,
        ingredients
    ) {
        val requirements = mutableMapOf<Int, Double>()

        activeRecipe.forEach { line ->
            requirements[line.ingredientId] =
                (requirements[line.ingredientId] ?: 0.0) + (line.quantityRequired * quantity)
        }

        if (mixFlavor && selectedFlavor != null) {
            val ingredientName = getMixIngredientName(selectedFlavor!!.productName)
            val ingredient = ingredients.find { it.ingredientName == ingredientName }
            if (ingredient != null) {
                val amount = getMixIngredientQuantity(selectedVariant?.sizeName ?: "Medium") * quantity
                requirements[ingredient.ingredientId] =
                    (requirements[ingredient.ingredientId] ?: 0.0) + amount
            }
        }

        selectedAddonRecipes.forEach { line ->
            requirements[line.ingredientId] =
                (requirements[line.ingredientId] ?: 0.0) + (line.quantityRequired * quantity)
        }

        requirements.filter { (ingredientId, required) ->
            val stockItem = inventory.find {
                it.ingredientId == ingredientId
            }

            val ingredient = ingredients.find {
                it.ingredientId == ingredientId
            }

            val available = if (stockItem != null && ingredient != null) {
                val unit = ingredient.unitType.lowercase(Locale.US)
                val weight = ingredient.estimatedWeightPerUnit
                
                val needsConversion = unit == "pcs" || unit == "can" || unit == "pack" || 
                                     unit == "kg" || unit == "unit" || unit == "units" ||
                                     unit == "bottle" || unit == "tub"

                if (needsConversion) {
                    if (weight > 0.0) {
                        stockItem.currentStock * weight
                    } else if (unit == "kg") {
                        stockItem.currentStock * 1000.0
                    } else {
                        stockItem.currentStock
                    }
                } else {
                    stockItem.currentStock
                }
            } else {
                0.0
            }

            available < required
        }
    }

    val isStockAvailable = insufficientIngredients.isEmpty()

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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(
                            text = product.productName,
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

                Text(
                    text = "Select Size",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1B1B1B)
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (variants.isEmpty()) {
                    Text(
                        text = "No sizes available for this product.",
                        color = Color.Red,
                        fontSize = 13.sp
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        variants.forEach { item ->
                            val isSelected = selectedVariant?.variantId == item.variantId

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
                                    .clickable {
                                        selectedVariant = item
                                    }
                                    .padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = item.sizeName,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) GreenPrimary else Color(0xFF1B1B1B)
                                    )

                                    Text(
                                        text = "₱${String.format(Locale.US, "%,.2f", item.price)}",
                                        fontSize = 13.sp,
                                        color = if (isSelected) GreenPrimary else Color(0xFF888888)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Mix Flavor",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1B1B1B)
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFF0F0F0))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "+₱15",
                            fontSize = 12.sp,
                            color = Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(12.dp))
                        .clickable {
                            mixFlavor = !mixFlavor
                            if (!mixFlavor) {
                                selectedFlavor = null
                            }
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = mixFlavor,
                        onCheckedChange = {
                            mixFlavor = it
                            if (!it) {
                                selectedFlavor = null
                            }
                        },
                        colors = CheckboxDefaults.colors(checkedColor = CheckboxBlue)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "Add two-fruit combination",
                        fontSize = 14.sp,
                        color = Color(0xFF1B1B1B)
                    )
                }

                if (mixFlavor) {
                    Spacer(modifier = Modifier.height(12.dp))

                    Box(modifier = Modifier.fillMaxWidth()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .onGloballyPositioned { coordinates ->
                                    dropdownWidth = with(density) { coordinates.size.width.toDp() }
                                }
                                .clickable {
                                    dropdownExpanded = true
                                },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFFEEEEEE)),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = selectedFlavor?.productName ?: "Select fruit add-on",
                                    fontSize = 14.sp,
                                    color = if (selectedFlavor != null) {
                                        Color.Black
                                    } else {
                                        Color.Gray
                                    }
                                )

                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = Color.Gray
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = {
                                dropdownExpanded = false
                            },
                            modifier = Modifier
                                .width(dropdownWidth)
                                .background(Color.White)
                        ) {
                            if (availableMixFlavors.isEmpty()) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "No other fruits available",
                                            color = Color.Gray
                                        )
                                    },
                                    onClick = {
                                        dropdownExpanded = false
                                    }
                                )
                            } else {
                                availableMixFlavors.forEach { addon ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(addon.productName)
                                        },
                                        onClick = {
                                            selectedFlavor = addon
                                            selectedAddOns = selectedAddOns
                                                .filterNot {
                                                    it.productId == addon.productId
                                                }
                                                .toSet()
                                            dropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Add-ons",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1B1B1B)
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (availableAddons.isEmpty()) {
                    Text(
                        text = "No add-ons available.",
                        color = Color.Gray,
                        fontSize = 13.sp
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        availableAddons.chunked(3).forEach { rowItems ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                rowItems.forEach { addon ->
                                    val isSelected = addon in selectedAddOns

                                    val addonRecipe = recipes.filter {
                                        it.productId == addon.productId && it.variantId == null
                                    }

                                    val hasAddonRecipe = addonRecipe.isNotEmpty()

                                    val isAddonStockAvailable = remember(
                                        addon,
                                        quantity,
                                        recipes,
                                        inventory,
                                        ingredients
                                    ) {
                                        if (!hasAddonRecipe) {
                                            return@remember false
                                        }

                                        addonRecipe.all { line ->
                                            val required = line.quantityRequired * quantity

                                            val stockItem = inventory.find {
                                                it.ingredientId == line.ingredientId
                                            }

                                            val ingredient = ingredients.find {
                                                it.ingredientId == line.ingredientId
                                            }

                                            val available = if (stockItem != null && ingredient != null) {
                                                val unit = ingredient.unitType.lowercase(Locale.US)
                                                val weight = ingredient.estimatedWeightPerUnit

                                                val needsConversion = unit == "pcs" || unit == "can" || unit == "pack" || 
                                                                     unit == "kg" || unit == "unit" || unit == "units" ||
                                                                     unit == "bottle" || unit == "tub"

                                                if (needsConversion) {
                                                    if (weight > 0.0) {
                                                        stockItem.currentStock * weight
                                                    } else if (unit == "kg") {
                                                        stockItem.currentStock * 1000.0
                                                    } else {
                                                        stockItem.currentStock
                                                    }
                                                } else {
                                                    stockItem.currentStock
                                                }
                                            } else {
                                                0.0
                                            }

                                            available >= required
                                        }
                                    }

                                    val isAddonSelectable = hasAddonRecipe && isAddonStockAvailable

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                when {
                                                    isSelected -> GreenPrimary
                                                    !isAddonSelectable -> Color(0xFFF5F5F5)
                                                    else -> Color.White
                                                }
                                            )
                                            .border(
                                                1.dp,
                                                when {
                                                    isSelected -> GreenPrimary
                                                    !isAddonSelectable -> Color(0xFFE0E0E0)
                                                    else -> Color(0xFFEEEEEE)
                                                },
                                                RoundedCornerShape(12.dp)
                                            )
                                            .clickable(enabled = isAddonSelectable || isSelected) {
                                                selectedAddOns = if (isSelected) {
                                                    selectedAddOns - addon
                                                } else {
                                                    selectedAddOns + addon
                                                }
                                            }
                                            .padding(horizontal = 8.dp, vertical = 12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = addon.productName,
                                                fontSize = 12.sp,
                                                color = when {
                                                    isSelected -> Color.White
                                                    !isAddonSelectable -> Color.LightGray
                                                    else -> Color(0xFF1B1B1B)
                                                },
                                                fontWeight = if (isSelected) {
                                                    FontWeight.Bold
                                                } else {
                                                    FontWeight.Medium
                                                },
                                                textAlign = TextAlign.Center
                                            )

                                            Text(
                                                text = when {
                                                    !hasAddonRecipe -> "No Recipe"
                                                    !isAddonStockAvailable -> "Out of Stock"
                                                    else -> "+ ₱${String.format(Locale.US, "%,.2f", addon.price)}"
                                                },
                                                fontSize = 10.sp,
                                                color = when {
                                                    isSelected -> Color.White.copy(alpha = 0.8f)
                                                    !isAddonSelectable -> Color.Red.copy(alpha = 0.6f)
                                                    else -> Color.Gray
                                                }
                                            )
                                        }
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
                }

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(color = Color(0xFFEEEEEE))
                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Quantity",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1B1B1B)
                    )

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF5F5F5))
                            .padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = {
                                if (quantity > 1) {
                                    quantity--
                                }
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Remove,
                                contentDescription = "Decrease",
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Text(
                            text = quantity.toString(),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        IconButton(
                            onClick = {
                                quantity++
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Increase",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (!hasRecipe) {
                    Text(
                        text = "⚠️ No recipe defined for this size.",
                        color = Color.Red,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                } else if (!isStockAvailable) {
                    val ingredientNames = insufficientIngredients.keys
                        .map { id ->
                            ingredients.find {
                                it.ingredientId == id
                            }?.ingredientName ?: "ID: $id"
                        }
                        .joinToString(", ")

                    Text(
                        text = "⚠️ Insufficient stock for: $ingredientNames",
                        color = Color.Red,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Item Total",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )

                        Text(
                            text = "₱${String.format(Locale.US, "%,.2f", itemTotal)}",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (hasRecipe && isStockAvailable) {
                                GreenPrimary
                            } else {
                                Color.Gray
                            }
                        )
                    }

                    Button(
                        onClick = {
                            if (variant != null) {
                                onAddToCart(
                                    variant,
                                    if (mixFlavor) {
                                        selectedFlavor
                                    } else {
                                        null
                                    },
                                    selectedAddOns.toList(),
                                    quantity
                                )
                            }
                        },
                        enabled = variant != null && hasRecipe && isStockAvailable,
                        modifier = Modifier
                            .height(54.dp)
                            .weight(1f)
                            .padding(start = 24.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (hasRecipe && isStockAvailable) {
                                GreenPrimary
                            } else {
                                Color.LightGray
                            }
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = "Add to Order",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CartBottomSection(
    addedToCartMsg: Boolean,
    onAddedMessageConsumed: () -> Unit,
    cartExpanded: Boolean,
    onCartToggle: () -> Unit,
    cartCount: Int,
    cartTotal: Double,
    cartItems: List<CartItem>,
    onQuantityChange: (CartItem, Int) -> Unit,
    onRemove: (CartItem) -> Unit,
    onClear: () -> Unit,
    onCheckout: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        AnimatedVisibility(visible = addedToCartMsg) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFEEEEEE))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Added to cart",
                    color = Color(0xFF888888),
                    fontSize = 13.sp
                )
            }

            LaunchedEffect(addedToCartMsg) {
                delay(2000)
                onAddedMessageConsumed()
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(GreenPrimary)
                .clickable {
                    onCartToggle()
                }
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "🛒", fontSize = 18.sp)

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
                            text = cartCount.toString(),
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = "₱ ${String.format(Locale.US, "%,.2f", cartTotal)}",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.width(8.dp))

                Icon(
                    imageVector = if (cartExpanded) {
                        Icons.Default.KeyboardArrowDown
                    } else {
                        Icons.Default.KeyboardArrowUp
                    },
                    contentDescription = null,
                    tint = Color.White
                )
            }
        }

        AnimatedVisibility(
            visible = cartExpanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            CartPanel(
                cartItems = cartItems,
                onQuantityChange = onQuantityChange,
                onRemove = onRemove,
                onClear = onClear,
                onCheckout = onCheckout
            )
        }
    }
}

private fun getMixIngredientName(productName: String): String {
    return when (productName) {
        "Cheesecake Shake" -> "Lemon Square Cheesecake"
        "Oreo Shake" -> "Oreo"
        else -> productName.removeSuffix(" Shake")
    }
}

private fun getMixIngredientQuantity(sizeName: String): Double {
    return if (sizeName.equals("Large", ignoreCase = true)) {
        220.0
    } else {
        150.0
    }
}

@Composable
private fun CartPanel(
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
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🛒",
                        fontSize = 36.sp,
                        color = Color(0xFFCCCCCC)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Cart is empty",
                        fontSize = 13.sp,
                        color = Color(0xFFAAAAAA)
                    )
                }
            } else {
                cartItems.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.productName,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1B1B1B)
                            )

                            Text(
                                text = buildString {
                                    append(item.sizeName)

                                    if (item.addons.isNotEmpty()) {
                                        append(" • ")
                                        append(
                                            item.addons.joinToString(", ") { addon ->
                                                addon.addonName
                                            }
                                        )
                                    }
                                },
                                fontSize = 12.sp,
                                color = Color(0xFF888888)
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .border(1.dp, Color(0xFFDDDDDD), CircleShape)
                                    .clickable {
                                        onQuantityChange(item, -1)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "−",
                                    fontSize = 14.sp,
                                    color = Color(0xFF333333)
                                )
                            }

                            Text(
                                text = item.quantity.toString(),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .border(1.dp, Color(0xFFDDDDDD), CircleShape)
                                    .clickable {
                                        onQuantityChange(item, 1)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "+",
                                    fontSize = 14.sp,
                                    color = Color(0xFF333333)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "₱ ${String.format(Locale.US, "%,.2f", item.subtotal)}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = GreenPrimary
                            )

                            TextButton(
                                onClick = {
                                    onRemove(item)
                                },
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(
                                    text = "Remove",
                                    fontSize = 11.sp,
                                    color = RedRemove
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = Color(0xFFEEEEEE))
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onClear,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "Clear",
                            fontSize = 14.sp,
                            color = Color(0xFF333333)
                        )
                    }

                    Button(
                        onClick = onCheckout,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        enabled = cartItems.isNotEmpty(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
                    ) {
                        Text(
                            text = "Checkout",
                            fontSize = 14.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}