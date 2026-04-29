package com.example.fruitylicious

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

// Data model for functional queue
data class OrderItem(
    val id: String,
    val customer: String,
    val trxId: String,
    val items: List<Pair<String, String>>,
    var status: String, // "Pending", "Preparing", "Ready", "Completed"
    val total: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueScreen(
    navController: NavController,
    drawerState: DrawerState,
    scope: CoroutineScope
) {
    // Shared state for all 4 stages
    val orders = remember { mutableStateListOf(
        OrderItem("1", "Mari", "TRX-875086", listOf("1x Avocado (Medium)" to "P60.00"), "Pending", "P60.00"),
        OrderItem("2", "Jaime", "TRX-441638", listOf("1x Cheesecake+ Oreo (Medium)" to "P85.00", "1x Melon (Large)" to "P90.00"), "Pending", "P175.00"),
        OrderItem("3", "Dangca", "TRX-134195", listOf("1x Apple (Medium)" to "P80.00"), "Pending", "P80.00"),
        OrderItem("1", "Jaime", "TRX-002702", listOf("1x Cheesecake+ Oreo (Medium)" to "P85.00", "1x Melon (Large)" to "P90.00"), "Preparing", "P175.00"),
        OrderItem("1", "Mari", "TRX-661373", listOf("1x Avocado (Medium)" to "P60.00"), "Ready", "P60.00"),
        OrderItem("4", "Mari", "TRX-661373", listOf("1x Avocado (Medium)" to "P60.00"), "Completed", "P60.00")
    )}

    var selectedTab by remember { mutableStateOf("Pending") }
    val filteredOrders = orders.filter { it.status == selectedTab }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("ORDER QUEUE", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                        Icon(Icons.Default.Menu, contentDescription = null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color(0xFF2E7D32))
            )
        },
        containerColor = Color(0xFFFDEB95)
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Status Tabs - Navigates between the 4 pages
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatusTab(orders.count { it.status == "Pending" }.toString(), "Pending", selectedTab == "Pending", Modifier.weight(1f)) { selectedTab = "Pending" }
                StatusTab(orders.count { it.status == "Preparing" }.toString(), "Preparing", selectedTab == "Preparing", Modifier.weight(1f)) { selectedTab = "Preparing" }
                StatusTab(orders.count { it.status == "Ready" }.toString(), "Ready", selectedTab == "Ready", Modifier.weight(1f)) { selectedTab = "Ready" }
                StatusTab(orders.count { it.status == "Completed" }.toString(), "Completed", selectedTab == "Completed", Modifier.weight(1f)) { selectedTab = "Completed" }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 20.dp)
            ) {
                items(filteredOrders) { order ->
                    OrderCard(
                        order = order,
                        onActionClick = {
                            val index = orders.indexOf(order)
                            when (order.status) {
                                "Pending" -> orders[index] = order.copy(status = "Preparing")
                                "Preparing" -> orders[index] = order.copy(status = "Ready")
                                "Ready" -> orders[index] = order.copy(status = "Completed")
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun OrderCard(order: OrderItem, onActionClick: () -> Unit) {
    val (buttonText, buttonColor) = when (order.status) {
        "Pending" -> "Start Preparing" to Color(0xFF4267B2)
        "Preparing" -> "Mark as Ready" to Color(0xFF2E7D32)
        "Ready" -> "Complete Order" to Color(0xFF37474F)
        else -> "" to Color.Transparent
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = CircleShape, color = Color(0xFFFFD54F), modifier = Modifier.size(24.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(order.id, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.width(8.dp))
                Text(order.status, color = if(order.status == "Preparing") Color(0xFF4267B2) else Color(0xFFFFA000), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(Modifier.weight(1f))
                Surface(color = Color.Black, shape = RoundedCornerShape(4.dp)) {
                    Text(order.trxId, color = Color.White, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
            }

            Spacer(Modifier.height(12.dp))
            Text(order.customer, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("Cash", fontSize = 12.sp, color = Color.Gray)

            Spacer(Modifier.height(12.dp))
            order.items.forEach { (name, price) ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(name, fontSize = 13.sp, color = Color.DarkGray)
                    Text(price, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }

            HorizontalDivider(Modifier.padding(vertical = 12.dp), thickness = 0.5.dp)

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Total", fontSize = 10.sp, color = Color.Gray)
                    Text(order.total, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                }
                Spacer(Modifier.weight(1f))

                if (order.status != "Completed") {
                    Button(
                        onClick = onActionClick,
                        colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(buttonText, fontSize = 12.sp)
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.Default.ArrowForward, null, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun StatusTab(count: String, label: String, isSelected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val activeColor = if (label == "Ready") Color(0xFF2E7D32) else Color(0xFF4267B2)

    Card(
        onClick = onClick,
        modifier = modifier.height(70.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().border(
                width = if (isSelected) 2.dp else 0.dp,
                color = if (isSelected) activeColor else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(count, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = if (isSelected) activeColor else Color.Black)
            Text(label, fontSize = 10.sp, color = Color.Gray)
        }
    }
}