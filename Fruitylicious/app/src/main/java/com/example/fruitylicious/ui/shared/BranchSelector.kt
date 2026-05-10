package com.example.fruitylicious.ui.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fruitylicious.data.local.entity.BranchEntity

/**
 * A shared component for selecting branches, used across various admin and shared screens.
 */
@Composable
fun BranchSelector(
    selectedBranchId: Int?,
    branches: List<BranchEntity>,
    isOnline: Boolean,
    onBranchSelected: (Int?) -> Unit,
    modifier: Modifier = Modifier,
    activeColor: Color = Color(0xFF2E7D32), // Default Fruitylicious Green
    containerColor: Color = Color(0xFFF5F5F5),
    contentColor: Color = Color(0xFF666E7A),
    localBranchId: Int? = null
) {
    // Ensure displayBranches contains at least the local branch if provided
    val displayBranches = remember(branches, localBranchId) {
        if (localBranchId != null && branches.none { it.branchId == localBranchId }) {
            val localBranch = BranchEntity(
                branchId = localBranchId,
                branchName = "Local Branch",
                address = "",
                contactNumber = "",
                lastModified = 0L,
                isSynced = true,
                syncedAt = null
            )
            (branches + localBranch).sortedBy { it.branchId }
        } else {
            branches.sortedBy { it.branchId }
        }
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (isOnline) containerColor else containerColor.copy(alpha = 0.5f))
            .padding(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BranchSelectorTab(
            label = "All",
            isSelected = selectedBranchId == null,
            enabled = isOnline,
            activeColor = activeColor,
            contentColor = contentColor,
            onClick = { onBranchSelected(null) }
        )

        displayBranches.forEach { branch ->
            val isLocal = branch.branchId == localBranchId
            BranchSelectorTab(
                label = "B${branch.branchId}",
                isSelected = selectedBranchId == branch.branchId,
                enabled = isLocal || isOnline,
                activeColor = activeColor,
                contentColor = contentColor,
                onClick = { onBranchSelected(branch.branchId) }
            )
        }
    }
}

@Composable
private fun BranchSelectorTab(
    label: String,
    isSelected: Boolean,
    enabled: Boolean,
    activeColor: Color,
    contentColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) activeColor else Color.Transparent)
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isSelected) Color.White else contentColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
