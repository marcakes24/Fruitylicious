package com.example.fruitylicious.ui.admin.recipes

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
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.RestaurantMenu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.fruitylicious.data.local.entity.IngredientEntity
import com.example.fruitylicious.data.local.entity.ProductEntity
import com.example.fruitylicious.data.local.entity.ProductVariantEntity
import com.example.fruitylicious.ui.shared.OwnerSideBarContent
import kotlinx.coroutines.launch
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import com.example.fruitylicious.util.ImageStorage
import androidx.compose.ui.layout.ContentScale

private val RmGreen = Color(0xFF2E7D32)
private val RmPageBg = Color(0xFFFFEAA0)
private val RmTextMain = Color(0xFF1A1A1A)
private val RmTextSub = Color(0xFF757575)

@Composable
fun RecipeManagementScreen(
    navController: NavController,
    adminName: String = "Admin User",
    onLogout: () -> Unit = {},
    viewModel: RecipeManagementViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var selectedTab by remember { mutableStateOf("Products") }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color.Transparent,
                drawerTonalElevation = 0.dp
            ) {
                OwnerSideBarContent(
                    navController = navController,
                    drawerState = drawerState,
                    scope = scope,
                    ownerName = adminName,
                    onLogout = onLogout
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(RmPageBg)
        ) {
            Header(
                onMenuClick = {
                    scope.launch { drawerState.open() }
                }
            )

            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (selectedTab == "Products") "Products Available" else "Addons Available",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = RmTextMain
                    )

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White.copy(alpha = 0.5f))
                            .padding(4.dp)
                    ) {
                        listOf("Products", "Addons").forEach { tab ->
                            val isActive = selectedTab == tab
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (isActive) RmGreen else Color.Transparent)
                                    .clickable { selectedTab = tab }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = tab,
                                    color = if (isActive) Color.White else RmTextMain,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    shadowElevation = 2.dp
                ) {
                    val displayList = if (selectedTab == "Products") uiState.products else uiState.addons

                    if (displayList.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No ${selectedTab.lowercase()} found", color = RmTextSub)
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            modifier = Modifier.padding(12.dp),
                            contentPadding = PaddingValues(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(displayList, key = { it.productId }) { product ->
                                ProductGridItem(
                                    product = product,
                                    variants = uiState.variantsByProductId[product.productId].orEmpty(),
                                    hasRecipe = product.productId in uiState.productIdsWithRecipes,
                                    onClick = { viewModel.selectProduct(product) }
                                )
                            }
                        }
                    }
                }
            }
        }

        val selectedProduct = uiState.selectedProduct
        if (selectedProduct != null) {
            RecipeDetailDialog(
                product = selectedProduct,
                variants = uiState.variantsByProductId[selectedProduct.productId].orEmpty(),
                selectedVariant = uiState.selectedVariant,
                ingredients = uiState.ingredients,
                packagingLines = uiState.packagingLines,
                recipeLines = uiState.recipeLines,
                error = uiState.error,
                successMessage = uiState.successMessage,
                onVariantSelected = viewModel::selectVariant,
                onAddLine = viewModel::addLine,
                onRemoveLine = viewModel::removeLine,
                onIngredientSelected = viewModel::updateLineIngredient,
                onQuantityChanged = viewModel::updateLineQuantity,
                onAddPackagingLine = viewModel::addPackagingLine,
                onRemovePackagingLine = viewModel::removePackagingLine,
                onPackagingSelected = viewModel::updatePackagingIngredient,
                onPackagingQuantityChanged = viewModel::updatePackagingQuantity,
                onSave = viewModel::saveRecipe,
                onDismiss = viewModel::dismissDialog
            )
        }
    }
}

@Composable
private fun Header(
    onMenuClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(RmGreen)
            .padding(top = 40.dp, bottom = 12.dp, start = 16.dp, end = 16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = onMenuClick) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Menu",
                    tint = Color.White
                )
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
}

