package com.example.fruitylicious.ui.shared

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.fruitylicious.ui.shared.OwnerSideBarContent
import com.example.fruitylicious.ui.shared.SharedDrawerContent
import com.example.fruitylicious.ui.shared.SharedScreenMode
import com.example.fruitylicious.util.ImageStorage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val HeaderGreen = Color(0xFF2C8C44)
private val ScreenBackground = Color(0xFFFFEAA0)
private val CardBackground = Color.White
private val TimeTextColor = Color(0xFF1A1A1A)
private val SubtleText = Color(0xFF757575)
private val LightGrayBox = Color(0xFFF5F5F5)
private val OnlineGreen = Color(0xFF22C55E)
private val StatusGreenBox = Color(0xFFE8F5E9)
private val DarkButton = Color(0xFF333333)

@Composable
fun TimeLogScreen(
    navController: NavController,
    userName: String = "User",
    branchName: String = "",
    onLogout: () -> Unit = {},
    viewModel: TimeLogViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var showCamera by remember { mutableStateOf(false) }
    var showLogoutAlert by remember { mutableStateOf(false) }
    var showClockOutConfirm by remember { mutableStateOf(false) }
    var showExpandedImage by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            showCamera = true
        } else {
            viewModel.setError("Camera permission is required to clock in.")
        }
    }

    if (showCamera) {
        CameraCaptureDialog(
            onDismiss = { showCamera = false },
            onCaptured = { bitmap ->
                showCamera = false
                val timestampedBitmap = addTimestampToBitmap(bitmap)
                val path = ImageStorage.saveBitmap(
                    context = context,
                    bitmap = timestampedBitmap,
                    folder = "staff_logs"
                )
                viewModel.clockInWithImage(path)
            }
        )
    }

    if (showLogoutAlert) {
        AlertDialog(
            onDismissRequest = { showLogoutAlert = false },
            title = { Text("Cannot Log Out") },
            text = { Text("You are currently clocked in. Please clock out before logging out of the application.") },
            confirmButton = {
                Button(onClick = { showLogoutAlert = false }) {
                    Text("OK")
                }
            }
        )
    }

    if (showClockOutConfirm) {
        AlertDialog(
            onDismissRequest = { showClockOutConfirm = false },
            title = { Text("Confirm Clock Out") },
            text = { Text("Are you sure you want to clock out?") },
            confirmButton = {
                Button(
                    onClick = {
                        showClockOutConfirm = false
                        viewModel.clockOut()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkButton)
                ) {
                    Text("Clock Out")
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showClockOutConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showExpandedImage && uiState.activeImagePath != null) {
        ExpandedImageDialog(
            imagePath = uiState.activeImagePath!!,
            onDismiss = { showExpandedImage = false }
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color.Transparent,
                drawerTonalElevation = 0.dp
            ) {
                SharedDrawerContent(
                    mode = SharedScreenMode.STAFF,
                    navController = navController,
                    drawerState = drawerState,
                    scope = scope,
                    userName = userName,
                    branchName = branchName,
                    onLogout = {
                        if (uiState.attendanceState == AttendanceState.CLOCKED_IN) {
                            showLogoutAlert = true
                        } else {
                            onLogout()
                        }
                    }
                )
            }
        }
    ) {
        TimeLogContent(
            uiState = uiState,
            onMenuClick = {
                scope.launch {
                    drawerState.open()
                }
            },
            onClockActionClick = {
                when (uiState.attendanceState) {
                    AttendanceState.CLOCKED_OUT -> {
                        val permission = Manifest.permission.CAMERA
                        if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
                            showCamera = true
                        } else {
                            permissionLauncher.launch(permission)
                        }
                    }

                    AttendanceState.CLOCKED_IN -> {
                        showClockOutConfirm = true
                    }
                }
            },
            onImageClick = {
                showExpandedImage = true
            }
        )
    }
}

private fun addTimestampToBitmap(bitmap: Bitmap): Bitmap {
    val result = bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(result)
    val paint = Paint().apply {
        color = android.graphics.Color.WHITE
        textSize = bitmap.height / 15f
        isAntiAlias = true
        style = Paint.Style.FILL
        setShadowLayer(8f, 3f, 3f, android.graphics.Color.BLACK)
    }

    val timeFormatter = SimpleDateFormat("hh:mm:ss a", Locale.getDefault())
    val timestamp = timeFormatter.format(Date())

    val x = 40f
    val y = bitmap.height - 60f
    canvas.drawText(timestamp, x, y, paint)

    return result
}

@Composable
fun CameraCaptureDialog(
    onDismiss: () -> Unit,
    onCaptured: (Bitmap) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val imageCapture = remember { ImageCapture.Builder().build() }

    val nowMillis by produceState(initialValue = System.currentTimeMillis()) {
        while (true) {
            value = System.currentTimeMillis()
            delay(1000)
        }
    }
    val timeFormatter = remember { SimpleDateFormat("hh:mm:ss a", Locale.getDefault()) }
    val timeText = remember(nowMillis) { timeFormatter.format(Date(nowMillis)) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val executor = ContextCompat.getMainExecutor(ctx)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }
                        val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                imageCapture
                            )
                        } catch (e: Exception) {
                            // handle binding error
                        }
                    }, executor)
                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )

            // Real-time Clock Overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 60.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    color = Color.Black.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = timeText,
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }

            // Capture Controls
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 50.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable {
                            imageCapture.takePicture(
                                ContextCompat.getMainExecutor(context),
                                object : ImageCapture.OnImageCapturedCallback() {
                                    override fun onCaptureSuccess(image: ImageProxy) {
                                        val bitmap = image.toBitmap()
                                        onCaptured(bitmap)
                                        image.close()
                                    }

                                    override fun onError(exception: ImageCaptureException) {
                                        // handle capture error
                                    }
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .clip(CircleShape)
                            .border(2.dp, Color.Black, CircleShape)
                    )
                }
            }

            // Close Button
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 48.dp, end = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

