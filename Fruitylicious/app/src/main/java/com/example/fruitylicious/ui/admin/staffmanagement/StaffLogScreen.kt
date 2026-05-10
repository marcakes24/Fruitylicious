package com.example.fruitylicious.ui.admin.staffmanagement

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.fruitylicious.data.local.entity.BranchEntity
import com.example.fruitylicious.ui.shared.BranchSelector
import com.example.fruitylicious.ui.shared.ErrorWarning
import com.example.fruitylicious.ui.shared.FruityDateFilterField
import com.example.fruitylicious.ui.shared.FruityDatePicker
import com.example.fruitylicious.ui.shared.FruitySearchField
import com.example.fruitylicious.ui.shared.OwnerSideBarContent
import com.example.fruitylicious.util.ImageStorage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

private val SlGreen = Color(0xFF2E7D32)
private val SlPageBg = Color(0xFFFFEAA0)
private val SlTextMain = Color(0xFF1E293B)
private val SlTextSub = Color.Gray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffLogScreen(
    navController: NavController,
    adminName: String = "Admin User",
    onLogout: () -> Unit = {},
    viewModel: StaffLogViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var expandedImagePath by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedDateMillis by remember { mutableLongStateOf(0L) }
    val datePickerState = rememberDatePickerState()

    val selectedDateText = remember(selectedDateMillis) {
        if (selectedDateMillis == 0L) "" else SimpleDateFormat("MM/dd/yyyy", Locale.US).format(Date(selectedDateMillis))
    }

    val filteredLogs = remember(uiState.logs, searchQuery, selectedDateMillis) {
        uiState.logs.filter { log ->
            val matchesSearch = log.staffName.contains(searchQuery, ignoreCase = true) ||
                    log.username.contains(searchQuery, ignoreCase = true) ||
                    log.branchName.contains(searchQuery, ignoreCase = true)

            val matchesDate = if (selectedDateMillis == 0L) {
                true
            } else {
                val logCal = Calendar.getInstance().apply { timeInMillis = log.clockIn }
                val filterCal = Calendar.getInstance().apply { timeInMillis = selectedDateMillis }
                logCal.get(Calendar.YEAR) == filterCal.get(Calendar.YEAR) &&
                        logCal.get(Calendar.DAY_OF_YEAR) == filterCal.get(Calendar.DAY_OF_YEAR)
            }
            matchesSearch && matchesDate
        }
    }

    if (showDatePicker) {
        FruityDatePicker(
            state = datePickerState,
            onDismiss = { showDatePicker = false },
            onConfirm = { millis ->
                selectedDateMillis = millis ?: 0L
            },
            onClear = {
                selectedDateMillis = 0L
            }
        )
    }

    if (expandedImagePath != null) {
        ExpandedImageDialog(
            imagePath = expandedImagePath!!,
            onDismiss = { expandedImagePath = null }
        )
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
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(SlPageBg)
            ) {
                Header(
                    selectedBranchId = uiState.selectedBranchId,
                    branches = uiState.branches,
                    isAdmin = uiState.isAdmin,
                    isOnline = uiState.isOnline,
                    localBranchId = uiState.localBranchId,
                    onBranchSelect = { branchId ->
                        viewModel.onBranchSelected(branchId)
                    },
                    onMenuClick = {
                        scope.launch {
                            drawerState.open()
                        }
                    }
                )

                ErrorWarning(message = uiState.error)

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (uiState.isAdmin && !uiState.isOnline) {
                        item {
                            Text(
                                text = "Offline mode: Only local branch staff logs are available.",
                                color = SlTextSub,
                                fontSize = 12.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp)
                            )
                        }
                    }

                    item {
                        FilterCard(
                            searchQuery = searchQuery,
                            onSearchChange = { searchQuery = it },
                            selectedDateText = selectedDateText,
                            onDateClick = { showDatePicker = true },
                            onDateClear = { selectedDateMillis = 0L }
                        )
                    }

                    if (uiState.isLoading) {
                        item {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(24.dp),
                                color = Color.White,
                                shadowElevation = 2.dp
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(48.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    androidx.compose.material3.CircularProgressIndicator(color = SlGreen)
                                }
                            }
                        }
                    } else if (filteredLogs.isEmpty()) {
                        item {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(24.dp),
                                color = Color.White,
                                shadowElevation = 2.dp
                            ) {
                                EmptyStaffLogText("No staff logs found")
                            }
                        }
                    } else {
                        items(
                            items = filteredLogs,
                            key = { it.logId }
                        ) { log ->
                            StaffLogCard(
                                log = log,
                                onImageClick = { path -> expandedImagePath = path }
                            )
                        }

                        if (uiState.hasMore) {
                            item {
                                Button(
                                    onClick = { viewModel.loadMore() },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !uiState.isLoadingMore,
                                    colors = ButtonDefaults.buttonColors(containerColor = SlGreen)
                                ) {
                                    Text(if (uiState.isLoadingMore) "Loading..." else "Load More")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(
    selectedBranchId: Int?,
    branches: List<BranchEntity>,
    isAdmin: Boolean,
    isOnline: Boolean,
    localBranchId: Int,
    onBranchSelect: (Int?) -> Unit,
    onMenuClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(SlGreen)
            .padding(top = 40.dp, bottom = 12.dp, start = 16.dp, end = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onMenuClick) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Menu",
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "STAFF LOG",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )

            if (isAdmin) {
                BranchSelector(
                    selectedBranchId = selectedBranchId,
                    branches = branches,
                    isOnline = isOnline,
                    onBranchSelected = onBranchSelect,
                    activeColor = SlGreen,
                    containerColor = Color(0xFFF5F5F5),
                    localBranchId = localBranchId
                )
            }
        }
    }
}

@Composable
private fun FilterCard(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    selectedDateText: String,
    onDateClick: () -> Unit,
    onDateClear: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FruitySearchField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = "Search staff name, username, or branch"
            )

            FruityDateFilterField(
                selectedDateText = selectedDateText,
                onClick = onDateClick,
                onClear = onDateClear
            )
        }
    }
}

