package com.course.challengeme.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.course.challengeme.data.ChallengeModel
import com.course.challengeme.data.LeaderboardEntry
import com.course.challengeme.data.LeaderboardMode
import com.course.challengeme.ui.theme.AppText
import com.course.challengeme.ui.theme.BlueLauncherBg
import com.course.challengeme.ui.theme.ButtonDark

@Composable
fun ChallengeHeaderSection(
    challenge: ChallengeModel,
    leaderboardEntries: List<LeaderboardEntry>,
    leaderboardMode: LeaderboardMode,
    onModeSelect: (LeaderboardMode) -> Unit,
    myRank: Int?,
    dateLabel: String,
    onInviteCodeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isDescriptionExpanded by remember { mutableStateOf(false) }
    var isDescriptionCutOff by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        LeaderboardPodium(topThree = leaderboardEntries.take(3))
        Spacer(modifier = Modifier.height(12.dp))
        LeaderboardToggle(
            selected = leaderboardMode,
            onSelect = onModeSelect
        )
        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                challenge.title,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = BlueLauncherBg,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            TextButton(
                onClick = onInviteCodeClick,
                colors = ButtonDefaults.textButtonColors(
                    containerColor = BlueLauncherBg,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(24.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Icon(
                    Icons.Filled.ConfirmationNumber,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Invite code", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        ChallengeStatsLine(
            myRank = myRank,
            rankLabel = if (leaderboardMode == LeaderboardMode.TOTAL) "overall" else "this week",
            memberCount = challenge.memberIds.size,
            endDateLabel = dateLabel,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                challenge.description?.takeIf { it.isNotBlank() }?.let { desc ->
                    Text(
                        text = desc,
                        fontSize = 13.sp,
                        color = AppText.copy(alpha = 0.7f),
                        maxLines = if (isDescriptionExpanded) Int.MAX_VALUE else 2,
                        overflow = TextOverflow.Ellipsis,
                        onTextLayout = { result ->
                            if (!isDescriptionExpanded) {
                                isDescriptionCutOff = result.hasVisualOverflow
                            }
                        }
                    )
                    if (isDescriptionCutOff || isDescriptionExpanded) {
                        Text(
                            text = if (isDescriptionExpanded) "Hide description" else "See description",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = ButtonDark,
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .clickable { isDescriptionExpanded = !isDescriptionExpanded }
                        )
                    }
                }
            }

            challenge.prize?.takeIf { it.isNotBlank() }?.let { prize ->
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(BlueLauncherBg.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Prize:",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = BlueLauncherBg,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = prize,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = BlueLauncherBg,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        Text("Check-ins", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AppText)
        Spacer(modifier = Modifier.height(12.dp))
    }
}