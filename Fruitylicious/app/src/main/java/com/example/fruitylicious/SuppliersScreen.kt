package com.example.fruitylicious

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

// ── Data ──────────────────────────────────────────────────────────────────────

data class Supplier(
    val name: String,
    val category: String,
    val phone: String,
    val orders: String,
    val emoji: String,
    val emojiBg: Color
)

val sampleSuppliers = listOf(
    Supplier("Mango",        "Tropical Fruits", "+639123456789", "15", "🥭", Color(0xFFFFF9C4)),
    Supplier("Buko",         "Tropical Fruits", "+639123456789", "12", "🥥", Color(0xFFFFF9C4)),
    Supplier("Avocado",      "Tropical Fruits", "+639123456789", "07", "🥑", Color(0xFFE8F5E9)),
    Supplier("Dragon fruit", "Tropical Fruits", "+639123456789", "24", "🐉", Color(0xFFE8F5E9)),
    Supplier("Melon",        "Tropical Fruits", "+639123456789", "12", "🍈", Color(0xFFFCE4EC)),
    Supplier("Apple",        "Tropical Fruits", "+639123456789", "09", "🍎", Color(0xFFFBE9E7)),
)

// ── Screen ────────────────────────────────────────────────────────────────────

@Composable
fun SuppliersScreen(
    navController: NavController,
    drawerState: DrawerState,
    scope: CoroutineScope
) {
    var searchQuery by remember { mutableStateOf("") }

    val filtered = sampleSuppliers.filter {
        it.name.contains(searchQuery, ignoreCase = true) ||
        it.category.contains(searchQuery, ignoreCase = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFEAA0))
            .verticalScroll(rememberScrollState())
    ) {

        // ── Green Header ──────────────────────────────────────────────
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
                // Hamburger
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
                // Avatar
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
                text = "Suppliers",
                color = Color.White,
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "6 active suppliers",
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 13.sp
            )
        }

        // ── Body ──────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {

            // ── Search Bar ────────────────────────────────────────────
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(50.dp),
                color = Color.White,
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🔍",
                        fontSize = 16.sp,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        decorationBox = { inner ->
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Search suppliers...",
                                    color = Color(0xFFAAAAAA),
                                    fontSize = 14.sp
                                )
                            }
                            inner()
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Supplier Cards ────────────────────────────────────────
            filtered.forEach { supplier ->
                SupplierCard(supplier = supplier)
                Spacer(modifier = Modifier.height(10.dp))
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    // ── FAB ───────────────────────────────────────────────────────────
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

// ── Supplier Card ─────────────────────────────────────────────────────────────

@Composable
fun SupplierCard(supplier: Supplier) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Emoji icon
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(supplier.emojiBg),
                contentAlignment = Alignment.Center
            ) {
                Text(text = supplier.emoji, fontSize = 30.sp)
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Name + category + phone
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = supplier.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1B1B1B)
                )
                Text(
                    text = supplier.category,
                    fontSize = 12.sp,
                    color = Color(0xFF888888)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "📞",
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = supplier.phone,
                        fontSize = 12.sp,
                        color = Color(0xFF2E7D32),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Orders count
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = supplier.orders,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFFFA000)
                )
                Text(
                    text = "ORDERS",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFA000),
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

// ── Preview ───────────────────────────────────────────────────────────────────

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SuppliersScreenPreview() {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    SuppliersScreen(
        navController = rememberNavController(),
        drawerState = drawerState,
        scope = scope
    )
}
