package com.example.fruitylicious

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

// High-fidelity Color Palette from images
private val BrandGreen = Color(0xFF2E7D32)
private val LightBgYellow = Color(0xFFFDEB95)
private val CardWhite = Color.White
private val TextGrey = Color(0xFF757575)
private val ErrorRed = Color(0xFFE57373)

data class User(
    val id: Int,
    val name: String,
    val username: String,
    val role: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserManagementScreen(
    navController: NavController,
    drawerState: DrawerState,
    scope: CoroutineScope
) {
    val userList = remember { mutableStateListOf(
        User(1, "Mianne Navarro", "Mianne", "Owner"),
        User(2, "Ber Navarro", "Ber", "Owner"),
        User(3, "Mariz Tuliao", "Mariz", "Staff")
    )}

    var showAddDialog by remember { mutableStateOf(false) }
    var userToEdit by remember { mutableStateOf<User?>(null) }
    var userToDelete by remember { mutableStateOf<User?>(null) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("USER MANAGEMENT", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                        Icon(Icons.Default.Menu, contentDescription = null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = BrandGreen)
            )
        },
        containerColor = LightBgYellow,
        bottomBar = {
            Box(modifier = Modifier.padding(16.dp)) {
                Button(
                    onClick = { showAddDialog = true },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("+ Add User", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(userList) { user ->
                UserListItem(user) { userToEdit = user }
            }
        }
    }

    if (showAddDialog) {
        AddUserDialog(
            onDismiss = { showAddDialog = false },
            onSave = { userList.add(it); showAddDialog = false }
        )
    }

    userToEdit?.let { user ->
        EditUserDialog(
            user = user,
            onDismiss = { userToEdit = null },
            onUpdate = { updated ->
                val index = userList.indexOfFirst { it.id == user.id }
                if (index != -1) userList[index] = updated
                userToEdit = null
            },
            onDeleteClick = {
                userToDelete = user
                userToEdit = null
            }
        )
    }

    userToDelete?.let { user ->
        DeleteUserDialog(
            userName = user.name,
            onDismiss = { userToDelete = null },
            onConfirm = { userList.remove(user); userToDelete = null }
        )
    }
}

@Composable
fun UserListItem(user: User, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(90.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(50.dp).background(Color(0xFFF5F5F5), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (user.role == "Owner") Icons.Default.Settings else Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = Color.Black
                )
            }
            Spacer(Modifier.width(20.dp))
            Column {
                Text(user.name, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.Black)
                Text(user.username, fontSize = 13.sp, color = TextGrey)
                Text(user.role, fontSize = 13.sp, color = TextGrey)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddUserDialog(onDismiss: () -> Unit, onSave: (User) -> Unit) {
    var name by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf("Owner") }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Card(
            modifier = Modifier.fillMaxWidth(0.92f).padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = CardWhite)
        ) {
            Column(Modifier.padding(24.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Add User", fontWeight = FontWeight.Bold, fontSize = 22.sp)
                        Text("Add new user information", fontSize = 13.sp, color = TextGrey)
                    }
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null) }
                }

                Spacer(Modifier.height(24.dp))
                CustomLabelledField("Name", name, "Enter name") { name = it }
                Spacer(Modifier.height(16.dp))
                CustomLabelledField("Username", username, "Enter username") { username = it }
                Spacer(Modifier.height(16.dp))
                CustomLabelledField("Password", password, "Enter password", isPassword = true) { password = it }

                Spacer(Modifier.height(24.dp))
                Text("Select Role", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    RoleSelectionButton("Owner", selectedRole == "Owner", Modifier.weight(1f)) { selectedRole = "Owner" }
                    RoleSelectionButton("Staff", selectedRole == "Staff", Modifier.weight(1f)) { selectedRole = "Staff" }
                }

                Spacer(Modifier.height(32.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color.LightGray)
                    ) { Text("Discard", color = Color.Black) }

                    Button(
                        onClick = { onSave(User((100..999).random(), name, username, selectedRole)) },
                        modifier = Modifier.weight(1f).height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
                        shape = RoundedCornerShape(10.dp)
                    ) { Text("Save", color = Color.White) }
                }
            }
        }
    }
}

