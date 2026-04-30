package com.example.fruitylicious.ui.admin.products

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.fruitylicious.data.local.entity.AppDatabase
import com.example.fruitylicious.data.local.entity.ProductEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

// ── Brand colors ──────────────────────────────────────────────────────────────
private val MpGreen        = Color(0xFF2C8C44)
private val MpGreenDark    = Color(0xFF1B5E20)
private val MpPageBg       = Color(0xFFFFEAA0)
private val MpCardBg       = Color.White
private val MpRed          = Color(0xFFD32F2F)
private val MpTextMain     = Color(0xFF1A1A1A)
private val MpTextSub      = Color(0xFF757575)
private val MpTabActive    = Color(0xFFFFB300)   // amber — selected tab pill
private val MpTabInactive  = Color.White

// Dialog specific colors
private val DialogBgGray   = Color(0xFFF5F5F5)
private val DialogBlueIcon = Color(0xFF2196F3)
private val DialogRedIcon  = Color(0xFFF44336)
private val DialogBtnCancel= Color(0xFFE0E0E0)
private val DialogBtnDelete= Color(0xFFFF8A80)
private val DialogBtnCancelDark = Color(0xFF79867C) // For delete dialog cancel button

private enum class MpTab(val label: String) {
    PRODUCTS("Product List"),
    ADDONS("Add ons List")
}

// ── Root screen ───────────────────────────────────────────────────────────────
@Composable
fun ManageProductsScreen(
    drawerState:   DrawerState,
    scope:         CoroutineScope,
    onAddProduct:  () -> Unit = {},
    onAddOn:       () -> Unit = {},
    onEditProduct: (ProductEntity) -> Unit = {},
    onEditAddOn:   (ProductEntity) -> Unit = {},
) {
    var selectedBranch by remember { mutableStateOf("B1") }
    var selectedTab    by remember { mutableStateOf(MpTab.PRODUCTS) }

    // Dialog States
    var showProductDialog by remember { mutableStateOf(false) }
    var showAddonDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    var currentProduct by remember { mutableStateOf<ProductEntity?>(null) }
    var currentAddon by remember { mutableStateOf<ProductEntity?>(null) }
    var itemToDelete by remember { mutableStateOf<ProductEntity?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MpPageBg)
        ) {
            // ── Header ────────────────────────────────────────────────────────────
            MpHeader(
                selectedBranch = selectedBranch,
                onBranchSelect = { selectedBranch = it },
                onMenuClick    = { scope.launch { drawerState.open() } }
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Spacer(modifier = Modifier.height(4.dp))

                // ── Action buttons ────────────────────────────────────────────────
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick  = {
                            currentProduct = null
                            showProductDialog = true
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MpGreenDark),
                        shape  = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            "+ Add Product",
                            color      = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize   = 15.sp
                        )
                    }
                    Button(
                        onClick  = {
                            currentAddon = null
                            showAddonDialog = true
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MpGreenDark),
                        shape  = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            "+ Add Ons",
                            color      = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize   = 15.sp
                        )
                    }
                }

                // ── Tab pills ─────────────────────────────────────────────────────
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MpTab.entries.forEach { tab ->
                        val isActive = selectedTab == tab
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isActive) MpTabActive else MpTabInactive)
                                .border(
                                    width = if (isActive) 0.dp else 1.dp,
                                    color = Color(0xFFDDDDDD),
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .clickable { selectedTab = tab }
                                .padding(horizontal = 14.dp, vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text       = tab.label,
                                color      = if (isActive) Color.White else MpTextMain,
                                fontSize   = 13.sp,
                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                // ── List card ─────────────────────────────────────────────────────
                Surface(
                    modifier        = Modifier.fillMaxSize(),
                    color           = MpCardBg,
                    shape           = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                    shadowElevation = 2.dp
                ) {
                    when (selectedTab) {
                        MpTab.PRODUCTS -> ProductListContent(
                            onEdit   = { product ->
                                currentProduct = product
                                showProductDialog = true
                            },
                            onDelete = { product ->
                                itemToDelete = product
                                showDeleteDialog = true
                            }
                        )
                        MpTab.ADDONS -> AddOnListContent(
                            onEdit   = { addon ->
                                currentAddon = addon
                                showAddonDialog = true
                            },
                            onDelete = { addon ->
                                itemToDelete = addon
                                showDeleteDialog = true
                            }
                        )
                    }
                }
            }
        }

        // ── Dialogs ───────────────────────────────────────────────────────────
        if (showProductDialog) {
            ProductEditDialog(
                product = currentProduct,
                onDismiss = { showProductDialog = false },
                onSave = { /* Handle save logic */ showProductDialog = false }
            )
        }

        if (showAddonDialog) {
            AddonEditDialog(
                addon = currentAddon,
                onDismiss = { showAddonDialog = false },
                onSave = { /* Handle save logic */ showAddonDialog = false }
            )
        }

        if (showDeleteDialog && itemToDelete != null) {
            DeleteConfirmationDialog(
                onDismiss = { showDeleteDialog = false },
                onConfirm = { /* Handle delete logic */ showDeleteDialog = false }
            )
        }
    }
}

