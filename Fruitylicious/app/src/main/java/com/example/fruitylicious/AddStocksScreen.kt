package com.example.fruitylicious

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
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

// ── Data ──────────────────────────────────────────────────────────────────────

data class StockProduct(
    val name: String,
    val count: String,
    val emoji: String,
    val emojiBg: Color,
    val progressColor: Color,
    val progressFraction: Float
)

val sampleStockProducts = listOf(
    StockProduct("Mango",       "20 products", "🥭", Color(0xFFFFF9C4), Color(0xFF4CAF50), 0.75f),
    StockProduct("Buko",        "14 products", "🥥", Color(0xFFFCE4EC), Color(0xFFFFA000), 0.45f),
    StockProduct("Dragon Fruit","2 products",  "🐉", Color(0xFFE8F5E9), Color(0xFFE53935), 0.10f),
    StockProduct("Melon",       "2 products",  "🍈", Color(0xFFFFF9C4), Color(0xFFE53935), 0.10f),
    StockProduct("Pineapple",   "2 products",  "🍍", Color(0xFFFFF9C4), Color(0xFFE53935), 0.10f),
    StockProduct("Dragonfruit", "2 products",  "🐉", Color(0xFFFCE4EC), Color(0xFFE53935), 0.10f),
)

val stockFilterTabs = listOf("All", "🍎 Fruits", "🧊 Tube Ice", "+ Straws & Cups", "🍪 Graham Cra...")

// ── Screen ────────────────────────────────────────────────────────────────────

@Composable
fun AddStocksScreen(
    navController: NavController,
    drawerState: DrawerState,
    scope: CoroutineScope
) {
    var selectedTab       by remember { mutableStateOf("All") }
    var editingProduct    by remember { mutableStateOf<StockProduct?>(null) }
    var deletingProduct   by remember { mutableStateOf<StockProduct?>(null) }

    // ── Delete Confirmation Dialog ─────────────────────────────
    if (deletingProduct != null) {
        Dialog(onDismissRequest = { deletingProduct = null }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                modifier = Modifier.width(280.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Trash icon
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFFFFF3E0)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🗑️", fontSize = 36.sp)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Are you sure you want to\ndelete this product?",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF1B1B1B),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Yes, Delete button
                        Button(
                            onClick = { deletingProduct = null },
                            modifier = Modifier.weight(1f).height(44.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333))
                        ) {
                            Text("Yes, Delete", color = Color.White, fontSize = 13.sp)
                        }
                        // Cancel button
                        OutlinedButton(
                            onClick = { deletingProduct = null },
                            modifier = Modifier.weight(1f).height(44.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Cancel", color = Color(0xFF333333), fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }

    // ── Edit Dialog ────────────────────────────────────────────
    if (editingProduct != null) {
        val product = editingProduct!!
        var quantity by remember { mutableStateOf("") }
        var notes    by remember { mutableStateOf("") }
        var counter  by remember { mutableStateOf(0) }

        Dialog(onDismissRequest = { editingProduct = null }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                modifier = Modifier.width(300.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {

                    // Product header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(product.emojiBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(product.emoji, fontSize = 24.sp)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = product.name,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1B1B1B)
                                )
                                Text(
                                    text = product.count,
                                    fontSize = 12.sp,
                                    color = Color(0xFF888888)
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF2E7D32))
                                .padding(horizontal = 12.dp, vertical = 5.dp)
                        ) {
                            Text("Save", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Progress bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFE0E0E0))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(product.progressFraction)
                                .height(4.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(product.progressColor)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Edit Quantity label
                    Text(
                        text = "Edit Quantity",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1B1B1B)
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    // Quantity input + counter
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = quantity,
                            onValueChange = { quantity = it },
                            placeholder = {
                                Text("Enter quantity here", fontSize = 12.sp, color = Color(0xFFAAAAAA))
                            },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = Color(0xFFE0E0E0),
                                focusedBorderColor = Color(0xFF2E7D32)
                            ),
                            singleLine = true
                        )
                        // Minus button
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFEEEEEE))
                                .clickable { if (counter > 0) counter-- },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("-", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF333333))
                        }
                        // Plus button
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF2E7D32))
                                .clickable { counter++ },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        // Save small button
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF2E7D32))
                                .padding(horizontal = 10.dp, vertical = 10.dp)
                                .clickable { },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Save", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Notes label
                    Text(
                        text = "Notes",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1B1B1B)
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        placeholder = {
                            Text("Enter quantity here", fontSize = 12.sp, color = Color(0xFFAAAAAA))
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = Color(0xFFE0E0E0),
                            focusedBorderColor = Color(0xFF2E7D32)
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Save Changes / Cancel buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { editingProduct = null },
                            modifier = Modifier.weight(1f).height(46.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                        ) {
                            Text("Save Changes", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = { editingProduct = null },
                            modifier = Modifier.weight(1f).height(46.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Cancel", color = Color(0xFF333333), fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }

    // ── Main Screen ────────────────────────────────────────────
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFEAA0))
            .verticalScroll(rememberScrollState())
    ) {

        // ── Green Header ──────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF2E7D32))
                .padding(start = 20.dp, end = 20.dp, top = 48.dp, bottom = 28.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.clickable { scope.launch { drawerState.open() } },
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    repeat(3) {
                        Box(
                            modifier = Modifier
                                .width(22.dp)
                                .height(2.dp)
                                .background(Color.White, RoundedCornerShape(1.dp))
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(Color(0xFFC62828), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("E1", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Add Fruits/Stocks",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Add & Edit your stocks here",
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 13.sp
            )
        }

        // ── Body ──────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {

            // ── Filter Tabs ───────────────────────────────────────
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                stockFilterTabs.forEach { tab ->
                    val selected = tab == selectedTab
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (selected) Color(0xFF2E7D32) else Color.White)
                            .clickable { selectedTab = tab }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = tab,
                            color = if (selected) Color.White else Color(0xFF555555),
                            fontSize = 12.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Product List ──────────────────────────────────────
            sampleStockProducts.forEach { product ->
                StockProductRow(
                    product = product,
                    onEditClick   = { editingProduct  = product },
                    onDeleteClick = { deletingProduct = product }
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    // ── FAB ───────────────────────────────────────────────────────
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .size(52.dp)
                .background(Color(0xFFFFA000), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("+", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// ── Stock Product Row ─────────────────────────────────────────────────────────

@Composable
fun StockProductRow(
    product: StockProduct,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Emoji
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(product.emojiBg),
                    contentAlignment = Alignment.Center
                ) {
                    Text(product.emoji, fontSize = 28.sp)
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = product.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1B1B1B)
                    )
                    Text(
                        text = product.count,
                        fontSize = 12.sp,
                        color = Color(0xFF888888)
                    )
                }

                // Edit icon
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFFFF9C4))
                        .clickable { onEditClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("✏️", fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Delete icon
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFFCE4EC))
                        .clickable { onDeleteClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("🗑️", fontSize = 16.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFFE0E0E0))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(product.progressFraction)
                        .height(5.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(product.progressColor)
                )
            }
        }
    }
}

// ── Preview ───────────────────────────────────────────────────────────────────

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun AddStocksScreenPreview() {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    AddStocksScreen(
        navController = rememberNavController(),
        drawerState = drawerState,
        scope = scope
    )
}
