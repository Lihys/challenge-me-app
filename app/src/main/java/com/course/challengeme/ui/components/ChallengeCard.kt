package com.course.challengeme.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.course.challengeme.data.Challenge

@Composable
fun ChallengeCard(
    challenge: Challenge,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(challenge.cardColor)
            .clickable(onClick = onClick)
            .padding(20.dp)
    ) {
        Text(
            text = challenge.title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${challenge.memberCount} members",
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.85f)
            )
            Text(
                text = "${challenge.myPoints} pts",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }
    }
}