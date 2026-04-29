package com.example.fruitylicious.ui.shared

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.fruitylicious.R

@Composable
fun GuestsScreen(navController: NavController) {
    Box(modifier = Modifier.fillMaxSize()) {

        // ── Scrollable Content ──
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFFDEB95)),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Green Header with Logo ──
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF2E7D32))
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Image(
                            painter = painterResource(id = R.drawable.logo),
                            contentDescription = "Fruitylicious Logo",
                            modifier = Modifier
                                .width(230.dp)
                                .height(110.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Fruitylicious",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp
                        )
                        Text(
                            "FRUITS AND SHAKES STATION",
                            color = Color(0xFFB9F6CA),
                            fontSize = 10.sp,
                            letterSpacing = 1.5.sp
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }

            // ── Our Mission ──
            item {
                InfoCard(
                    title = "Our Mission",
                    icon = Icons.Default.Public,
                    iconTint = Color(0xFF2E7D32),
                    content = "To serve refreshing, delicious, and nutritious fruit shakes made from high-quality, natural ingredients, promoting healthier lifestyles while delivering excellent customer satisfaction in every cup."
                )
            }

            item { Spacer(Modifier.height(12.dp)) }

            // ── Our Vision ──
            item {
                InfoCard(
                    title = "Our Vision",
                    icon = Icons.Default.Star,
                    iconTint = Color(0xFFFBC02D),
                    content = "To become a trusted and leading fruit shake brand known for inspiring healthier communities by making nutritious drinks accessible, enjoyable, and part of everyday life."
                )
            }

            item { Spacer(Modifier.height(12.dp)) }

            // ── Core Values ──
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(15.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Favorite, null, tint = Color(0xFFE57373))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Core Values",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32),
                                fontSize = 18.sp
                            )
                        }
                        Spacer(Modifier.height(12.dp))

                        val coreValues = listOf(
                            "Freshness" to "We use only the freshest fruits and ingredients",
                            "Quality" to "Every shake is crafted with care and consistency",
                            "Affordability" to "Great taste at prices everyone can enjoy",
                            "Customer First" to "Your satisfaction is our top priority"
                        )
                        coreValues.forEach { (title, desc) ->
                            Row(
                                modifier = Modifier.padding(vertical = 4.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text("• ", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                                Column {
                                    Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(desc, fontSize = 12.sp, color = Color.Gray)
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(12.dp)) }

            // ── Our Menu ──
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(15.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.RestaurantMenu, null, tint = Color(0xFF2E7D32))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Our Menu",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32),
                                fontSize = 18.sp
                            )
                        }
                        Spacer(Modifier.height(12.dp))

                        val menuItems = listOf(
                            "Avocado", "Mango",
                            "Dragon Fruit", "Banana",
                            "Guyabano", "Buko",
                            "Apple", "Strawberry",
                            "Melon", "Cheesecake",
                            "Oreo"
                        )
                        menuItems.chunked(2).forEach { row ->
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                row.forEach { item ->
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(vertical = 4.dp),
                                        color = Color(0xFFFFFDE7),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            item,
                                            Modifier
                                                .padding(vertical = 8.dp)
                                                .fillMaxWidth(),
                                            textAlign = TextAlign.Center,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                                if (row.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }

                        HorizontalDivider(Modifier.padding(vertical = 12.dp))

                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Medium (16 oz)", fontSize = 14.sp)
                            Text("₱60", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                        }
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Large (22 oz)", fontSize = 14.sp)
                            Text("₱80", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Mix Flavor (2 fruits)  +₱15",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(12.dp)) }

            // ── Our Branches ──
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(15.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, null, tint = Color(0xFF2E7D32))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Our Branches",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32),
                                fontSize = 18.sp
                            )
                        }
                        Spacer(Modifier.height(12.dp))

                        BranchItem(
                            name = "Branch 1",
                            address = "South Gate, Barangay Pansol, Quezon City",
                            hours = "Open: 11:00 AM – 8:00 PM"
                        )
                        Spacer(Modifier.height(10.dp))
                        BranchItem(
                            name = "Branch 2",
                            address = "Kaingin 1, Block 3, Barangay Pansol, Quezon City",
                            hours = "Open: 11:00 AM – 8:00 PM"
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(12.dp)) }

            // ── Contact ──
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(15.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Phone, null, tint = Color(0xFF2E7D32))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Contact Us – 09088668197",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color(0xFF2E7D32)
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Follow us on Facebook @fruitylicious",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
            }

            // ── Footer ──
            item {
                Text(
                    "All rights reserved 2025.",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            }
        } // closes LazyColumn

        // ── Back Button (overlays top-left of green header) ──
        IconButton(
            onClick = { navController.popBackStack() },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
                .statusBarsPadding()
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White
            )
        }

    } // closes Box
}

// ── Reusable InfoCard ──
@Composable
fun InfoCard(
    title: String,
    icon: ImageVector,
    iconTint: Color = Color(0xFF2E7D32),
    content: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(15.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = iconTint)
                Spacer(Modifier.width(8.dp))
                Text(title, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32), fontSize = 18.sp)
            }
            Text(
                content,
                fontSize = 13.sp,
                color = Color.DarkGray,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

// ── Branch Item ──
@Composable
fun BranchItem(name: String, address: String, hours: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            Icons.Default.LocationOn,
            null,
            tint = Color(0xFFFF7043),
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(6.dp))
        Column {
            Text(name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(address, fontSize = 12.sp, color = Color.Gray)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.AccessTime,
                    null,
                    tint = Color.Gray,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(hours, fontSize = 11.sp, color = Color.Gray)
            }
        }
    }
}