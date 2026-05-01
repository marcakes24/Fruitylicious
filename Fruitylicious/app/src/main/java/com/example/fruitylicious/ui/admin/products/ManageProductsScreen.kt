package com.example.fruitylicious.ui.admin.products

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.fruitylicious.data.local.entity.ProductEntity
import com.example.fruitylicious.ui.shared.AdminSideBarContent
import kotlinx.coroutines.launch

private val MpGreen = Color(0xFF2C8C44)
private val MpPageBg = Color(0xFFFFEAA0)
private val MpCardBg = Color.White
private val MpRed = Color(0xFFE53935)
private val MpTextMain = Color(0xFF1A1A1A)
private val MpTextSub = Color(0xFF757575)
private val MpTabActive = Color(0xFFFFB300)
private val MpTabInactive = Color.White

private val DialogBgGray = Color(0xFFF5F5F5)
private val DialogBlueIcon = Color(0xFF2196F3)
private val DialogRedIcon = Color(0xFFE53935)
private val DialogBtnCancel = Color(0xFF5D6B60)
private val DialogBtnDelete = Color(0xFFFF8A80)
private val EditBtnColor = Color(0xFF333333)

private enum class MpTab(val label: String) {
    PRODUCTS("Product List"),
    ADDONS("Add-ons List")
}

@Composable
fun ManageProductsScreen(
    navController: NavController,
    adminName: String = "Admin User",
    onLogout: () -> Unit = {},
    viewModel: ManageProductsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var selectedTab by remember { mutableStateOf(MpTab.PRODUCTS) }
    var searchQuery by remember { mutableStateOf("") }

    var showProductDialog by remember { mutableStateOf(false) }
    var showAddonDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    var currentProductRow by remember { mutableStateOf<ProductVariantRow?>(null) }
    var currentAddon by remember { mutableStateOf<ProductEntity?>(null) }

    var deleteName by remember { mutableStateOf("") }
    var deleteProductVariantId by remember { mutableStateOf<Int?>(null) }
    var deleteAddonId by remember { mutableStateOf<Int?>(null) }

    val filteredProducts = uiState.productRows.filter {
        it.product.productName.contains(searchQuery, ignoreCase = true) ||
                it.variant.sizeName.contains(searchQuery, ignoreCase = true)
    }

    val filteredAddons = uiState.addons.filter {
        it.productName.contains(searchQuery, ignoreCase = true)
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color.Transparent,
                drawerTonalElevation = 0.dp
            ) {
                AdminSideBarContent(
                    navController = navController,
                    drawerState = drawerState,
                    scope = scope,
                    adminName = adminName,
                    onLogout = onLogout
                )
            }
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MpPageBg)
            ) {
                MpHeader(
                    onMenuClick = {
                        scope.launch {
                            drawerState.open()
                        }
                    }
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                currentProductRow = null
                                showProductDialog = true
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MpGreen),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "+ Add Product",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = {
                                currentAddon = null
                                showAddonDialog = true
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MpGreen),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "+ Add Ons",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    SearchBar(
                        value = searchQuery,
                        onValueChange = { searchQuery = it }
                    )

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
                                    text = tab.label,
                                    color = if (isActive) Color.White else MpTextMain,
                                    fontSize = 13.sp,
                                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    uiState.error?.let {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp
                        )
                    }

                    uiState.successMessage?.let {
                        Text(
                            text = it,
                            color = MpGreen,
                            fontSize = 12.sp
                        )
                    }

                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MpCardBg,
                        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                    ) {
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
                                        EmptyText("No products found")
                                    } else {
                                        filteredProducts.forEach { row ->
                                            ProductRow(
                                                row = row,
                                                onEdit = {
                                                    currentProductRow = row
                                                    showProductDialog = true
                                                },
                                                onDelete = {
                                                    deleteProductVariantId = row.variant.variantId
                                                    deleteAddonId = null
                                                    deleteName = "${row.product.productName} ${row.variant.sizeName}"
                                                    showDeleteDialog = true
                                                }
                                            )
                                        }
                                    }
                                }

                                MpTab.ADDONS -> {
                                    if (filteredAddons.isEmpty()) {
                                        EmptyText("No add-ons found")
                                    } else {
                                        filteredAddons.forEach { addon ->
                                            AddOnRow(
                                                addon = addon,
                                                onEdit = {
                                                    currentAddon = addon
                                                    showAddonDialog = true
                                                },
                                                onDelete = {
                                                    deleteAddonId = addon.productId
                                                    deleteProductVariantId = null
                                                    deleteName = addon.productName
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

            if (showProductDialog) {
                ProductEditDialog(
                    row = currentProductRow,
                    onDismiss = { showProductDialog = false },
                    onSave = { name, size, price, image ->
                        viewModel.saveProductWithVariant(
                            existingProductId = currentProductRow?.product?.productId,
                            existingVariantId = currentProductRow?.variant?.variantId,
                            name = name,
                            image = image,
                            sizeName = size,
                            price = price.toDoubleOrNull() ?: 0.0
                        )
                        showProductDialog = false
                    }
                )
            }

            if (showAddonDialog) {
                AddonEditDialog(
                    addon = currentAddon,
                    onDismiss = { showAddonDialog = false },
                    onSave = { name, price, image ->
                        viewModel.saveAddon(
                            existingAddonId = currentAddon?.productId,
                            name = name,
                            image = image,
                            price = price.toDoubleOrNull() ?: 0.0
                        )
                        showAddonDialog = false
                    }
                )
            }

            if (showDeleteDialog) {
                DeleteConfirmationDialog(
                    itemName = deleteName,
                    isAddon = deleteAddonId != null,
                    onDismiss = { showDeleteDialog = false },
                    onConfirm = {
                        deleteProductVariantId?.let {
                            viewModel.deleteProductVariant(it)
                        }

                        deleteAddonId?.let {
                            viewModel.deleteAddon(it)
                        }

                        showDeleteDialog = false
                    }
                )
            }
        }
    }
}

@Composable
private fun EmptyText(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = MpTextSub
        )
    }
}