@Composable
private fun EmptyStaffLogText(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = SlTextSub,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun StaffLogCard(
    log: StaffLogRow,
    onImageClick: (String) -> Unit
) {
    val context = LocalContext.current

    val imageFile = log.imagePath?.let {
        ImageStorage.getImageFile(context, it)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF5F5F5))
                        .clickable(enabled = log.imagePath != null) {
                            onImageClick(log.imagePath!!)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (imageFile != null && imageFile.exists()) {
                        AsyncImage(
                            model = imageFile,
                            contentDescription = "Staff log image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Staff",
                            tint = SlGreen,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column {
                            Text(
                                text = log.staffName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = SlTextMain
                            )

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "@${log.username}",
                                    fontSize = 13.sp,
                                    color = SlTextSub
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                Surface(
                                    color = Color(0xFFE2E8F0),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = log.branchName.ifBlank { "B${log.branchId}" },
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }
                        }

                        Text(
                            text = formatDate(log.clockIn),
                            fontSize = 13.sp,
                            color = SlTextSub
                        )
                    }

                    Spacer(modifier = Modifier.size(12.dp))

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFFF8FAFC),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            StaffLogTimeRow(
                                label = "Clock In:",
                                value = formatTime(log.clockIn),
                                indicatorColor = Color(0xFF22C55E)
                            )

                            Spacer(modifier = Modifier.size(8.dp))

                            StaffLogTimeRow(
                                label = "Clock Out:",
                                value = log.clockOut?.let { formatTime(it) } ?: "Active",
                                indicatorColor = if (log.clockOut == null) Color(0xFF22C55E) else Color.Gray,
                                active = log.clockOut == null
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StaffLogTimeRow(
    label: String,
    value: String,
    indicatorColor: Color,
    active: Boolean = false
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(indicatorColor)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = label,
            fontSize = 13.sp,
            color = SlTextSub
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = if (active) Color(0xFF22C55E) else SlTextMain
        )
    }
}

private fun formatDate(timestamp: Long): String {
    return SimpleDateFormat("MMM dd, yyyy", Locale.US).format(Date(timestamp))
}

private fun formatTime(timestamp: Long): String {
    return SimpleDateFormat("hh:mm a", Locale.US).format(Date(timestamp))
}

@Composable
fun ExpandedImageDialog(
    imagePath: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val imageFile = ImageStorage.getImageFile(context, imagePath)

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
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