// Additional components for Edit and Delete (Refined)
@Composable
fun CustomLabelledField(label: String, value: String, placeholder: String, isPassword: Boolean = false, onValueChange: (String) -> Unit) {
    Column {
        Text(label, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.padding(bottom = 6.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = Color.LightGray) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = TextFieldDefaults.colors(
                unfocusedContainerColor = Color.Transparent,
                focusedContainerColor = Color.Transparent,
                unfocusedIndicatorColor = Color.LightGray,
                focusedIndicatorColor = BrandGreen
            )
        )
    }
}

@Composable
fun RoleSelectionButton(label: String, isSelected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, if (isSelected) BrandGreen else Color.LightGray),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (isSelected) Color(0xFFE8F5E9) else Color.Transparent
        )
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (label == "Owner") Icons.Default.Settings else Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = if (isSelected) BrandGreen else Color.Black
            )
            Spacer(Modifier.width(8.dp))
            Text(label, color = if (isSelected) BrandGreen else Color.Black, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
        }
    }
}

@Composable
fun DeleteUserDialog(userName: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = CardWhite)) {
            Column(Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(shape = CircleShape, color = Color(0xFFFFEBEE), modifier = Modifier.size(72.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Delete, null, tint = ErrorRed, modifier = Modifier.size(36.dp))
                    }
                }
                Spacer(Modifier.height(20.dp))
                Text("Delete User", fontWeight = FontWeight.ExtraBold, fontSize = 24.sp)
                Text("Are you sure you want to\nremove this user?", textAlign = TextAlign.Center, color = TextGrey, modifier = Modifier.padding(top = 8.dp))
                Spacer(Modifier.height(32.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button(onClick = onDismiss, modifier = Modifier.weight(1f).height(48.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF90A4AE)), shape = RoundedCornerShape(12.dp)) { Text("Cancel") }
                    Button(onClick = onConfirm, modifier = Modifier.weight(1f).height(48.dp), colors = ButtonDefaults.buttonColors(containerColor = ErrorRed), shape = RoundedCornerShape(12.dp)) { Text("Delete User") }
                }
            }
        }
    }
}

@Composable
fun EditUserDialog(user: User, onDismiss: () -> Unit, onUpdate: (User) -> Unit, onDeleteClick: () -> Unit) {
    // Similar to AddUserDialog but with initial values and "Update" / "Delete User" buttons as seen in image_96de74.png
    var name by remember { mutableStateOf(user.name) }
    var username by remember { mutableStateOf(user.username) }
    var selectedRole by remember { mutableStateOf(user.role) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Card(
            modifier = Modifier.fillMaxWidth(0.92f).padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = CardWhite)
        ) {
            Column(Modifier.padding(24.dp)) {
                Text("Edit / Delete User", fontWeight = FontWeight.Bold, fontSize = 22.sp)
                Text("Modify user profile", fontSize = 13.sp, color = TextGrey)

                Spacer(Modifier.height(24.dp))
                CustomLabelledField("Name", name, "") { name = it }
                Spacer(Modifier.height(16.dp))
                CustomLabelledField("Username", username, "") { username = it }

                Spacer(Modifier.height(24.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    RoleSelectionButton("Owner", selectedRole == "Owner", Modifier.weight(1f)) { selectedRole = "Owner" }
                    RoleSelectionButton("Staff", selectedRole == "Staff", Modifier.weight(1f)) { selectedRole = "Staff" }
                }

                Spacer(Modifier.height(32.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = onDeleteClick, modifier = Modifier.weight(1f).height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = ErrorRed), shape = RoundedCornerShape(10.dp)) { Text("Delete User") }
                    Button(onClick = { onUpdate(User(user.id, name, username, selectedRole)) }, modifier = Modifier.weight(1f).height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = BrandGreen), shape = RoundedCornerShape(10.dp)) { Text("Update") }
                }
            }
        }
    }
}