@Composable
private fun SearchBar(
    value: String,
    onValueChange: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(30.dp))
            .background(Color.White)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = Color.Gray,
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(fontSize = 14.sp, color = MpTextMain),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                if (value.isEmpty()) {
                    Text(
                        text = "Search product, add-ons, etc.",
                        color = Color.LightGray,
                        fontSize = 14.sp
                    )
                }
                inner()
            },
            singleLine = true
        )
    }
}

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
        decorationBox = { inner ->
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
            inner()
        },
        singleLine = true
    )
}

@Composable
private fun ProductEditDialog(
    row: ProductVariantRow?,
    onDismiss: () -> Unit,
    onSave: (name: String, size: String, price: String, image: String) -> Unit
) {
    var name by remember { mutableStateOf(row?.product?.productName ?: "") }
    var size by remember { mutableStateOf(row?.variant?.sizeName ?: "") }
    var price by remember { mutableStateOf(row?.variant?.price?.toString() ?: "") }
    var image by remember { mutableStateOf(row?.product?.image ?: "") }

    EditDialogShell(
        title = "Add / Edit Product",
        onDismiss = onDismiss,
        onSave = { onSave(name, size, price, image) }
    ) {
        FieldBlock("Product Name") {
            DialogTextField(name, { name = it }, "e.g. Mango Shake")
        }

        FieldBlock("Size") {
            DialogTextField(size, { size = it }, "e.g. Medium")
        }

        FieldBlock("Price") {
            DialogTextField(price, { price = it }, "0.00")
        }

        FieldBlock("Image URI") {
            DialogTextField(image, { image = it }, "Optional image URI")
        }

        UploadBox("Upload product image")
    }
}

@Composable
private fun AddonEditDialog(
    addon: ProductEntity?,
    onDismiss: () -> Unit,
    onSave: (name: String, price: String, image: String) -> Unit
) {
    var name by remember { mutableStateOf(addon?.productName ?: "") }
    var price by remember { mutableStateOf(addon?.price?.toString() ?: "") }
    var image by remember { mutableStateOf(addon?.image ?: "") }

    EditDialogShell(
        title = "Add / Edit Add ons",
        onDismiss = onDismiss,
        onSave = { onSave(name, price, image) }
    ) {
        FieldBlock("Add ons Name") {
            DialogTextField(name, { name = it }, "e.g. Pearl")
        }

        FieldBlock("Price") {
            DialogTextField(price, { price = it }, "0.00")
        }

        FieldBlock("Image URI") {
            DialogTextField(image, { image = it }, "Optional image URI")
        }

        UploadBox("Upload add ons image")
    }
}

@Composable
private fun EditDialogShell(
    title: String,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
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
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(DialogBlueIcon),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MpTextMain
                )

                Spacer(modifier = Modifier.height(20.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    content = content
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEEEEEE)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "Discard",
                            color = MpTextMain,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = onSave,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MpGreen),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "Save",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FieldBlock(
    label: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = MpTextMain,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        content()
    }
}

@Composable
private fun UploadBox(label: String) {
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
            .background(DialogBgGray, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Image,
                contentDescription = null,
                tint = Color.Gray
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = label,
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
    }
}

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
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(DialogRedIcon),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (isAddon) "Delete Add on" else "Delete Product Size",
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DialogBtnCancel),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "Cancel",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = onConfirm,
                        modifier = Modifier
                            .weight(1.4f)
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DialogBtnDelete),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = if (isAddon) "Delete Add on" else "Delete Size",
                            color = MpTextMain,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductRow(
    row: ProductVariantRow,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    RowCard {
        IconCircle(
            icon = Icons.Default.Inventory2,
            contentDescription = "Product"
        )

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = row.product.productName,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MpTextMain
            )

            Text(
                text = "${row.variant.sizeName} - ₱${row.variant.price.toInt()}",
                fontSize = 12.sp,
                color = MpTextSub
            )
        }

        RowButtons(
            onEdit = onEdit,
            onDelete = onDelete
        )
    }
}

@Composable
private fun AddOnRow(
    addon: ProductEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    RowCard {
        IconCircle(
            icon = Icons.Default.AddCircle,
            contentDescription = "Add-on"
        )

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = addon.productName,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MpTextMain
            )

            Text(
                text = "₱${addon.price.toInt()}",
                fontSize = 12.sp,
                color = MpTextSub
            )
        }

        RowButtons(
            onEdit = onEdit,
            onDelete = onDelete
        )
    }
}

@Composable
private fun RowCard(
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFFFF9C4))
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

@Composable
private fun IconCircle(
    icon: ImageVector,
    contentDescription: String
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = MpGreen,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun RowButtons(
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(7.dp))
                .background(EditBtnColor)
                .clickable { onEdit() }
                .padding(horizontal = 14.dp, vertical = 7.dp)
        ) {
            Text(
                text = "Edit",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(7.dp))
                .background(MpRed)
                .clickable { onDelete() }
                .padding(horizontal = 10.dp, vertical = 7.dp)
        ) {
            Text(
                text = "Delete",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun MpHeader(
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

            Text(
                text = "MANAGE PRODUCTS",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
