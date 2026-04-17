package com.example.fruitylicious

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun NotificationsScreen(
    drawerState: DrawerState,
    scope: CoroutineScope
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFF9C4)) // Figma Cream Background
            .verticalScroll(rememberScrollState())
    ) {

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF2E7D32)) // Figma Green
                .padding(top = 48.dp, bottom = 20.dp, start = 20.dp, end = 20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    text = "☰",
                    color = Color.White,
                    fontSize = 24.sp,
                    modifier = Modifier.clickable {
                        scope.launch { drawerState.open() }
                    }
                )

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = "NOTIFICATION",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.weight(1.2f))
            }
        }


        Column(modifier = Modifier.padding(16.dp)) {

            NotificationItem(
                name = "Mango",
                sku = "SKU-0041 - Mangoes",
                current = "7 kg",
                min = "50 kg",
                status = "Critical",
                emoji = "🥭"
            )

            Spacer(modifier = Modifier.height(16.dp))

            NotificationItem(
                name = "Watermelon",
                sku = "SKU-0018 - Melons",
                current = "12 kg",
                min = "40 kg",
                status = "Critical",
                emoji = "🍉"
            )

            Spacer(modifier = Modifier.height(16.dp))

            NotificationItem(
                name = "Avocado",
                sku = "SKU-0012 - Avocados",
                current = "15 kg",
                min = "N/A",
                status = "Warning",
                emoji = "🥑"
            )
        }
    }
}

@Composable
fun NotificationItem(
    name: String,
    sku: String,
    current: String,
    min: String,
    status: String,
    emoji: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {

                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = emoji, fontSize = 30.sp)
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(text = name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(text = sku, color = Color.Gray, fontSize = 12.sp)
                }


                val badgeColor = if (status == "Critical") Color(0xFFFFEBEE) else Color(0xFFFFF9C4)
                val textColor = if (status == "Critical") Color.Red else Color(0xFFFBC02D)

                Surface(color = badgeColor, shape = RoundedCornerShape(12.dp)) {
                    Text(
                        text = status,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = textColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "Current: $current", color = Color(0xFF795548), fontWeight = FontWeight.Bold)
                Text(text = "Min: $min", color = Color(0xFF795548))
            }


            LinearProgressIndicator(
                progress = { 0.2f }, // Use curly braces for the progress lambda
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                color = if (status == "Critical") Color.Red else Color(0xFFFBC02D),
                trackColor = Color.LightGray
            )

            Button(
                onClick = { /* Navigate to detailed view */ },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(text = "View Details →", color = Color.White)
            }
        }
    }
}