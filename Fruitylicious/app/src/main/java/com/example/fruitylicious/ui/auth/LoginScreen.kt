package com.example.fruitylicious.ui.auth

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.fruitylicious.R

private val GreenDark = Color(0xFF2E7D32)
private val CreamYellow = Color(0xFFFDEB95)
private val InputGray = Color(0xFFF1F1F1)
private val HintGray = Color(0xFFBDBDBD)
private val FooterGray = Color(0xFF757575)
private val BrownText = Color(0xFF5D4037)

@Composable
fun LoginScreen(
    onLoginSuccess: (String) -> Unit,
    onGuestClick: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var passwordVisible by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.loggedInRole) {
        val role = uiState.loggedInRole
        if (!role.isNullOrBlank()) {
            onLoginSuccess(role)
            viewModel.consumeLoginNavigation()
        }
    }

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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(GreenDark)
                    .padding(top = 50.dp, bottom = 120.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = "Fruitylicious Logo",
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
                    text = uiState.branchName.ifBlank { "Manage Fruitylicious Now!" },
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 15.sp
                )
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 25.dp)
                    .offset(y = (-80).dp),
                shape = RoundedCornerShape(35.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 15.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 30.dp)
                ) {
                    Text(
                        text = "Username",
                        color = BrownText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    TextField(
                        value = uiState.username,
                        onValueChange = viewModel::onUsernameChanged,
                        placeholder = {
                            Text(
                                text = "Enter your username here",
                                color = HintGray,
                                fontSize = 14.sp
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(55.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = TextFieldDefaults.colors(
                            unfocusedContainerColor = InputGray,
                            focusedContainerColor = InputGray,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            errorContainerColor = InputGray,
                            errorIndicatorColor = Color.Transparent
                        ),
                        singleLine = true,
                        isError = uiState.usernameError != null
                    )

                    val usernameError = uiState.usernameError
                    if (!usernameError.isNullOrBlank()) {
                        Text(
                            text = usernameError,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "Password",
                        color = BrownText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    TextField(
                        value = uiState.password,
                        onValueChange = viewModel::onPasswordChanged,
                        placeholder = {
                            Text(
                                text = "Enter your password here",
                                color = HintGray,
                                fontSize = 14.sp
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(55.dp),
                        shape = RoundedCornerShape(12.dp),
                        visualTransformation = if (passwordVisible) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        trailingIcon = {
                            val icon = if (passwordVisible) {
                                Icons.Filled.Visibility
                            } else {
                                Icons.Filled.VisibilityOff
                            }

                            IconButton(
                                onClick = { passwordVisible = !passwordVisible }
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = "Toggle password visibility",
                                    tint = HintGray
                                )
                            }
                        },
                        colors = TextFieldDefaults.colors(
                            unfocusedContainerColor = InputGray,
                            focusedContainerColor = InputGray,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            errorContainerColor = InputGray,
                            errorIndicatorColor = Color.Transparent
                        ),
                        singleLine = true,
                        isError = uiState.passwordError != null
                    )

                    val passwordError = uiState.passwordError
                    if (!passwordError.isNullOrBlank()) {
                        Text(
                            text = passwordError,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(30.dp))

                    Button(
                        onClick = viewModel::login,
                        enabled = !uiState.isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(55.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GreenDark)
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(22.dp)
                            )
                        } else {
                            Text(
                                text = "Log In",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            thickness = 1.dp,
                            color = Color.LightGray.copy(alpha = 0.5f)
                        )
                        Text(
                            text = "or",
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            thickness = 1.dp,
                            color = Color.LightGray.copy(alpha = 0.5f)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    OutlinedButton(
                        onClick = onGuestClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(55.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.5.dp, Color(0xFFFDEB95)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = BrownText)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Continue as Guest",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    val errorMessage = uiState.error
                    if (!errorMessage.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Text(
                text = "All rights reserved 2026.",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 40.dp)
                    .offset(y = (-40).dp),
                color = FooterGray,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}