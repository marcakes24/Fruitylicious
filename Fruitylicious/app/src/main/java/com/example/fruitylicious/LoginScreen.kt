package com.example.fruitylicious

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.navigation.NavController
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// --- Color Palette ---
val GreenDark = Color(0xFF2E7D32)
val CreamYellow = Color(0xFFF5E6A3)
val InputGray = Color(0xFFF0F0F0)
val TextDark = Color(0xFF333333)
val HintGray = Color(0xFFAAAAAA)
val FooterGray = Color(0xFF888888)

@Composable
fun FruityliciousLoginScreen(navController: NavController) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // Logic function to handle login attempts
    fun handleLogin() {
        if (username.isNotBlank() && password.isNotEmpty()) {
            if(username == "user" && password == "123") {
                navController.navigate("home")
            }
            else if(username == "admin" && password == "admin") {
                navController.navigate("admin_home")
            } else {
                // Here you would typically show a Snackbar or error text in UI
                println("Login failed: invalid credentials")
            }
        } else {
            println("Login failed: fields are empty")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CreamYellow)
            .verticalScroll(rememberScrollState())
    ) {

        // ── Green Header ──────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(GreenDark)
                .padding(top = 56.dp, bottom = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "Fruitylicious Logo",
                modifier = Modifier
                    .width(180.dp)
                    .height(90.dp)
                    .rotate(-10f)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Welcome Back",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Manage Fruitylicious Now!",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 14.sp
            )
        }

        // ── Cream Body ────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(CreamYellow)
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    // Username Field
                    Text(
                        text = "Username",
                        color = TextDark,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    TextField(
                        value = username, // Connect to state
                        onValueChange = { username = it }, // Update state on input
                        placeholder = {
                            Text(
                                text = "Enter your username here",
                                color = HintGray,
                                fontSize = 14.sp
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = TextFieldDefaults.colors(
                            unfocusedContainerColor = InputGray,
                            focusedContainerColor = InputGray,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedTextColor = TextDark,
                            focusedTextColor = TextDark
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Password Field
                    Text(
                        text = "Password",
                        color = TextDark,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    TextField(
                        value = password, // Connect to state
                        onValueChange = { password = it }, // Update state on input
                        placeholder = {
                            Text(
                                text = "Enter your password here",
                                color = HintGray,
                                fontSize = 14.sp
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(8.dp),
                        visualTransformation = PasswordVisualTransformation(),
                        colors = TextFieldDefaults.colors(
                            unfocusedContainerColor = InputGray,
                            focusedContainerColor = InputGray,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedTextColor = TextDark,
                            focusedTextColor = TextDark
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Log In Button
                    Button(
                        onClick = { handleLogin() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GreenDark)
                    ) {
                        Text(
                            text = "Log In",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Footer
            Text(
                text = "All rights reserved 2026.",
                color = FooterGray,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
