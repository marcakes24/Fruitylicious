package com.example.fruitylicious.ui.admin.settings

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.fruitylicious.ui.shared.OwnerSideBarContent
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.InputStream

private val MpGreen = Color(0xFF2C8C44)
private val MpPageBg = Color(0xFFFFEAA0)
private val MpCardBg = Color.White
private val MpTextMain = Color(0xFF1A1A1A)
private val MpTextSub = Color(0xFF757575)
private val FieldBg = Color(0xFFF5F5F5)

@Composable
fun BranchSettingsScreen(
    navController: NavController,
    adminName: String = "Admin User",
    onLogout: () -> Unit = {},
    viewModel: BranchSettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val (base64, mimeType) = processImageUri(context, it)
            if (base64 != null && mimeType != null) {
                viewModel.onQrImageSelected(base64, mimeType)
            }
        }
    }

    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            val message = if (uiState.requiresRestart) {
                "Configuration saved. Please restart the app or re-login to apply the new backend URL."
            } else {
                "Branch configuration saved."
            }
            snackbarHostState.showSnackbar(message)
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(uiState.testConnectionSuccess) {
        uiState.testConnectionSuccess?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(uiState.testConnectionError) {
        uiState.testConnectionError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color.Transparent,
                drawerTonalElevation = 0.dp
            ) {
                OwnerSideBarContent(
                    navController = navController,
                    drawerState = drawerState,
                    scope = scope,
                    ownerName = adminName,
                    onLogout = onLogout
                )
            }
        }
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                BranchSettingsHeader(
                    onMenuClick = {
                        scope.launch {
                            drawerState.open()
                        }
                    }
                )
            },
            containerColor = MpPageBg
        ) { paddingValues ->
            Box(modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)) {
                if (uiState.isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MpGreen)
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp)
                            .verticalScroll(scrollState),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Spacer(modifier = Modifier.height(10.dp))

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MpCardBg,
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(18.dp)
                            ) {
                                Text(
                                    text = "Branch Information",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MpGreen
                                )

                                SettingsField(
                                    label = "Branch Name",
                                    value = uiState.branchName,
                                    onValueChange = viewModel::onBranchNameChange,
                                    placeholder = "Enter branch name"
                                )

                                SettingsField(
                                    label = "Address",
                                    value = uiState.address,
                                    onValueChange = viewModel::onAddressChange,
                                    placeholder = "Enter branch address",
                                    singleLine = false,
                                    minLines = 2
                                )

                                SettingsField(
                                    label = "Contact Number",
                                    value = uiState.contactNumber,
                                    onValueChange = viewModel::onContactNumberChange,
                                    placeholder = "Enter contact number"
                                )

                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = Color(0xFFEEEEEE))

                                Text(
                                    text = "GCash Payment Details",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MpGreen
                                )

                                SettingsField(
                                    label = "GCash Account Name",
                                    value = uiState.gcashAccountName,
                                    onValueChange = viewModel::onGcashAccountNameChange,
                                    placeholder = "Enter account name"
                                )

                                SettingsField(
                                    label = "GCash Account Number",
                                    value = uiState.gcashAccountNumber,
                                    onValueChange = viewModel::onGcashAccountNumberChange,
                                    placeholder = "Enter account number"
                                )

                                Column {
                                    Text(
                                        text = "GCash QR Code",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MpTextMain,
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    )

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(240.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(FieldBg)
                                            .border(1.dp, Color(0xFFDDDDDD), RoundedCornerShape(12.dp))
                                            .clickable { imagePickerLauncher.launch("image/*") },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (uiState.gcashQrImage != null) {
                                            val bitmap = remember(uiState.gcashQrImage) {
                                                decodeBase64ToBitmap(uiState.gcashQrImage!!)
                                            }
                                            if (bitmap != null) {
                                                Box(modifier = Modifier.fillMaxSize()) {
                                                    Image(
                                                        bitmap = bitmap.asImageBitmap(),
                                                        contentDescription = "GCash QR Code",
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .padding(12.dp),
                                                        contentScale = ContentScale.Fit
                                                    )
                                                    
                                                    IconButton(
                                                        onClick = viewModel::onRemoveQrImage,
                                                        modifier = Modifier
                                                            .align(Alignment.TopEnd)
                                                            .padding(8.dp)
                                                            .background(Color.White.copy(alpha = 0.8f), RoundedCornerShape(20.dp))
                                                    ) {
                                                        Icon(Icons.Default.Delete, contentDescription = "Remove QR", tint = Color.Red)
                                                    }
                                                }
                                            }
                                        } else {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Icon(
                                                    imageVector = Icons.Default.CloudUpload,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(48.dp),
                                                    tint = Color.Gray
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text("Tap to upload QR image", color = MpTextSub, fontSize = 14.sp)
                                            }
                                        }
                                    }
                                }

                                if (uiState.error != null) {
                                    Text(
                                        text = uiState.error!!,
                                        color = Color.Red,
                                        fontSize = 14.sp,
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Center
                                    )
                                }

                                if (uiState.hasChanges || uiState.isSaving) {
                                    Button(
                                        onClick = viewModel::saveSettings,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(56.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MpGreen),
                                        shape = RoundedCornerShape(12.dp),
                                        enabled = !uiState.isSaving
                                    ) {
                                        if (uiState.isSaving) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(24.dp),
                                                color = Color.White
                                            )
                                        } else {
                                            Text("SAVE BRANCH DETAILS", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        }
                                    }
                                }
                            }
                        }

                        // ADMIN ONLY BRANCH CONFIGURATION SECTION
                        if (uiState.isAdmin) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = MpCardBg,
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(18.dp)
                                ) {
                                    Text(
                                        text = "Branch Configuration (Admin)",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MpGreen
                                    )

                                    SettingsField(
                                        label = "Branch ID",
                                        value = uiState.configBranchIdText,
                                        onValueChange = viewModel::onConfigBranchIdChanged,
                                        placeholder = "e.g. 1",
                                        enabled = false,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                    )

                                    SettingsField(
                                        label = "Branch Name (Config)",
                                        value = uiState.configBranchName,
                                        onValueChange = viewModel::onConfigBranchNameChanged,
                                        placeholder = "e.g. Branch 1",
                                        enabled = false
                                    )

                                    SettingsField(
                                        label = "API Base URL",
                                        value = uiState.configApiBaseUrl,
                                        onValueChange = viewModel::onConfigApiBaseUrlChanged,
                                        placeholder = "http://192.168.1.100:8083/"
                                    )

                                    SettingsField(
                                        label = "API Key",
                                        value = uiState.configApiKey,
                                        onValueChange = viewModel::onConfigApiKeyChanged,
                                        placeholder = "Enter API Key",
                                        isPassword = true,
                                        isPasswordVisible = uiState.isApiKeyVisible,
                                        onTogglePasswordVisibility = viewModel::toggleApiKeyVisibility
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = viewModel::testConnection,
                                            modifier = Modifier.weight(1f).height(50.dp),
                                            shape = RoundedCornerShape(12.dp),
                                            enabled = !uiState.isTestingConnection,
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MpGreen)
                                        ) {
                                            if (uiState.isTestingConnection) {
                                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MpGreen, strokeWidth = 2.dp)
                                            } else {
                                                Text(
                                                    text = "TEST CONNECTION",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                        }

                                        Button(
                                            onClick = viewModel::saveConfig,
                                            modifier = Modifier.weight(1f).height(50.dp),
                                            shape = RoundedCornerShape(12.dp),
                                            enabled = !uiState.isSaving,
                                            colors = ButtonDefaults.buttonColors(containerColor = MpGreen)
                                        ) {
                                            Text(
                                                text = "SAVE CONFIG",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }

                                    TextButton(
                                        onClick = viewModel::resetConfigToDefault,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Reset to Default", color = Color.Gray, fontSize = 14.sp)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(30.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun BranchSettingsHeader(
    onMenuClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MpGreen)
            .padding(start = 16.dp, end = 16.dp, top = 48.dp, bottom = 14.dp)
    ) {
        Row(
            modifier = Modifier.align(Alignment.CenterStart),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .clickable { onMenuClick() }
                    .padding(end = 12.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                repeat(3) {
                    Box(
                        modifier = Modifier
                            .width(22.dp)
                            .height(2.5.dp)
                            .background(Color.White, RoundedCornerShape(2.dp))
                    )
                }
            }

            Text(
                text = "BRANCH SETTINGS",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
        
        Icon(
            imageVector = Icons.Default.Settings,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.5f),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(24.dp)
        )
    }
}

@Composable
private fun SettingsField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    minLines: Int = 1,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    isPassword: Boolean = false,
    isPasswordVisible: Boolean = false,
    onTogglePasswordVisibility: () -> Unit = {}
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (enabled) MpTextMain else MpTextSub,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            readOnly = !enabled,
            textStyle = TextStyle(
                fontSize = 14.sp, 
                color = if (enabled) MpTextMain else MpTextSub
            ),
            keyboardOptions = keyboardOptions,
            visualTransformation = if (isPassword && !isPasswordVisible) PasswordVisualTransformation() else VisualTransformation.None,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(if (enabled) FieldBg else FieldBg.copy(alpha = 0.5f))
                .border(
                    1.dp, 
                    if (enabled) Color(0xFFEEEEEE) else Color(0xFFDDDDDD), 
                    RoundedCornerShape(10.dp)
                )
                .padding(horizontal = 16.dp, vertical = 14.dp),
            decorationBox = { inner ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.weight(1f)) {
                        if (value.isEmpty()) {
                            Text(
                                text = placeholder,
                                color = Color.Gray,
                                fontSize = 14.sp
                            )
                        }
                        inner()
                    }
                    if (isPassword) {
                        Icon(
                            imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier
                                .size(20.dp)
                                .clickable { onTogglePasswordVisibility() }
                        )
                    }
                }
            },
            singleLine = singleLine,
            minLines = minLines
        )
    }
}

fun decodeBase64ToBitmap(base64Str: String): Bitmap? {
    return try {
        val imageBytes = Base64.decode(base64Str, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    } catch (e: Exception) {
        null
    }
}

fun processImageUri(context: Context, uri: Uri): Pair<String?, String?> {
    return try {
        val mimeType = context.contentResolver.getType(uri)
        val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
        val bytes = inputStream?.readBytes()
        inputStream?.close()

        if (bytes == null) return Pair(null, null)

        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        val outputStream = ByteArrayOutputStream()
        
        val maxWidth = 1024
        val maxHeight = 1024
        val width = bitmap.width
        val height = bitmap.height
        
        val resizedBitmap = if (width > maxWidth || height > maxHeight) {
            val scale = Math.min(maxWidth.toFloat() / width, maxHeight.toFloat() / height)
            Bitmap.createScaledBitmap(bitmap, (width * scale).toInt(), (height * scale).toInt(), true)
        } else {
            bitmap
        }

        val finalMimeType = if (mimeType == "image/jpeg" || mimeType == "image/jpg") {
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            mimeType
        } else {
            resizedBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            "image/png"
        }
        
        val base64 = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
        Pair(base64, finalMimeType)
    } catch (e: Exception) {
        Pair(null, null)
    }
}
