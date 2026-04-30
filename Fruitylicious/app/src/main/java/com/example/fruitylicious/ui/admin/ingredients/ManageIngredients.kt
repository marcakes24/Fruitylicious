package com.example.fruitylicious.ui.admin.ingredients

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
import androidx.compose.material.icons.filled.KeyboardArrowDown
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.fruitylicious.data.local.entity.AppDatabase
import com.example.fruitylicious.data.local.entity.IngredientEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.UUID

// ── Colors ────────────────────────────────────────────────────────────────────
private val MpGreen         = Color(0xFF2C8C44)
private val MpGreenDark     = Color(0xFF1B5E20)
private val MpPageBg        = Color(0xFFFFEAA0)
private val MpCardBg        = Color.White
private val MpRed           = Color(0xFFE53935)
private val MpTextMain      = Color(0xFF1A1A1A)
private val MpTextSub       = Color(0xFF757575)
private val DialogBgGray    = Color(0xFFF5F5F5)
private val DialogBlueIcon  = Color(0xFF2196F3)
private val DialogRedIcon   = Color(0xFFE53935)
private val DialogBtnCancel = Color(0xFF5D6B60)
private val DialogBtnDelete = Color(0xFFFF8A80)
private val EditBtnColor    = Color(0xFF333333)

@Composable
fun ManageIngredientsScreen(
    drawerState: DrawerState,
    scope: CoroutineScope
) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val ingredientDao = db.ingredientDao()

    val ingredients by ingredientDao.getAll().collectAsState(initial = emptyList())

    var selectedBranch by remember { mutableStateOf("B1") }
    var searchQuery by remember { mutableStateOf("") }

    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var currentIngredient by remember { mutableStateOf<IngredientEntity?>(null) }

    val filteredIngredients = ingredients.filter {
        it.ingredientName.contains(searchQuery, ignoreCase = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MpPageBg)
    ) {
        // ── Header ────────────────────────────────────────────────────────────
        MiHeader(
            selectedBranch = selectedBranch,
            onBranchSelect = { selectedBranch = it },
            onMenuClick = { scope.launch { drawerState.open() } }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // ── Add Button ────────────────────────────────────────────────────
            Button(
                onClick = {
                    currentIngredient = null
                    showEditDialog = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MpGreen),
                shape = RoundedCornerShape(10.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Text(
                    "+ Add New Ingredient",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
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
                        if (searchQuery.isEmpty()) Text("Search ingredient, staff, etc.", color = Color.LightGray, fontSize = 14.sp)
                        inner()
                    },
                    singleLine = true
                )
            }

            // ── Ingredient List Card ──────────────────────────────────────────
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MpCardBg,
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
            ) {
                if (filteredIngredients.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No ingredients found", color = MpTextSub)
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

    // ── Dialogs ───────────────────────────────────────────────────────────────
    if (showEditDialog) {
        IngredientEditDialog(
            ingredient = currentIngredient,
            onDismiss = { showEditDialog = false },
            onSave = { name, unit, weight, threshold, isPackaging ->
                scope.launch {
                    val entity = IngredientEntity(
                        ingredientId = currentIngredient?.ingredientId ?: UUID.randomUUID().toString(),
                        ingredientName = name,
                        unitType = unit,
                        estimatedWeightPerUnit = weight.toDoubleOrNull() ?: 0.0,
                        isPackaging = isPackaging,
                        lastModified = System.currentTimeMillis()
                    )
                    if (currentIngredient == null) ingredientDao.insert(entity)
                    else ingredientDao.update(entity)
                    showEditDialog = false
                }
            }
        )
    }
    if (showDeleteDialog && currentIngredient != null) {
        DeleteIngredientDialog(
            ingredientName = currentIngredient!!.ingredientName,
            onDismiss = { showDeleteDialog = false },
            onConfirm = {
                scope.launch {
                    ingredientDao.delete(currentIngredient!!)
                    showDeleteDialog = false
                }
            }
        )
    }
}

// ── Ingredient Row ────────────────────────────────────────────────────────────
@Composable
private fun IngredientRow(
    ingredient: IngredientEntity,
    onEdit: () -> Unit,
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
        // Circular icon
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Text(ingredientEmoji(ingredient.ingredientName), fontSize = 22.sp)
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                ingredient.ingredientName,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MpTextMain
            )
            Text(
                ingredient.unitType,
                fontSize = 12.sp,
                color = MpTextSub
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

// ── Add / Edit Dialog ─────────────────────────────────────────────────────────
@Composable
fun IngredientEditDialog(
    ingredient: IngredientEntity?,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, Boolean) -> Unit
) {
    var name           by remember { mutableStateOf(ingredient?.ingredientName ?: "") }
    var selectedUnit   by remember { mutableStateOf(ingredient?.unitType ?: "") }
    var weight         by remember { mutableStateOf(ingredient?.estimatedWeightPerUnit?.toString() ?: "") }
    var threshold      by remember { mutableStateOf("") }
    var packagingChecked by remember { mutableStateOf(ingredient?.isPackaging ?: false) }
    var unitDropdownExpanded by remember { mutableStateOf(false) }

    val units = listOf("pcs", "can", "pack", "grams", "ml")

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
                    "Add / Edit Ingredient",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MpTextMain
                )

                Spacer(modifier = Modifier.height(20.dp))

                // ── Ingredient Name ───────────────────────────────────────────
                MiDialogLabel("Ingredient Name")
                Spacer(modifier = Modifier.height(4.dp))
                MiDialogTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = "e.g. Mango"
                )

                Spacer(modifier = Modifier.height(14.dp))

                // ── Unit Type Dropdown ────────────────────────────────────────
                MiDialogLabel("Unit Type")
                Spacer(modifier = Modifier.height(4.dp))

                // Full-width dropdown trigger
                Box(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(DialogBgGray)
                            .border(
                                width = if (unitDropdownExpanded) 1.5.dp else 0.dp,
                                color = if (unitDropdownExpanded) MpGreen else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { unitDropdownExpanded = !unitDropdownExpanded }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = selectedUnit.ifEmpty { "Select unit type" },
                            color = if (selectedUnit.isEmpty()) Color.Gray else MpTextMain,
                            fontSize = 14.sp
                        )
                        Icon(
                            Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = Color.Gray
                        )
                    }

                    // Dropdown menu — full width, styled
                    DropdownMenu(
                        expanded = unitDropdownExpanded,
                        onDismissRequest = { unitDropdownExpanded = false },
                        modifier = Modifier
                            .fillMaxWidth(0.85f) // match dialog width
                            .background(Color.White)
                            .border(1.dp, Color(0xFFCCCCCC), RoundedCornerShape(8.dp))
                    ) {
                        units.forEach { unit ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = unit,
                                        fontSize = 14.sp,
                                        color = MpTextMain,
                                        fontWeight = FontWeight.Medium
                                    )
                                },
                                onClick = {
                                    selectedUnit = unit
                                    unitDropdownExpanded = false
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        if (selectedUnit == unit) Color(0xFFE8F5E9) else Color.Transparent
                                    ),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // ── Estimated Weight ──────────────────────────────────────────
                MiDialogLabel("Estimated Weight (optional)")
                Spacer(modifier = Modifier.height(4.dp))
                MiDialogTextField(
                    value = weight,
                    onValueChange = { weight = it },
                    placeholder = "e.g."
                )

                Spacer(modifier = Modifier.height(14.dp))

                // ── Stock Threshold ───────────────────────────────────────────
                MiDialogLabel("Stock threshold")
                Spacer(modifier = Modifier.height(4.dp))
                MiDialogTextField(
                    value = threshold,
                    onValueChange = { threshold = it },
                    placeholder = "e.g."
                )

                Spacer(modifier = Modifier.height(14.dp))

                // ── Packaging Checkbox ────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Packaging",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MpTextMain,
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

                Spacer(modifier = Modifier.height(14.dp))

                // ── Insert Image ──────────────────────────────────────────────
                MiDialogLabel("Insert Image")
                Spacer(modifier = Modifier.height(4.dp))
                MiImageUploadBox()

                Spacer(modifier = Modifier.height(24.dp))

                // ── Action Buttons ────────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Discard
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEEEEEE)),
                        shape = RoundedCornerShape(10.dp),
                        elevation = ButtonDefaults.buttonElevation(0.dp)
                    ) {
                        Text(
                            "Discard",
                            color = MpTextMain,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                    // Save
                    Button(
                        onClick = {
                            if (name.isNotEmpty() && selectedUnit.isNotEmpty()) {
                                onSave(name, selectedUnit, weight, threshold, packagingChecked)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MpGreen),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            "Save",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }
}

// ── Delete Dialog ─────────────────────────────────────────────────────────────
@Composable
fun DeleteIngredientDialog(
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
                    "Delete Ingredient",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MpTextMain
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    "Are you sure you want to\nremove this ingredient?",
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
                    // Cancel button — dark gray
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DialogBtnCancel),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            "Cancel",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                    // Delete Ingredient button — light red/salmon
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier
                            .weight(1.3f)
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DialogBtnDelete),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            "Delete Ingredient",
                            color = MpTextMain,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

// ── Reusable UI Helpers ───────────────────────────────────────────────────────

@Composable
private fun MiDialogLabel(text: String) {
    Text(
        text = text,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = MpTextMain,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun MiDialogTextField(
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
            if (value.isEmpty()) Text(placeholder, color = Color.Gray, fontSize = 14.sp)
            inner()
        },
        singleLine = true
    )
}

@Composable
private fun MiImageUploadBox() {
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
                Icons.Default.Image,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text("Upload ingredient image", fontSize = 12.sp, color = Color.Gray)
        }
    }
}

