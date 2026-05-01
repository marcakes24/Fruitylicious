package com.example.fruitylicious.ui.shared.inventory

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.fruitylicious.ui.shared.SharedDrawerContent
import com.example.fruitylicious.ui.shared.SharedScreenMode
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val IaGreen = Color(0xFF2E7D32)
private val IaPageBg = Color(0xFFFFEAA0)
private val IaTextMain = Color(0xFF1E293B)
private val IaTextSub = Color(0xFF64748B)
private val IaRed = Color(0xFFB91C1C)

@Composable
fun InventoryAdjustmentScreen(
    navController: NavController,
    mode: SharedScreenMode = SharedScreenMode.ADMIN,
    userName: String = "User",
    branchName: String = "",
    onLogout: () -> Unit = {},
    viewModel: InventoryAdjustmentViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var selectedIngredient by remember { mutableStateOf<AdjustmentIngredientRow?>(null) }
    var adjustmentType by remember { mutableStateOf("Add") }
    var quantity by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    var dropdownExpanded by remember { mutableStateOf(false) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color.Transparent,
                drawerTonalElevation = 0.dp
            ) {
                SharedDrawerContent(
                    mode = mode,
                    navController = navController,
                    drawerState = drawerState,
                    scope = scope,
                    userName = userName,
                    branchName = branchName,
                    onLogout = onLogout
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(IaPageBg)
        ) {
            Header(
                onMenuClick = {
                    scope.launch {
                        drawerState.open()
                    }
                }
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 20.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    NewAdjustmentCard(
                        ingredients = uiState.ingredients,
                        selectedIngredient = selectedIngredient,
                        adjustmentType = adjustmentType,
                        quantity = quantity,
                        reason = reason,
                        dropdownExpanded = dropdownExpanded,
                        onDropdownExpandedChange = { dropdownExpanded = it },
                        onIngredientSelected = {
                            selectedIngredient = it
                            dropdownExpanded = false
                            viewModel.clearMessages()
                        },
                        onTypeSelected = {
                            adjustmentType = it
                            viewModel.clearMessages()
                        },
                        onQuantityChange = {
                            if (it.all { char -> char.isDigit() || char == '.' }) {
                                quantity = it
                            }
                        },
                        onReasonChange = {
                            reason = it
                        },
                        onSubmit = {
                            viewModel.submitAdjustment(
                                ingredient = selectedIngredient,
                                type = adjustmentType,
                                quantityText = quantity,
                                reason = reason
                            )

                            quantity = ""
                            reason = ""
                            selectedIngredient = null
                        }
                    )
                }

                if (!uiState.error.isNullOrBlank()) {
                    item {
                        Text(
                            text = uiState.error ?: "",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 13.sp
                        )
                    }
                }

                if (!uiState.successMessage.isNullOrBlank()) {
                    item {
                        Text(
                            text = uiState.successMessage ?: "",
                            color = IaGreen,
                            fontSize = 13.sp
                        )
                    }
                }

                item {
                    RecentAdjustmentsCard(
                        items = uiState.history
                    )
                }
            }
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
            .background(IaGreen)
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
                text = "INVENTORY ADJUSTMENT",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun NewAdjustmentCard(
    ingredients: List<AdjustmentIngredientRow>,
    selectedIngredient: AdjustmentIngredientRow?,
    adjustmentType: String,
    quantity: String,
    reason: String,
    dropdownExpanded: Boolean,
    onDropdownExpandedChange: (Boolean) -> Unit,
    onIngredientSelected: (AdjustmentIngredientRow) -> Unit,
    onTypeSelected: (String) -> Unit,
    onQuantityChange: (String) -> Unit,
    onReasonChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
    val isFormValid = selectedIngredient != null && quantity.isNotBlank()

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "New Adjustment",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = IaTextMain
            )

            Column {
                Label("Select Ingredient")

                Spacer(modifier = Modifier.height(8.dp))

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedIngredient?.ingredientName ?: "",
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text("Choose an ingredient", color = Color.LightGray)
                        },
                        trailingIcon = {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = Color(0xFFE2E8F0),
                            focusedBorderColor = IaGreen
                        )
                    )

                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { onDropdownExpandedChange(true) }
                    )

                    DropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { onDropdownExpandedChange(false) },
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .background(Color.White)
                            .heightIn(max = 400.dp)
                    ) {
                        ingredients.forEach { ingredient ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = ingredient.ingredientName,
                                            fontSize = 14.sp,
                                            color = IaTextMain
                                        )

                                        Text(
                                            text = "${formatQuantity(ingredient.currentStock)} ${ingredient.unitType}",
                                            fontSize = 12.sp,
                                            color = Color.Gray
                                        )
                                    }
                                },
                                onClick = { onIngredientSelected(ingredient) }
                            )
                        }
                    }
                }
            }

            if (selectedIngredient != null) {
                CurrentStockCard(selectedIngredient)
            }

            Column {
                Label("Adjustment Type")

                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AdjustmentTypeButton(
                        label = "Add Stock",
                        iconAdd = true,
                        selected = adjustmentType == "Add",
                        selectedColor = IaGreen,
                        onClick = { onTypeSelected("Add") },
                        modifier = Modifier.weight(1f)
                    )

                    AdjustmentTypeButton(
                        label = "Reduce Stock",
                        iconAdd = false,
                        selected = adjustmentType == "Reduce",
                        selectedColor = IaRed,
                        onClick = { onTypeSelected("Reduce") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Column {
                Label("Quantity ${selectedIngredient?.unitType?.let { "($it)" } ?: ""}")

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = quantity,
                    onValueChange = onQuantityChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text("Enter quantity", color = Color.LightGray)
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = Color(0xFFE2E8F0),
                        focusedBorderColor = IaGreen
                    ),
                    singleLine = true
                )
            }

            Column {
                Label("Reason")

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = reason,
                    onValueChange = onReasonChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp),
                    placeholder = {
                        Text("Enter reason for adjustment", color = Color.LightGray)
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = Color(0xFFE2E8F0),
                        focusedBorderColor = IaGreen
                    )
                )
            }

            Button(
                onClick = onSubmit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = isFormValid,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = IaGreen,
                    disabledContainerColor = Color(0xFFCBD5E1)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = if (isFormValid) Color.White else Color(0xFF94A3B8),
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "Submit Adjustment",
                    color = if (isFormValid) Color.White else Color(0xFF94A3B8),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun Label(text: String) {
    Text(
        text = text,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color(0xFF475569)
    )
}

@Composable
private fun CurrentStockCard(
    ingredient: AdjustmentIngredientRow
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFFFFFBEB),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Current Stock",
                color = Color(0xFF92400E),
                fontSize = 14.sp
            )

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = formatQuantity(ingredient.currentStock),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.Black
                )

                Spacer(modifier = Modifier.width(4.dp))

                Text(
                    text = ingredient.unitType,
                    fontSize = 14.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun AdjustmentTypeButton(
    label: String,
    iconAdd: Boolean,
    selected: Boolean,
    selectedColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(60.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = if (selected) selectedColor.copy(alpha = 0.08f) else Color.White,
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) selectedColor else Color(0xFFE2E8F0)
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (iconAdd) Icons.Default.Add else Icons.Default.Remove,
                contentDescription = null,
                tint = if (selected) selectedColor else Color.Gray,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = label,
                textAlign = TextAlign.Center,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) selectedColor else Color.Gray,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun RecentAdjustmentsCard(
    items: List<AdjustmentHistoryRow>
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    tint = Color(0xFF475569),
                    modifier = Modifier.size(18.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "Recent Adjustments",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF334155)
                )
            }

            HorizontalDivider(color = Color(0xFFF1F5F9))

            if (items.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No adjustments found",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
            } else {
                items.forEachIndexed { index, item ->
                    AdjustmentItem(item)

                    if (index < items.size - 1) {
                        HorizontalDivider(color = Color(0xFFF1F5F9))
                    }
                }
            }
        }
    }
}

@Composable
private fun AdjustmentItem(
    item: AdjustmentHistoryRow
) {
    val isAdd = item.adjustmentType == "Add"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.ingredientName,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = IaTextMain
            )

            Text(
                text = item.reason,
                fontSize = 12.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Branch ${item.branchId}",
                fontSize = 12.sp,
                color = Color.Gray
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Surface(
                color = if (isAdd) IaGreen else IaRed,
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = "${if (isAdd) "+" else "-"}${formatQuantity(item.quantity)}",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = formatDate(item.dateTime),
                fontSize = 11.sp,
                color = Color.Gray
            )

            Text(
                text = formatTime(item.dateTime),
                fontSize = 11.sp,
                color = Color.Gray
            )
        }
    }
}

private fun formatQuantity(value: Double): String {
    return if (value % 1.0 == 0.0) {
        value.toInt().toString()
    } else {
        String.format(Locale.US, "%.2f", value)
    }
}

private fun formatDate(timestamp: Long): String {
    return SimpleDateFormat("M/d/yyyy", Locale.US).format(Date(timestamp))
}

private fun formatTime(timestamp: Long): String {
    return SimpleDateFormat("h:mm a", Locale.US).format(Date(timestamp))
}