// ── Custom Form TextField ─────────────────────────────────────────────────────
@Composable
private fun DialogTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = TextStyle(fontSize = 14.sp, color = MpTextMain),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(DialogBgGray)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        decorationBox = { innerTextField ->
            if (value.isEmpty()) {
                Text(placeholder, color = Color.Gray, fontSize = 14.sp)
            }
            innerTextField()
        }
    )
}

// ── Product Add/Edit Dialog ───────────────────────────────────────────────────
@Composable
private fun ProductEditDialog(
    product: ProductEntity?,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    var name by remember { mutableStateOf(product?.productName ?: "") }
    var price by remember { mutableStateOf(product?.price?.toString() ?: "") }
    var size by remember { mutableStateOf("") } // Assuming size is not in entity yet

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Edit Icon Header
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(DialogBlueIcon),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.White)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Add / Edit Product",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MpTextMain
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Fields
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column {
                        Text("Product Name", fontSize = 12.sp, color = MpTextMain, modifier = Modifier.padding(bottom = 4.dp))
                        DialogTextField(value = name, onValueChange = { name = it }, placeholder = "e.g. Mango")
                    }
                    Column {
                        Text("Price", fontSize = 12.sp, color = MpTextMain, modifier = Modifier.padding(bottom = 4.dp))
                        DialogTextField(value = price, onValueChange = { price = it }, placeholder = "₱ 0.00")
                    }
                    Column {
                        Text("Size", fontSize = 12.sp, color = MpTextMain, modifier = Modifier.padding(bottom = 4.dp))
                        DialogTextField(value = size, onValueChange = { size = it }, placeholder = "e.g. Small")
                    }
                    Column {
                        Text("Insert Image", fontSize = 12.sp, color = MpTextMain, modifier = Modifier.padding(bottom = 4.dp))

                        // Dashed upload box
                        val dashPath = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(DialogBgGray)
                                .drawBehind {
                                    drawRoundRect(
                                        color = Color.Gray,
                                        style = Stroke(width = 4f, pathEffect = dashPath),
                                        cornerRadius = CornerRadius(8.dp.toPx())
                                    )
                                }
                                .clickable { /* Upload image logic */ },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Image, contentDescription = "Upload", tint = Color.Gray)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Upload product image", fontSize = 12.sp, color = Color.Gray)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DialogBtnCancel),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Discard", color = MpTextMain, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = onSave,
                        modifier = Modifier.weight(1f).height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MpGreen),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Save", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ── Addon Add/Edit Dialog ─────────────────────────────────────────────────────
