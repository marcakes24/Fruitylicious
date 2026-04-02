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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

// ── Data ──────────────────────────────────────────────────────────────────────

data class MovementItem(
    val name: String,
    val type: String,       // "Stock in", "Stock out", "Adjust"
    val time: String,
    val source: String,
    val amount: String,     // e.g. "+200 kg" or "-45 kg"
    val isPositive: Boolean
)

data class MovementGroup(
    val dateLabel: String,
    val items: List<MovementItem>
)

val sampleMovements = listOf(
    MovementGroup(
        dateLabel = "TODAY - MARCH 8",
        items = listOf(
            MovementItem("Mango",       "Stock in",  "8:30",  "TropicSource", "+200 kg", true),
            MovementItem("Avocoda",     "Stock out", "10:15 AM", "Sales",     "-45 kg",  false),
            MovementItem("Dragon fruit","Adjust",    "11:00 AM", "Spoilage",  "-5 kg",   false),
        )
    ),
    MovementGroup(
        dateLabel = "YESTERDAY - MARCH 7",
        items = listOf(
            MovementItem("Melon",    "Stock in",  "3:00",    "Melonking", "+60 kg",  true),
            MovementItem("Guyabano", "Stock out", "5:45 PM", "Sales",     "-30 kg",  false),
            MovementItem("Buko",     "Stock in",  "6:20 PM", "BerryBest", "+40 kg",  true),
        )
    )
)

val movementFilters = listOf("All", "Stock in", "Stock out", "Date")

// ── Screen ────────────────────────────────────────────────────────────────────

@Composable
fun MovementsScreen(
    navController: NavController,
    drawerState: DrawerState,
    scope: CoroutineScope
) {
    var selectedFilter by remember { mutableStateOf("All") }

    val filteredGroups = sampleMovements.map { group ->
        group.copy(
            items = group.items.filter { item ->
                when (selectedFilter) {
                    "Stock in"  -> item.type == "Stock in"
                    "Stock out" -> item.type == "Stock out"
                    else        -> true
                }
            }
        )
    }.filter { it.items.isNotEmpty() }

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
                text = "Movements",
                color = Color.White,
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "All stock in/out transactions",
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

            // ── Filter Tabs ───────────────────────────────────────────
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                movementFilters.forEach { filter ->
                    val selected = filter == selectedFilter
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (selected) Color(0xFF2E7D32) else Color.White
                            )
                            .clickable { selectedFilter = filter }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (filter == "Stock in")  Text("📥", fontSize = 11.sp)
                            if (filter == "Stock out") Text("📤", fontSize = 11.sp)
                            if (filter == "Date")      Text("📅", fontSize = 11.sp)
                            Text(
                                text = filter,
                                color = if (selected) Color.White else Color(0xFF555555),
                                fontSize = 13.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Movement Groups ───────────────────────────────────────
            filteredGroups.forEach { group ->
                // Date label
                Text(
                    text = group.dateLabel,
                    color = Color(0xFFA07840),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                    modifier = Modifier.padding(bottom = 10.dp)
                )

                group.items.forEach { item ->
                    MovementRow(item = item)
                    Spacer(modifier = Modifier.height(10.dp))
                }

                Spacer(modifier = Modifier.height(10.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ── Movement Row ──────────────────────────────────────────────────────────────

@Composable
fun MovementRow(item: MovementItem) {
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
            // Color dot indicator
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            item.isPositive         -> Color(0xFF4CAF50)
                            item.type == "Adjust"   -> Color(0xFFFFC107)
                            else                    -> Color(0xFFE53935)
                        }
                    )
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Name + type + time + source
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1B1B1B)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(text = "📦", fontSize = 10.sp)
                    Text(
                        text = "${item.type}  ${item.time} ${item.source}",
                        fontSize = 12.sp,
                        color = Color(0xFF888888)
                    )
                }
            }

            // Amount
            Text(
                text = item.amount,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = if (item.isPositive) Color(0xFF2E7D32) else Color(0xFFE53935)
            )
        }
    }
}

// ── Preview ───────────────────────────────────────────────────────────────────

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun MovementsScreenPreview() {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    MovementsScreen(
        navController = rememberNavController(),
        drawerState = drawerState,
        scope = scope
    )
}
