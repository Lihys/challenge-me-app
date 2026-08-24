package com.course.challengeme.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 *  "you're #x | y members | ends [date]"
 */
@Composable
fun ChallengeStatsLine(
    myRank: Int?,
    rankLabel: String? = null,
    memberCount: Int,
    endDateLabel: String?,
    fontSize: androidx.compose.ui.unit.TextUnit = 12.sp,
    fontWeight: FontWeight = FontWeight.Normal,
    color: Color,
    modifier: Modifier = Modifier
) {
    Text(
        text = buildString {
            myRank?.let {
                append("you're #$it")
                rankLabel?.let { label -> append(" $label") }
                append(" | ")
            }
            append("$memberCount members")
            endDateLabel?.let { append(" | ends $it") }
        },
        fontSize = fontSize,
        fontWeight = fontWeight,
        color = color,
        modifier = modifier
    )
}