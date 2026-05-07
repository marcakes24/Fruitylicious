package com.example.fruitylicious.ui.shared

import androidx.compose.runtime.Composable
import androidx.compose.material3.DrawerState
import androidx.navigation.NavController
import kotlinx.coroutines.CoroutineScope

@Composable
fun SharedDrawerContent(
    mode: SharedScreenMode,
    navController: NavController,
    drawerState: DrawerState,
    scope: CoroutineScope,
    userName: String,
    branchName: String = "",
    isClockedIn: Boolean = false,
    onLogout: () -> Unit
) {
    when (mode) {
        SharedScreenMode.OWNER -> {
            OwnerSideBarContent(
                navController = navController,
                drawerState = drawerState,
                scope = scope,
                ownerName = userName.ifBlank { "Admin User" },
                onLogout = onLogout
            )
        }

        SharedScreenMode.STAFF -> {
            StaffSideBarContent(
                navController = navController,
                drawerState = drawerState,
                scope = scope,
                staffName = userName.ifBlank { "Staff User" },
                branchName = branchName.ifBlank { "Branch" },
                isClockedIn = isClockedIn,
                onLogout = onLogout
            )
        }
    }
}
