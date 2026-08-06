package com.course.challengeme.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.course.challengeme.data.ProofModel
import com.course.challengeme.ui.theme.AppBackground
import com.course.challengeme.ui.theme.AppText
import com.course.challengeme.ui.theme.ButtonDark
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun CheckIn(update: ProofModel, memberName: String, modifier: Modifier = Modifier) {
    val date = update.createdAt?.toDate()
    val today = Calendar.getInstance()
    val updateDay = Calendar.getInstance().apply { date?.let { time = it } }
    val isToday = date != null &&
            today.get(Calendar.DAY_OF_YEAR) == updateDay.get(Calendar.DAY_OF_YEAR) &&
            today.get(Calendar.YEAR) == updateDay.get(Calendar.YEAR)

    val timeLabel = date?.let {
        val prefix = if (isToday) "Today" else SimpleDateFormat("EEE", Locale.getDefault()).format(it)
        val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(it)
        "$prefix $time"
    } ?: ""

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .padding(1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(memberName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = AppText)
                Text(timeLabel, fontSize = 12.sp, color = AppText.copy(alpha = 0.5f))
            }
            Text(
                "+${update.pointsAwarded}",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = ButtonDark
            )
        }

        update.textContent?.let {
            Spacer(modifier = Modifier.height(4.dp))
            Text(it, fontSize = 13.sp, color = AppText.copy(alpha = 0.8f))
        }

        if (update.photoUrl != null || (update.y != null && update.x != null)) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (update.photoUrl != null) {
                    AssistChip(
                        onClick = {},
                        label = { Text("Photo proof", fontSize = 11.sp) },
                        leadingIcon = { Icon(Icons.Default.Photo, contentDescription = null, modifier = Modifier.size(14.dp)) },
                        colors = AssistChipDefaults.assistChipColors(containerColor = AppBackground)
                    )
                }
                if (update.y != null && update.x != null) {
                    AssistChip(
                        onClick = {},
                        label = { Text("Location", fontSize = 11.sp) },
                        leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(14.dp)) },
                        colors = AssistChipDefaults.assistChipColors(containerColor = AppBackground)
                    )
                }
            }
        }
    }
}