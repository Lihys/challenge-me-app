package com.course.challengeme.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.course.challengeme.data.LeaderboardMode
import com.course.challengeme.ui.theme.AppText
import com.course.challengeme.ui.theme.BlueLauncherBg
import com.course.challengeme.ui.theme.ButtonDark

/**
 * This Week/Total  toggle
 *
 * */
@Composable
fun LeaderboardToggle(
    selected: LeaderboardMode,
    onSelect: (LeaderboardMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(ButtonDark.copy(alpha = 0.08f))
            .padding(3.dp)
    ) {
        ToggleOption(
            label = "This Week",
            isSelected = selected == LeaderboardMode.WEEKLY,
            onClick = { onSelect(LeaderboardMode.WEEKLY) }
        )
        ToggleOption(
            label = "Total",
            isSelected = selected == LeaderboardMode.TOTAL,
            onClick = { onSelect(LeaderboardMode.TOTAL) }
        )
    }
}

@Composable
private fun RowScope.ToggleOption(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(17.dp))
            .background(if (isSelected) BlueLauncherBg.copy(alpha = 0.50f)  else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isSelected) Color.White else AppText
        )
    }
}