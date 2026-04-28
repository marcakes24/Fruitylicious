package com.example.fruitylicious

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.navigation.NavController
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// --- Final Color Calibration ---
val GreenDark = Color(0xFF2E7D32)
val CreamYellow = Color(0xFFFDEB95)
val InputGray = Color(0xFFF1F1F1)
val HintGray = Color(0xFFBDBDBD)
val FooterGray = Color(0xFF757575)
val BrownText = Color(0xFF5D4037)
val GoldBorder = Color(0xFFFFD54F)

// --- User Data Model ---
data class User(
    val username: String,
    val password: String,
    val role: String // "admin" or "staff"
)

// --- Authentication Repository ---
object AuthRepository {
    private val users = listOf(
        User("admin", "admin", "admin"),
        User("user", "123", "staff")
    )

    fun authenticate(username: String, password: String): User? {
        return users.find { it.username == username && it.password == password }
    }
}

@Composable
fun FruityliciousLoginScreen(navController: NavController) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CreamYellow)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // --- Green Header (Background for the overlap) ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(GreenDark)
                    .padding(top = 50.dp, bottom = 120.dp), // Large bottom padding allows the card to "sit" on top
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = "Logo",
                    modifier = Modifier
                        .width(230.dp)
                        .height(110.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Welcome Back",
                    color = Color.White,
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Manage Fruitylicious Now!",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 15.sp
                )
            }

            // --- The White Card (Identical Spacing) ---
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 25.dp)
                    .offset(y = (-80).dp), // High negative offset to match your reference exactly
                shape = RoundedCornerShape(35.dp), // Deeply rounded corners
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 15.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 30.dp)
                ) {
                    Text("Username", color = BrownText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    TextField(
                        value = username,
                        onValueChange = { username = it },
                        placeholder = { Text("Enter your username here", color = HintGray, fontSize = 14.sp) },
                        modifier = Modifier.fillMaxWidth().height(55.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = TextFieldDefaults.colors(
                            unfocusedContainerColor = InputGray,
                            focusedContainerColor = InputGray,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Text("Password", color = BrownText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    TextField(
                        value = password,
                        onValueChange = { password = it },
                        placeholder = { Text("Enter your password here", color = HintGray, fontSize = 14.sp) },
                        modifier = Modifier.fillMaxWidth().height(55.dp),
                        shape = RoundedCornerShape(12.dp),
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(imageVector = image, contentDescription = null, tint = HintGray)
                            }
                        },
                        colors = TextFieldDefaults.colors(
                            unfocusedContainerColor = InputGray,
                            focusedContainerColor = InputGray,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(30.dp))

                    Button(
                        onClick = {
                            val user = AuthRepository.authenticate(username, password)
                            if (user != null) {
                                // Navigate based on role
                                val route = if (user.role == "admin") "admin_home" else "home"
                                navController.navigate(route) {
                                    popUpTo("login") { inclusive = true }
                                }
                                errorMessage = ""
                            } else {
                                errorMessage = "Invalid username or password"
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(55.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GreenDark)
                    ) {
                        Text("Log In", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }

                    // Error message display
                    if (errorMessage.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = errorMessage,
                            color = Color.Red,
                            fontSize = 12.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // "or" line
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f), color = InputGray)
                        Text(" or ", color = HintGray, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 10.dp))
                        HorizontalDivider(modifier = Modifier.weight(1f), color = InputGray)
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Continue as Guest button
                    OutlinedButton(
                        onClick = { navController.navigate("guests_screen") },
                        modifier = Modifier.fillMaxWidth().height(55.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, GoldBorder),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = BrownText)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AccountCircle, contentDescription = null, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Continue as Guest", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        }
                    }
                }
            }

            // --- Bottom Spacer & Footer ---
            Text(
                text = "All rights reserved 2026.",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 40.dp)
                    .offset(y = (-40).dp), // Adjust footer position relative to card offset
                color = FooterGray,
                fontSize = 12.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