@Composable
private fun AddonEditDialog(
    addon: ProductEntity?,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    var name by remember { mutableStateOf(addon?.productName ?: "") }
    var price by remember { mutableStateOf(addon?.price?.toString() ?: "") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Edit Icon Header
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(DialogBlueIcon),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.White)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Add / Edit Add ons",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MpTextMain
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Fields
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column {
                        Text("Add ons Name", fontSize = 12.sp, color = MpTextMain, modifier = Modifier.padding(bottom = 4.dp))
                        DialogTextField(value = name, onValueChange = { name = it }, placeholder = "e.g. Pearl")
                    }
                    Column {
                        Text("Price", fontSize = 12.sp, color = MpTextMain, modifier = Modifier.padding(bottom = 4.dp))
                        DialogTextField(value = price, onValueChange = { price = it }, placeholder = "₱ 0.00")
                    }
                    Column {
                        Text("Insert Image", fontSize = 12.sp, color = MpTextMain, modifier = Modifier.padding(bottom = 4.dp))

                        // Dashed upload box
                        val dashPath = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(DialogBgGray)
                                .drawBehind {
                                    drawRoundRect(
                                        color = Color.Gray,
                                        style = Stroke(width = 4f, pathEffect = dashPath),
                                        cornerRadius = CornerRadius(8.dp.toPx())
                                    )
                                }
                                .clickable { /* Upload image logic */ },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Image, contentDescription = "Upload", tint = Color.Gray)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Upload add ons image", fontSize = 12.sp, color = Color.Gray)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DialogBtnCancel),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Discard", color = MpTextMain, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = onSave,
                        modifier = Modifier.weight(1f).height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MpGreen),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Save", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ── Delete Confirmation Dialog ────────────────────────────────────────────────
@Composable
private fun DeleteConfirmationDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Delete Icon Header
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(DialogRedIcon),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White, modifier = Modifier.size(32.dp))
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Delete Product",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MpTextMain
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Are you sure you want\nto remove this product?",
                    fontSize = 14.sp,
                    color = MpTextSub,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DialogBtnCancelDark),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Cancel", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f).height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DialogBtnDelete),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Delete Product", color = MpTextMain, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}