@Composable
private fun ProductGridItem(
    product: ProductEntity,
    variants: List<ProductVariantEntity>,
    hasRecipe: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .aspectRatio(0.9f)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFEEEEEE))
    ) {
        Box {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            ) {
                ProductImage(
                    imagePath = product.image,
                    contentDescription = product.productName
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = product.productName,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = RmTextMain,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    lineHeight = 16.sp,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "${variants.size} size(s)",
                    fontSize = 10.sp,
                    color = RmTextSub,
                    textAlign = TextAlign.Center
                )
            }

            if (!hasRecipe) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(Color.Red),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "No Recipe",
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun RecipeDetailDialog(
    product: ProductEntity,
    variants: List<ProductVariantEntity>,
    selectedVariant: ProductVariantEntity?,
    ingredients: List<IngredientEntity>,
    packagingLines: List<RecipeLineUi>,
    recipeLines: List<RecipeLineUi>,
    error: String?,
    successMessage: String?,
    onVariantSelected: (ProductVariantEntity) -> Unit,
    onAddLine: () -> Unit,
    onRemoveLine: (Int) -> Unit,
    onIngredientSelected: (Int, IngredientEntity) -> Unit,
    onQuantityChanged: (Int, String) -> Unit,
    onAddPackagingLine: () -> Unit,
    onRemovePackagingLine: (Int) -> Unit,
    onPackagingSelected: (Int, IngredientEntity) -> Unit,
    onPackagingQuantityChanged: (Int, String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    val packagingIngredients = ingredients.filter { it.isPackaging }
    val regularIngredients = ingredients.filter { !it.isPackaging }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ProductImage(
                        imagePath = product.image,
                        contentDescription = product.productName,
                        size = 56.dp
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = product.productName,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = RmTextMain
                        )

                        Text(
                            text = "Create or edit recipe",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (variants.isNotEmpty()) {
                    Text(
                        text = "Select Size",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        variants.forEach { variant ->
                            SizeCard(
                                variant = variant,
                                isSelected = selectedVariant?.variantId == variant.variantId,
                                onClick = { onVariantSelected(variant) }
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                }

                if (!product.isAddon) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Packaging (Required)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )

                        Text(
                            text = "+ Add packaging",
                            color = RmGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { onAddPackagingLine() }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (packagingLines.isNotEmpty()) {
                        RecipeHeaderRow()
                    }

                    packagingLines.forEachIndexed { index, line ->
                        RecipeIngredientRow(
                            line = line,
                            ingredients = packagingIngredients,
                            onRemove = { onRemovePackagingLine(index) },
                            onIngredientSelected = { ingredient ->
                                onPackagingSelected(index, ingredient)
                            },
                            onQuantityChanged = { value ->
                                onPackagingQuantityChanged(index, value)
                            }
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Ingredients",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )

                    Text(
                        text = "+ Add ingredient",
                        color = RmGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { onAddLine() }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (recipeLines.isNotEmpty()) {
                    RecipeHeaderRow()
                }

                recipeLines.forEachIndexed { index, line ->
                    RecipeIngredientRow(
                        line = line,
                        ingredients = regularIngredients,
                        onRemove = { onRemoveLine(index) },
                        onIngredientSelected = { ingredient ->
                            onIngredientSelected(index, ingredient)
                        },
                        onQuantityChanged = { value ->
                            onQuantityChanged(index, value)
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (!error.isNullOrBlank()) {
                    Text(error, color = Color.Red, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (!successMessage.isNullOrBlank()) {
                    Text(successMessage, color = RmGreen, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onSave,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RmGreen),
                    shape = RoundedCornerShape(8.dp),
                    enabled = variants.isEmpty() || selectedVariant != null
                ) {
                    Text(
                        text = "Save Recipe",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun RecipeHeaderRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Ingredient",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = RmTextSub,
            modifier = Modifier.weight(1.8f)
        )
        Text(
            text = "Qty",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = RmTextSub,
            modifier = Modifier.width(75.dp)
        )
        Text(
            text = "Unit",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = RmTextSub,
            modifier = Modifier.width(45.dp)
        )
        Spacer(modifier = Modifier.size(28.dp)) // Offset for delete button
    }
}

@Composable
private fun RowScope.SizeCard(
    variant: ProductVariantEntity,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .weight(1f)
            .height(64.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) Color.White else Color.Transparent,
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) RmGreen else Color(0xFFDDDDDD)
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = variant.sizeName,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) RmGreen else Color.Gray
            )

            Text(
                text = "₱${variant.price.toInt()}",
                fontSize = 10.sp,
                color = Color.Gray
            )
        }
    }
}

@Composable
private fun RecipeIngredientRow(
    line: RecipeLineUi,
    ingredients: List<IngredientEntity>,
    onRemove: (() -> Unit)?,
    onIngredientSelected: (IngredientEntity) -> Unit,
    onQuantityChanged: (String) -> Unit
) {
    var ingredientExpanded by remember { mutableStateOf(false) }

    val selectedIngredient = ingredients.firstOrNull {
        it.ingredientId == line.ingredientId
    }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(modifier = Modifier.weight(1.8f)) {
                OutlinedButton(
                    onClick = { ingredientExpanded = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Black),
                    border = BorderStroke(1.dp, Color(0xFFE0E0E0))
                ) {
                    Text(
                        text = selectedIngredient?.ingredientName ?: "Select",
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                DropdownMenu(
                    expanded = ingredientExpanded,
                    onDismissRequest = { ingredientExpanded = false },
                    modifier = Modifier.background(Color.White)
                ) {
                    ingredients.forEach { ingredient ->
                        DropdownMenuItem(
                            text = { Text(ingredient.ingredientName, fontSize = 13.sp) },
                            onClick = {
                                onIngredientSelected(ingredient)
                                ingredientExpanded = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = line.quantity,
                onValueChange = onQuantityChanged,
                modifier = Modifier
                    .width(75.dp)
                    .height(44.dp),
                shape = RoundedCornerShape(8.dp),
                singleLine = true,
                textStyle = TextStyle(fontSize = 13.sp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = RmGreen,
                    unfocusedBorderColor = Color(0xFFE0E0E0)
                )
            )

            Text(
                text = line.unit.ifBlank { "-" },
                fontSize = 11.sp,
                color = RmTextSub,
                modifier = Modifier.width(45.dp),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            IconButton(
                onClick = onRemove ?: {},
                modifier = Modifier.size(28.dp),
                enabled = onRemove != null
            ) {
                if (onRemove != null) {
                    Icon(
                        imageVector = Icons.Default.RemoveCircleOutline,
                        contentDescription = "Remove",
                        tint = Color(0xFFE57373)
                    )
                }
            }
        }
    }
}

@Composable
private fun ProductImage(
    imagePath: String?,
    contentDescription: String,
    size: androidx.compose.ui.unit.Dp = 56.dp
) {
    val context = LocalContext.current
    val imageFile = imagePath?.let { ImageStorage.getImageFile(context, it) }

    Surface(
        shape = CircleShape,
        color = Color(0xFFF5F5F5),
        modifier = Modifier.size(size),
        border = BorderStroke(1.dp, Color(0xFFEEEEEE))
    ) {
        if (imageFile != null && imageFile.exists()) {
            AsyncImage(
                model = imageFile,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Outlined.RestaurantMenu,
                    contentDescription = contentDescription,
                    tint = RmGreen,
                    modifier = Modifier.size(size / 2)
                )
            }
        }
    }
}