// ── Header ────────────────────────────────────────────────────────────────────
@Composable
private fun MiHeader(
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
                text = "MANAGE INGREDIENTS",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Branch Selector Toggle
        Surface(
            color = MpGreenDark,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.height(32.dp).align(Alignment.CenterEnd)
        ) {
            Row(
                modifier = Modifier.padding(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf("B1", "B2", "All").forEach { branch ->
                    val isSelected = selectedBranch == branch
                    Surface(
                        color = if (isSelected) Color.White else Color.Transparent,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .fillMaxHeight()
                            .clickable { onBranchSelect(branch) }
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        ) {
                            Text(
                                text = branch,
                                color = if (isSelected) MpGreen else Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Emoji Helper ──────────────────────────────────────────────────────────────
private fun ingredientEmoji(name: String): String = when {
    name.contains("Dragon", ignoreCase = true)     -> "🔴"
    name.contains("Mango", ignoreCase = true)       -> "🥭"
    name.contains("Avocado", ignoreCase = true)     -> "🥑"
    name.contains("Cheese", ignoreCase = true)      -> "🧀"
    name.contains("Strawberry", ignoreCase = true)  -> "🍓"
    name.contains("Milk", ignoreCase = true)        -> "🥛"
    name.contains("Oreo", ignoreCase = true)        -> "🍪"
    name.contains("Buko", ignoreCase = true)        -> "🥥"
    name.contains("Condensed", ignoreCase = true)   -> "🥫"
    else -> "📦"
}