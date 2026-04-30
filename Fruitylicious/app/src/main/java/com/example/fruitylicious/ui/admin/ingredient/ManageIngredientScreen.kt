package com.example.fruitylicious.ui.admin.ingredients

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import com.example.fruitylicious.data.local.entity.IngredientEntity
import com.example.fruitylicious.ui.shared.AdminSideBarContent
import kotlinx.coroutines.launch

private val MiGreen = Color(0xFF2C8C44)
private val MiPageBg = Color(0xFFFFEAA0)
private val MiCardBg = Color.White
private val MiRed = Color(0xFFE53935)
private val MiTextMain = Color(0xFF1A1A1A)
private val MiTextSub = Color(0xFF757575)

private val DialogBgGray = Color(0xFFF5F5F5)
private val DialogBlueIcon = Color(0xFF2196F3)
private val DialogRedIcon = Color(0xFFE53935)
private val DialogBtnCancel = Color(0xFF5D6B60)
private val DialogBtnDelete = Color(0xFFFF8A80)
private val EditBtnColor = Color(0xFF333333)

@Composable
fun ManageIngredientsScreen(
    navController: NavController,
    adminName: String = "Admin User",
    onLogout: () -> Unit = {},
    viewModel: ManageIngredientsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var selectedBranch by remember { mutableStateOf("B1") }
    var searchQuery by remember { mutableStateOf("") }

    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var currentIngredient by remember { mutableStateOf<IngredientEntity?>(null) }

    val filteredIngredients = uiState.ingredients.filter {
        it.ingredientName.contains(searchQuery, ignoreCase = true) ||
                it.unitType.contains(searchQuery, ignoreCase = true)
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
                    .background(MiPageBg)
            ) {
                MiHeader(
                    selectedBranch = selectedBranch,
                    onBranchSelect = { selectedBranch = it },
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

                    Button(
                        onClick = {
                            currentIngredient = null
                            showEditDialog = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MiGreen),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "+ Add New Ingredient",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                    }

                    SearchBar(
                        value = searchQuery,
                        onValueChange = { searchQuery = it }
                    )

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
                            color = MiGreen,
                            fontSize = 12.sp
                        )
                    }

                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MiCardBg,
                        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                    ) {
                        if (filteredIngredients.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No ingredients found",
                                    color = MiTextSub
                                )
                            }
                        } else {
                            Column(
                                modifier = Modifier
                                    .verticalScroll(rememberScrollState())
                                    .padding(horizontal = 12.dp, vertical = 14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                filteredIngredients.forEach { ingredient ->
                                    IngredientRow(
                                        ingredient = ingredient,
                                        onEdit = {
                                            currentIngredient = ingredient
                                            showEditDialog = true
                                        },
                                        onDelete = {
                                            currentIngredient = ingredient
                                            showDeleteDialog = true
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (showEditDialog) {
                IngredientEditDialog(
                    ingredient = currentIngredient,
                    onDismiss = { showEditDialog = false },
                    onSave = { name, unit, weight, threshold, isPackaging, image ->
                        viewModel.saveIngredient(
                            existingIngredientId = currentIngredient?.ingredientId,
                            name = name,
                            unitType = unit,
                            estimatedWeightPerUnit = weight.toDoubleOrNull() ?: 0.0,
                            lowStockThreshold = threshold.toDoubleOrNull() ?: 0.0,
                            isPackaging = isPackaging,
                            image = image
                        )
                        showEditDialog = false
                    }
                )
            }

            if (showDeleteDialog && currentIngredient != null) {
                DeleteIngredientDialog(
                    ingredientName = currentIngredient!!.ingredientName,
                    onDismiss = { showDeleteDialog = false },
                    onConfirm = {
                        viewModel.deleteIngredient(currentIngredient!!.ingredientId)
                        showDeleteDialog = false
                    }
                )
            }
        }
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
            textStyle = TextStyle(fontSize = 14.sp, color = MiTextMain),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                if (value.isEmpty()) {
                    Text(
                        text = "Search ingredient",
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
private fun IngredientRow(
    ingredient: IngredientEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    RowCard {
        IconCircle(
            icon = Icons.Default.Inventory2,
            contentDescription = "Ingredient"
        )

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = ingredient.ingredientName,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MiTextMain
            )

            Text(
                text = buildString {
                    append(ingredient.unitType)
                    append(" | Low stock: ")
                    append(ingredient.lowStockThreshold)
                    if (ingredient.isPackaging) {
                        append(" | Packaging")
                    }
                },
                fontSize = 12.sp,
                color = MiTextSub
            )
        }

        RowButtons(
            onEdit = onEdit,
            onDelete = onDelete
        )
    }
}

@Composable
private fun IngredientEditDialog(
    ingredient: IngredientEntity?,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        unit: String,
        weight: String,
        threshold: String,
        isPackaging: Boolean,
        image: String
    ) -> Unit
) {
    var name by remember { mutableStateOf(ingredient?.ingredientName ?: "") }
    var selectedUnit by remember { mutableStateOf(ingredient?.unitType ?: "") }
    var weight by remember { mutableStateOf(ingredient?.estimatedWeightPerUnit?.toString() ?: "") }
    var threshold by remember { mutableStateOf(ingredient?.lowStockThreshold?.toString() ?: "") }
    var image by remember { mutableStateOf(ingredient?.image ?: "") }
    var packagingChecked by remember { mutableStateOf(ingredient?.isPackaging ?: false) }
    var unitDropdownExpanded by remember { mutableStateOf(false) }

    val units = listOf("pcs", "can", "pack", "grams", "ml", "box", "kg", "liter")

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
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Add / Edit Ingredient",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MiTextMain
                )

                Spacer(modifier = Modifier.height(20.dp))

                FieldBlock("Ingredient Name") {
                    DialogTextField(
                        value = name,
                        onValueChange = { name = it },
                        placeholder = "e.g. Mango"
                    )
                }

                FieldBlock("Unit Type") {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(DialogBgGray)
                                .border(
                                    width = if (unitDropdownExpanded) 1.5.dp else 0.dp,
                                    color = if (unitDropdownExpanded) MiGreen else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { unitDropdownExpanded = !unitDropdownExpanded }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = selectedUnit.ifEmpty { "Select unit type" },
                                color = if (selectedUnit.isEmpty()) Color.Gray else MiTextMain,
                                fontSize = 14.sp
                            )

                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = Color.Gray
                            )
                        }

                        DropdownMenu(
                            expanded = unitDropdownExpanded,
                            onDismissRequest = { unitDropdownExpanded = false },
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .background(Color.White)
                        ) {
                            units.forEach { unit ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = unit,
                                            fontSize = 14.sp,
                                            color = MiTextMain,
                                            fontWeight = FontWeight.Medium
                                        )
                                    },
                                    onClick = {
                                        selectedUnit = unit
                                        unitDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                FieldBlock("Estimated Weight") {
                    DialogTextField(
                        value = weight,
                        onValueChange = { weight = it },
                        placeholder = "0.00"
                    )
                }

                FieldBlock("Low Stock Threshold") {
                    DialogTextField(
                        value = threshold,
                        onValueChange = { threshold = it },
                        placeholder = "0.00"
                    )
                }

                FieldBlock("Image URI") {
                    DialogTextField(
                        value = image,
                        onValueChange = { image = it },
                        placeholder = "Optional image URI"
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Packaging",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MiTextMain,
                        modifier = Modifier.weight(1f)
                    )

                    Checkbox(
                        checked = packagingChecked,
                        onCheckedChange = { packagingChecked = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = Color(0xFF5C6BC0),
                            uncheckedColor = Color(0xFF5C6BC0)
                        )
                    )
                }

                FieldBlock("Insert Image") {
                    ImageUploadBox()
                }

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
                            color = MiTextMain,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = {
                            onSave(
                                name,
                                selectedUnit,
                                weight,
                                threshold,
                                packagingChecked,
                                image
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MiGreen),
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
private fun DeleteIngredientDialog(
    ingredientName: String,
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
                        tint = Color.White,
                        modifier = Modifier.size(34.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Delete Ingredient",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MiTextMain
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Are you sure you want to\nremove $ingredientName?",
                    fontSize = 14.sp,
                    color = MiTextSub,
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
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        Text(
                            text = "Delete Ingredient",
                            color = MiTextMain,
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
private fun FieldBlock(
    label: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = MiTextMain,
            modifier = Modifier.fillMaxWidth()
        )

        content()
    }

    Spacer(modifier = Modifier.height(14.dp))
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
        textStyle = TextStyle(fontSize = 14.sp, color = MiTextMain),
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
private fun ImageUploadBox() {
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
                tint = Color.Gray,
                modifier = Modifier.size(28.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Upload ingredient image",
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
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
            tint = MiGreen,
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
                .background(MiRed)
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
private fun MiHeader(
    selectedBranch: String,
    onBranchSelect: (String) -> Unit,
    onMenuClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MiGreen)
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
                text = "MANAGE INGREDIENTS",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

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
                        .background(if (isSelected) MiGreen else Color.Transparent)
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