// ── Product list ──────────────────────────────────────────────────────────────
@Composable
private fun ProductListContent(
    onEdit:   (ProductEntity) -> Unit,
    onDelete: (ProductEntity) -> Unit
) {
    val context      = LocalContext.current
    val db           = remember { AppDatabase.getDatabase(context) }
    val productDao   = db.productDao()

    val products by productDao.getMainProducts().collectAsState(initial = emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 8.dp)
    ) {
        if (products.isEmpty()) {
            Box(
                modifier         = Modifier.fillMaxWidth().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No products found", fontSize = 14.sp, color = MpTextSub)
            }
        } else {
            products.forEach { product ->
                ProductRow(
                    product  = product,
                    onEdit   = { onEdit(product) },
                    onDelete = { onDelete(product) }
                )
                HorizontalDivider(
                    color     = Color(0xFFF0F0F0),
                    modifier  = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }
}

@Composable
private fun ProductRow(
    product:  ProductEntity,
    onEdit:   () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Fruit emoji / image
        Box(
            modifier         = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(Color(0xFFF5F5F5)),
            contentAlignment = Alignment.Center
        ) {
            Text(fruitEmoji(product.productName), fontSize = 22.sp)
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Name + price
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = product.productName,
                fontSize   = 15.sp,
                fontWeight = FontWeight.Bold,
                color      = MpTextMain
            )
            Text(
                text     = "₱${product.price.toInt()}",
                fontSize = 12.sp,
                color    = MpTextSub
            )
        }

        // Edit button
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(MpTextMain)
                .clickable { onEdit() }
                .padding(horizontal = 12.dp, vertical = 7.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Edit", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.width(6.dp))

        // Delete button
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(MpRed)
                .clickable { onDelete() }
                .padding(horizontal = 12.dp, vertical = 7.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Delete", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// ── Add-on list ───────────────────────────────────────────────────────────────
@Composable
private fun AddOnListContent(
    onEdit:   (ProductEntity) -> Unit,
    onDelete: (ProductEntity) -> Unit
) {
    val context      = LocalContext.current
    val db           = remember { AppDatabase.getDatabase(context) }
    val productDao   = db.productDao()

    val addOns by productDao.getAddons().collectAsState(initial = emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 8.dp)
    ) {
        if (addOns.isEmpty()) {
            Box(
                modifier         = Modifier.fillMaxWidth().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No add-ons found", fontSize = 14.sp, color = MpTextSub)
            }
        } else {
            addOns.forEach { addOn ->
                AddOnRow(
                    addOn    = addOn,
                    onEdit   = { onEdit(addOn) },
                    onDelete = { onDelete(addOn) }
                )
                HorizontalDivider(
                    color    = Color(0xFFF0F0F0),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }
}

@Composable
private fun AddOnRow(
    addOn:    ProductEntity,
    onEdit:   () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon circle
        Box(
            modifier         = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(Color(0xFFF5F5F5)),
            contentAlignment = Alignment.Center
        ) {
            Text(addOnEmoji(addOn.productName), fontSize = 22.sp)
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Name + price
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = addOn.productName,
                fontSize   = 15.sp,
                fontWeight = FontWeight.Bold,
                color      = MpTextMain
            )
            Text(
                text     = "₱${addOn.price.toInt()}",
                fontSize = 12.sp,
                color    = MpTextSub
            )
        }

        // Edit button
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(MpTextMain)
                .clickable { onEdit() }
                .padding(horizontal = 12.dp, vertical = 7.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Edit", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.width(6.dp))

        // Delete button
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(MpRed)
                .clickable { onDelete() }
                .padding(horizontal = 12.dp, vertical = 7.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Delete", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// ── Header ────────────────────────────────────────────────────────────────────
@Composable
private fun MpHeader(
    selectedBranch: String,
    onBranchSelect: (String) -> Unit,
    onMenuClick:    () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MpGreen)
            .padding(start = 16.dp, end = 16.dp, top = 48.dp, bottom = 14.dp)
    ) {

        // Hamburger and Title Group
        Row(
            modifier = Modifier.align(Alignment.CenterStart),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Hamburger
            Column(
                modifier            = Modifier
                    .clickable { onMenuClick() }
                    .padding(end = 12.dp), // Space between hamburger and title
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                repeat(3) {
                    Box(
                        modifier = Modifier
                            .width(22.dp)
                            .height(2.5.dp)
                            .background(Color.White, RoundedCornerShape(2.dp))
                    )
                }
            }

            // Highlighted Title
            Text(
                text       = "MANAGE PRODUCTS",
                color      = Color.White,
                fontSize   = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Branch Selector Toggle (Updated radius to 16dp outer, 12dp inner)
        Row(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFFF5F5F5))
                .padding(4.dp)
        ) {
            listOf("B1", "B2", "All").forEach { branch ->
                val isSelected = selectedBranch == branch
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) MpGreen else Color.Transparent)
                        .clickable { onBranchSelect(branch) }
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

// ── Emoji helpers (fallback when no image URL) ────────────────────────────────
private fun fruitEmoji(name: String): String = when {
    name.contains("Dragon", ignoreCase = true) -> "🔴"
    name.contains("Mango",  ignoreCase = true) -> "🥭"
    name.contains("Buko",   ignoreCase = true) -> "🥥"
    name.contains("Strawberry", ignoreCase = true) -> "🍓"
    name.contains("Melon", ignoreCase = true)  -> "🍈"
    name.contains("Banana", ignoreCase = true) -> "🍌"
    name.contains("Apple",  ignoreCase = true) -> "🍎"
    name.contains("Avocado", ignoreCase = true) -> "🥑"
    else -> "🍑"
}

private fun addOnEmoji(name: String): String = when {
    name.contains("Nata",    ignoreCase = true) -> "🥥"
    name.contains("Pearl",   ignoreCase = true) -> "⚫"
    name.contains("Cheese",  ignoreCase = true) -> "🧀"
    name.contains("Oreo",    ignoreCase = true) -> "🍪"
    name.contains("Graham",  ignoreCase = true) -> "🍪"
    name.contains("Lemon",   ignoreCase = true) -> "🍋"
    else -> "✨"
}

// ── Preview ───────────────────────────────────────────────────────────────────
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ManageProductsScreenPreview() {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope       = rememberCoroutineScope()
    ManageProductsScreen(
        drawerState = drawerState,
        scope       = scope
    )
}
