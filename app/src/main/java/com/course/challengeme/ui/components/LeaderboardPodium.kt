package com.course.challengeme.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.course.challengeme.data.LeaderboardEntry
import com.course.challengeme.ui.theme.AppText
import com.course.challengeme.ui.theme.ChallengeBgTan

@Composable
fun LeaderboardPodium(
    topThree: List<LeaderboardEntry>,
    modifier: Modifier = Modifier
) {
    val first = topThree.getOrNull(0)
    val second = topThree.getOrNull(1)
    val third = topThree.getOrNull(2)

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        PodiumSpot(entry = third, barHeight = 80.dp)
        PodiumSpot(entry = first, barHeight = 120.dp)
        PodiumSpot(entry = second, barHeight = 100.dp)
    }
}

@Composable
private fun PodiumSpot(entry: LeaderboardEntry?, barHeight: Dp) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        MemberAvatar(name = entry?.name ?: "?", photoUrl = entry?.avatarUrl, size = 52.dp)
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = entry?.name ?: "—",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = AppText
        )
        Text(
            text = entry?.let { "${it.points} pts" } ?: "",
            fontSize = 11.sp,
            color = AppText.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .width(64.dp)
                .height(barHeight)
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                .background(ChallengeBgTan.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = entry?.rank?.toString() ?: "-",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = AppText
            )
        }
    }
}