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
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.UUID

// ── Brand colors ──────────────────────────────────────────────────────────────
private val MpGreen         = Color(0xFF2C8C44)
private val MpPageBg        = Color(0xFFFFEAA0)
private val MpCardBg        = Color.White
private val MpRed           = Color(0xFFE53935)
private val MpTextMain      = Color(0xFF1A1A1A)
private val MpTextSub       = Color(0xFF757575)
private val MpTabActive     = Color(0xFFFFB300)   // amber — selected tab pill
private val MpTabInactive   = Color.White

// Dialog specific colors
private val DialogBgGray    = Color(0xFFF5F5F5)
private val DialogBlueIcon  = Color(0xFF2196F3)
private val DialogRedIcon   = Color(0xFFE53935)
private val DialogBtnCancel = Color(0xFF5D6B60)
private val DialogBtnDelete = Color(0xFFFF8A80)
private val EditBtnColor    = Color(0xFF333333)

private enum class MpTab(val label: String) {
    PRODUCTS("Product List"),
    ADDONS("Add ons List")
}

// ── Local Data Models ────────────────────────────────────────────────────────

data class ManageProduct(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val size: String,
    val price: Double,
    val branch: String
)

data class ManageAddOn(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val price: Double,
    val branch: String
)

// ── Root screen ───────────────────────────────────────────────────────────────
@Composable
fun ManageProductsScreen(
    drawerState:   DrawerState,
    scope:         CoroutineScope
) {
    var selectedBranch by remember { mutableStateOf("B1") }
    var selectedTab    by remember { mutableStateOf(MpTab.PRODUCTS) }
    var searchQuery    by remember { mutableStateOf("") }

    // Mock Data for local functionality
    val products = remember {
        mutableStateListOf(
            ManageProduct(name = "Dragon Fruit", size = "Small", price = 60.0, branch = "B1"),
            ManageProduct(name = "Dragon Fruit", size = "Medium", price = 50.0, branch = "B1"),
            ManageProduct(name = "Dragon Fruit", size = "Large", price = 80.0, branch = "B1"),
            ManageProduct(name = "Mango", size = "Small", price = 60.0, branch = "B1"),
            ManageProduct(name = "Mango", size = "Medium", price = 50.0, branch = "B1"),
            ManageProduct(name = "Mango", size = "Large", price = 80.0, branch = "B1"),
            ManageProduct(name = "Buko", size = "Small", price = 60.0, branch = "B1")
        )
    }

    val addOns = remember {
        mutableStateListOf(
            ManageAddOn(name = "Nata De Coco", price = 10.0, branch = "B1"),
            ManageAddOn(name = "Pearl", price = 10.0, branch = "B1"),
            ManageAddOn(name = "Cheese", price = 10.0, branch = "B1")
        )
    }

    // Dialog States
    var showProductDialog by remember { mutableStateOf(false) }
    var showAddonDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    var currentProduct by remember { mutableStateOf<ManageProduct?>(null) }
    var currentAddon by remember { mutableStateOf<ManageAddOn?>(null) }
    var itemToDeleteId by remember { mutableStateOf<String?>(null) }
    var itemToDeleteName by remember { mutableStateOf("") }
    var isDeleteAddon by remember { mutableStateOf(false) }

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
                    .padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Spacer(modifier = Modifier.height(10.dp))

                // ── Action buttons ────────────────────────────────────────────────
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick  = {
                            currentProduct = null
                            showProductDialog = true
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MpGreen),
                        shape  = RoundedCornerShape(10.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
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
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MpGreen),
                        shape  = RoundedCornerShape(10.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                    ) {
                        Text(
                            "+ Add Ons",
                            color      = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize   = 15.sp
                        )
                    }
                }

                // ── Search Bar ────────────────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(30.dp))
                        .background(Color.White)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        textStyle = TextStyle(fontSize = 14.sp, color = MpTextMain),
                        modifier = Modifier.fillMaxWidth(),
                        decorationBox = { inner ->
                            if (searchQuery.isEmpty()) Text("Search product, addons, etc.", color = Color.LightGray, fontSize = 14.sp)
                            inner()
                        },
                        singleLine = true
                    )
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
                    shape           = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                ) {
                    val filteredProducts = products.filter {
                        (selectedBranch == "All" || it.branch == selectedBranch) &&
                                it.name.contains(searchQuery, ignoreCase = true)
                    }
                    val filteredAddOns = addOns.filter {
                        (selectedBranch == "All" || it.branch == selectedBranch) &&
                                it.name.contains(searchQuery, ignoreCase = true)
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 12.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        when (selectedTab) {
                            MpTab.PRODUCTS -> {
                                if (filteredProducts.isEmpty()) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text("No products found", color = MpTextSub)
                                    }
                                } else {
                                    filteredProducts.forEach { product ->
                                        ProductRow(
                                            product = product,
                                            onEdit = {
                                                currentProduct = product
                                                showProductDialog = true
                                            },
                                            onDelete = {
                                                itemToDeleteId = product.id
                                                itemToDeleteName = product.name
                                                isDeleteAddon = false
                                                showDeleteDialog = true
                                            }
                                        )
                                    }
                                }
                            }
                            MpTab.ADDONS -> {
                                if (filteredAddOns.isEmpty()) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text("No add-ons found", color = MpTextSub)
                                    }
                                } else {
                                    filteredAddOns.forEach { addon ->
                                        AddOnRow(
                                            addon = addon,
                                            onEdit = {
                                                currentAddon = addon
                                                showAddonDialog = true
                                            },
                                            onDelete = {
                                                itemToDeleteId = addon.id
                                                itemToDeleteName = addon.name
                                                isDeleteAddon = true
                                                showDeleteDialog = true
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── Dialogs ───────────────────────────────────────────────────────────
        if (showProductDialog) {
            ProductEditDialog(
                product = currentProduct,
                onDismiss = { showProductDialog = false },
                onSave = { name, price, size ->
                    if (currentProduct != null) {
                        val index = products.indexOfFirst { it.id == currentProduct!!.id }
                        if (index != -1) {
                            products[index] = currentProduct!!.copy(name = name, price = price.toDoubleOrNull() ?: 0.0, size = size)
                        }
                    } else {
                        products.add(ManageProduct(name = name, size = size, price = price.toDoubleOrNull() ?: 0.0, branch = if(selectedBranch == "All") "B1" else selectedBranch))
                    }
                    showProductDialog = false
                }
            )
        }

        if (showAddonDialog) {
            AddonEditDialog(
                addon = currentAddon,
                onDismiss = { showAddonDialog = false },
                onSave = { name, price ->
                    if (currentAddon != null) {
                        val index = addOns.indexOfFirst { it.id == currentAddon!!.id }
                        if (index != -1) {
                            addOns[index] = currentAddon!!.copy(name = name, price = price.toDoubleOrNull() ?: 0.0)
                        }
                    } else {
                        addOns.add(ManageAddOn(name = name, price = price.toDoubleOrNull() ?: 0.0, branch = if(selectedBranch == "All") "B1" else selectedBranch))
                    }
                    showAddonDialog = false
                }
            )
        }

        if (showDeleteDialog && itemToDeleteId != null) {
            DeleteConfirmationDialog(
                itemName = itemToDeleteName,
                isAddon = isDeleteAddon,
                onDismiss = { showDeleteDialog = false },
                onConfirm = {
                    if (isDeleteAddon) {
                        addOns.removeIf { it.id == itemToDeleteId }
                    } else {
                        products.removeIf { it.id == itemToDeleteId }
                    }
                    showDeleteDialog = false
                }
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
    product: ManageProduct?,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var name by remember { mutableStateOf(product?.name ?: "") }
    var price by remember { mutableStateOf(product?.price?.toString() ?: "") }
    var size by remember { mutableStateOf(product?.size ?: "") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Blue edit icon circle
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(DialogBlueIcon),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Add / Edit Product",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MpTextMain
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Fields
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Column {
                        Text("Product Name", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MpTextMain, modifier = Modifier.padding(bottom = 4.dp))
                        DialogTextField(value = name, onValueChange = { name = it }, placeholder = "e.g. Mango")
                    }
                    Column {
                        Text("Price", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MpTextMain, modifier = Modifier.padding(bottom = 4.dp))
                        DialogTextField(value = price, onValueChange = { price = it }, placeholder = "₱ 0.00")
                    }
                    Column {
                        Text("Size", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MpTextMain, modifier = Modifier.padding(bottom = 4.dp))
                        DialogTextField(value = size, onValueChange = { size = it }, placeholder = "e.g. Small")
                    }
                    Column {
                        Text("Insert Image", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MpTextMain, modifier = Modifier.padding(bottom = 4.dp))

                        // Dashed upload box
                        val dashPath = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(90.dp)
                                .drawBehind {
                                    drawRoundRect(
                                        color = Color.Gray,
                                        style = Stroke(width = 1.5f, pathEffect = dashPath),
                                        cornerRadius = CornerRadius(8.dp.toPx())
                                    )
                                }
                                .background(DialogBgGray, RoundedCornerShape(8.dp))
                                .clickable { /* Upload image logic */ },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Image, contentDescription = "Upload", tint = Color.Gray, modifier = Modifier.size(28.dp))
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
                        modifier = Modifier.weight(1f).height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEEEEEE)),
                        shape = RoundedCornerShape(10.dp),
                        elevation = ButtonDefaults.buttonElevation(0.dp)
                    ) {
                        Text("Discard", color = MpTextMain, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    Button(
                        onClick = { onSave(name, price, size) },
                        modifier = Modifier.weight(1f).height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MpGreen),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Save", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
        }
    }
}

// ── Addon Add/Edit Dialog ─────────────────────────────────────────────────────
@Composable
private fun AddonEditDialog(
    addon: ManageAddOn?,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var name by remember { mutableStateOf(addon?.name ?: "") }
    var price by remember { mutableStateOf(addon?.price?.toString() ?: "") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Blue edit icon circle
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(DialogBlueIcon),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Add / Edit Add ons",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MpTextMain
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Fields
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Column {
                        Text("Add ons Name", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MpTextMain, modifier = Modifier.padding(bottom = 4.dp))
                        DialogTextField(value = name, onValueChange = { name = it }, placeholder = "e.g. Pearl")
                    }
                    Column {
                        Text("Price", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MpTextMain, modifier = Modifier.padding(bottom = 4.dp))
                        DialogTextField(value = price, onValueChange = { price = it }, placeholder = "₱ 0.00")
                    }
                    Column {
                        Text("Insert Image", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MpTextMain, modifier = Modifier.padding(bottom = 4.dp))

                        // Dashed upload box
                        val dashPath = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(90.dp)
                                .drawBehind {
                                    drawRoundRect(
                                        color = Color.Gray,
                                        style = Stroke(width = 1.5f, pathEffect = dashPath),
                                        cornerRadius = CornerRadius(8.dp.toPx())
                                    )
                                }
                                .background(DialogBgGray, RoundedCornerShape(8.dp))
                                .clickable { /* Upload image logic */ },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Image, contentDescription = "Upload", tint = Color.Gray, modifier = Modifier.size(28.dp))
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
                        modifier = Modifier.weight(1f).height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEEEEEE)),
                        shape = RoundedCornerShape(10.dp),
                        elevation = ButtonDefaults.buttonElevation(0.dp)
                    ) {
                        Text("Discard", color = MpTextMain, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    Button(
                        onClick = { onSave(name, price) },
                        modifier = Modifier.weight(1f).height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MpGreen),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Save", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
        }
    }
}

// ── Delete Confirmation Dialog ────────────────────────────────────────────────
@Composable
private fun DeleteConfirmationDialog(
    itemName: String,
    isAddon: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(28.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Red trash icon circle
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(DialogRedIcon),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(34.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (isAddon) "Delete Add on" else "Delete Product",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MpTextMain
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Are you sure you want to\nremove $itemName?",
                    fontSize = 14.sp,
                    color = MpTextSub,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DialogBtnCancel),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Cancel", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1.4f).height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DialogBtnDelete),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        Text(
                            text = if (isAddon) "Delete Add on" else "Delete Product",
                            color = MpTextMain,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductRow(
    product:  ManageProduct,
    onEdit:   () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFFFF9C4))
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Fruit emoji / image
        Box(
            modifier         = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Text(fruitEmoji(product.name), fontSize = 22.sp)
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Name + size - price
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = product.name,
                fontSize   = 14.sp,
                fontWeight = FontWeight.Bold,
                color      = MpTextMain
            )
            Text(
                text     = "${product.size} - ₱${product.price.toInt()}",
                fontSize = 12.sp,
                color    = MpTextSub
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            // Edit button — dark background
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(7.dp))
                    .background(EditBtnColor)
                    .clickable { onEdit() }
                    .padding(horizontal = 14.dp, vertical = 7.dp)
            ) {
                Text("Edit", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            // Delete button — red background
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(7.dp))
                    .background(MpRed)
                    .clickable { onDelete() }
                    .padding(horizontal = 10.dp, vertical = 7.dp)
            ) {
                Text("Delete", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun AddOnRow(
    addon:    ManageAddOn,
    onEdit:   () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFFFF9C4))
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon circle
        Box(
            modifier         = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Text(addOnEmoji(addon.name), fontSize = 22.sp)
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Name + price
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = addon.name,
                fontSize   = 14.sp,
                fontWeight = FontWeight.Bold,
                color      = MpTextMain
            )
            Text(
                text     = "₱${addon.price.toInt()}",
                fontSize = 12.sp,
                color    = MpTextSub
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            // Edit button — dark background
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(7.dp))
                    .background(EditBtnColor)
                    .clickable { onEdit() }
                    .padding(horizontal = 14.dp, vertical = 7.dp)
            ) {
                Text("Edit", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            // Delete button — red background
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(7.dp))
                    .background(MpRed)
                    .clickable { onDelete() }
                    .padding(horizontal = 10.dp, vertical = 7.dp)
            ) {
                Text("Delete", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ── Header ────────────────────────────────────────────────────────────────────
@Composable
private fun MpHeader(
    selectedBranch: String,
    onBranchSelect: (String) -> Unit,
    onMenuClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MpGreen)
            .padding(start = 16.dp, end = 16.dp, top = 48.dp, bottom = 14.dp)
    ) {
        Row(
            modifier = Modifier.align(Alignment.CenterStart),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Hamburger
            Column(
                modifier = Modifier
                    .clickable { onMenuClick() }
                    .padding(end = 12.dp),
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
            // Yellow highlighted title
            Text(
                text = "MANAGE PRODUCTS",
                color = Color.White,
                fontSize = 18.sp,
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
