package com.course.challengeme.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.course.challengeme.ui.theme.AppText
import com.course.challengeme.ui.theme.Maroon
import com.course.challengeme.ui.theme.ChallengeBgTan

@Composable
fun TeamBonusBanner(memberCount: Int, bonusPoints: Long, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(ChallengeBgTan.copy(alpha = 0.35f))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Team bonus!", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = AppText)
                Text("$memberCount members checked in today!", fontSize = 12.sp, color = AppText.copy(alpha = 0.7f))
            }
            Text("+$bonusPoints pts each", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Maroon)
        }
        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(color = AppText.copy(alpha = 0.08f))
        Spacer(modifier = Modifier.height(12.dp))
    }
}