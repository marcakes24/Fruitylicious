package com.example.fruitylicious.ui.admin.reports

import androidx.lifecycle.ViewModel
import com.example.fruitylicious.util.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

data class ReportsUiState(
    val adminName: String = "",
    val isAdmin: Boolean = false,
    val userBranchId: String = "B1"
)

@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val sessionManager: SessionManager
) : ViewModel() {

    val uiState = ReportsUiState(
        adminName = sessionManager.getUserName().ifBlank { "Admin User" },
        isAdmin = sessionManager.getRole()?.equals("admin", ignoreCase = true) == true,
        userBranchId = "B${sessionManager.getBranchId()}"
    )
}