@Composable
fun ExpandedImageDialog(
    imagePath: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val imageFile = ImageStorage.getImageFile(context, imagePath)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.9f))
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = imageFile,
                contentDescription = "Expanded clock-in image",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentScale = ContentScale.Fit
            )

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 48.dp, end = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

@Composable
private fun TimeLogContent(
    uiState: TimeLogUiState,
    onMenuClick: () -> Unit,
    onClockActionClick: () -> Unit,
    onImageClick: () -> Unit
) {
    val nowMillis by produceState(initialValue = System.currentTimeMillis()) {
        while (true) {
            value = System.currentTimeMillis()
            delay(1000)
        }
    }

    val dateFormatter = remember {
        SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
    }

    val timeFormatter = remember {
        SimpleDateFormat("hh:mm:ss a", Locale.getDefault())
    }

    val dateText = remember(nowMillis) {
        dateFormatter.format(Date(nowMillis))
    }

    val timeText = remember(nowMillis) {
        timeFormatter.format(Date(nowMillis))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBackground)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            TimeLogHeader(
                onMenuClick = onMenuClick
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = CardBackground
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 2.dp
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 26.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = dateText,
                            color = SubtleText,
                            fontSize = 16.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = timeText,
                            color = TimeTextColor,
                            fontSize = 34.sp,
                            fontWeight = FontWeight.ExtraBold
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        AvatarWithStatus(
                            imagePath = uiState.activeImagePath,
                            onImageClick = onImageClick
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = uiState.fullName.ifBlank { "Unknown User" },
                            color = TimeTextColor,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = uiState.username.ifBlank { "@unknown" },
                            color = SubtleText,
                            fontSize = 15.sp
                        )

                        Spacer(modifier = Modifier.height(28.dp))

                        AttendanceStatusBox(
                            state = uiState.attendanceState,
                            clockedInSince = uiState.clockedInSince
                        )

                        if (!uiState.error.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = uiState.error ?: "",
                                color = Color.Red,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(modifier = Modifier.height(28.dp))

                        ClockActionButton(
                            state = uiState.attendanceState,
                            enabled = !uiState.isLoading,
                            onClick = onClockActionClick
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TimeLogHeader(
    onMenuClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(HeaderGreen)
            .padding(start = 16.dp, end = 16.dp, top = 48.dp, bottom = 14.dp)
    ) {
        Row(
            modifier = Modifier.align(Alignment.CenterStart),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HamburgerButton(
                onClick = onMenuClick
            )

            Text(
                text = "TIME LOG",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun HamburgerButton(
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clickable {
                onClick()
            }
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
}

@Composable
private fun AvatarWithStatus(
    imagePath: String?,
    onImageClick: () -> Unit
) {
    val context = LocalContext.current

    val imageFile = imagePath?.let {
        ImageStorage.getImageFile(
            context = context,
            relativePath = it
        )
    }

    Box(
        contentAlignment = Alignment.BottomEnd
    ) {
        Surface(
            modifier = Modifier
                .size(88.dp)
                .clickable(enabled = imagePath != null) { onImageClick() },
            shape = CircleShape,
            shadowElevation = 4.dp,
            color = Color(0xFF1D3022)
        ) {
            if (imageFile != null && imageFile.exists()) {
                AsyncImage(
                    model = imageFile,
                    contentDescription = "Clock-in image",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                ProfilePlaceholder()
            }
        }

        Box(
            modifier = Modifier
                .size(18.dp)
                .offset(x = (-2).dp, y = (-2).dp)
                .clip(CircleShape)
                .background(OnlineGreen)
        )
    }
}

@Composable
private fun ProfilePlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF334155)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = "Profile Placeholder",
            tint = Color.White,
            modifier = Modifier.size(44.dp)
        )
    }
}

@Composable
private fun AttendanceStatusBox(
    state: AttendanceState,
    clockedInSince: String
) {
    when (state) {
        AttendanceState.CLOCKED_OUT -> {
            StatusBoxClockedOut()
        }

        AttendanceState.CLOCKED_IN -> {
            StatusBoxClockedIn(
                since = clockedInSince
            )
        }
    }
}

@Composable
private fun StatusBoxClockedOut() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(LightGrayBox),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Not Clocked In",
            color = TimeTextColor,
            fontSize = 16.sp
        )
    }
}

@Composable
private fun StatusBoxClockedIn(
    since: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(StatusGreenBox)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Currently Clocked In",
                color = HeaderGreen,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Since $since",
                color = HeaderGreen,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun ClockActionButton(
    state: AttendanceState,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val buttonConfig = remember(state) {
        if (state == AttendanceState.CLOCKED_IN) {
            ClockButtonConfig(
                text = "Clock Out",
                color = DarkButton,
                icon = Icons.AutoMirrored.Filled.ExitToApp
            )
        } else {
            ClockButtonConfig(
                text = "Clock In",
                color = HeaderGreen,
                icon = Icons.Default.Login
            )
        }
    }

    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = buttonConfig.color
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 4.dp
        ),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = buttonConfig.icon,
                contentDescription = buttonConfig.text,
                tint = Color.White
            )

            Spacer(modifier = Modifier.size(8.dp))

            Text(
                text = buttonConfig.text,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        }
    }
}

private data class ClockButtonConfig(
    val text: String,
    val color: Color,
    val icon: ImageVector
)