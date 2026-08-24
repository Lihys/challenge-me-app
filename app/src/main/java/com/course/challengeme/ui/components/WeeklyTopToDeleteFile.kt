// TO DELETE I THINK

package com.course.challengeme.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.course.challengeme.ui.theme.AppText
import com.course.challengeme.ui.theme.ButtonDark

data class TopMember(val name: String, val points: Long)

@Composable
fun WeeklyTop(topThree: List<TopMember>, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        topThree.forEach { member ->
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .border(BorderStroke(1.dp, ButtonDark.copy(alpha = 0.3f)), RoundedCornerShape(12.dp))
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(member.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = AppText)
                Spacer(modifier = Modifier.height(4.dp))
                Text("(${member.points})", fontSize = 12.sp, color = ButtonDark)
            }
        }
    }
}