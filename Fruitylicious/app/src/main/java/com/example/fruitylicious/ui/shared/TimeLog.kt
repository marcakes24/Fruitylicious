package com.example.fruitylicious.ui.shared

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
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Login
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fruitylicious.R
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class AttendanceState {
    CLOCKED_OUT,
    CLOCKED_IN
}

data class TimeLogUiState(
    val fullName: String = "Eula Valdez",
    val username: String = "@staff",
    val attendanceState: AttendanceState = AttendanceState.CLOCKED_OUT,
    val clockedInSince: String = "10:34 AM",
    @DrawableRes val profileRes: Int? = R.drawable.eula
)

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
    uiState: TimeLogUiState,
    onMenuClick: () -> Unit = {},
    onClockActionClick: () -> Unit = {}
) {
    val nowMillis by produceState(initialValue = System.currentTimeMillis()) {
        while (true) {
            value = System.currentTimeMillis()
            delay(1000)
        }
    }

    val dateText = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date(nowMillis))
    val timeText = SimpleDateFormat("hh:mm:ss a", Locale.getDefault()).format(Date(nowMillis))

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
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
                    // Hamburger
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
                        text = "TIME LOG",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Content area
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
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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

                        AvatarWithStatus(profileRes = uiState.profileRes)

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = uiState.fullName,
                            color = TimeTextColor,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = uiState.username,
                            color = SubtleText,
                            fontSize = 15.sp
                        )

                        Spacer(modifier = Modifier.height(28.dp))

                        if (uiState.attendanceState == AttendanceState.CLOCKED_OUT) {
                            StatusBoxClockedOut()
                        } else {
                            StatusBoxClockedIn(uiState.clockedInSince)
                        }

                        Spacer(modifier = Modifier.height(28.dp))

                        ClockActionButton(
                            state = uiState.attendanceState,
                            onClick = onClockActionClick
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AvatarWithStatus(@DrawableRes profileRes: Int?) {
    Box(
        contentAlignment = Alignment.BottomEnd
    ) {
        Surface(
            modifier = Modifier.size(88.dp),
            shape = CircleShape,
            shadowElevation = 4.dp,
            color = Color(0xFF1D3022)
        ) {
            if (profileRes != null) {
                Image(
                    painter = painterResource(id = profileRes), 
                    contentDescription = "Profile",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
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
private fun StatusBoxClockedIn(since: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(StatusGreenBox)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
    onClick: () -> Unit
) {
    val isClockedIn = state == AttendanceState.CLOCKED_IN
    val buttonColor = if (isClockedIn) DarkButton else HeaderGreen
    val buttonText = if (isClockedIn) "Clock Out" else "Clock In"
    val icon = if (isClockedIn) Icons.AutoMirrored.Filled.ExitToApp else Icons.Default.Login

    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = buttonText,
                tint = Color.White
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                text = buttonText,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 720)
@Composable
private fun PreviewClockedOut() {
    MaterialTheme {
        TimeLogScreen(
            uiState = TimeLogUiState(
                fullName = "Eula Valdez",
                username = "@staff",
                attendanceState = AttendanceState.CLOCKED_OUT,
                clockedInSince = "10:34 AM"
            )
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 720)
@Composable
private fun PreviewClockedIn() {
    MaterialTheme {
        TimeLogScreen(
            uiState = TimeLogUiState(
                fullName = "Eula Valdez",
                username = "@staff",
                attendanceState = AttendanceState.CLOCKED_IN,
                clockedInSince = "10:34 AM"
            )
        )
    }
}
