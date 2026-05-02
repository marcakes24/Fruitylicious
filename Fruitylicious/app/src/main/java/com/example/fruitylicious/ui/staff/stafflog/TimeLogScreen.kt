package com.example.fruitylicious.ui.shared

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
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
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.fruitylicious.util.ImageStorage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

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
    onMenuClick: () -> Unit = {},
    viewModel: TimeLogViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri == null) {
            viewModel.setError("No image selected.")
            return@rememberLauncherForActivityResult
        }

        try {
            val imagePath = ImageStorage.saveImageFromUri(
                context = context,
                sourceUri = uri,
                folder = "staff_logs"
            )

            viewModel.clockInWithImage(
                imagePath = imagePath
            )
        } catch (exception: Exception) {
            viewModel.setError(
                exception.message ?: "Failed to save selected image."
            )
        }
    }

    TimeLogContent(
        uiState = uiState,
        onMenuClick = onMenuClick,
        onClockActionClick = {
            when (uiState.attendanceState) {
                AttendanceState.CLOCKED_OUT -> {
                    imagePicker.launch("image/*")
                }

                AttendanceState.CLOCKED_IN -> {
                    viewModel.clockOut()
                }
            }
        }
    )
}

@Composable
private fun TimeLogContent(
    uiState: TimeLogUiState,
    onMenuClick: () -> Unit,
    onClockActionClick: () -> Unit
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
                            imagePath = uiState.activeImagePath
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
    imagePath: String?
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
            modifier = Modifier.size(88.dp),
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