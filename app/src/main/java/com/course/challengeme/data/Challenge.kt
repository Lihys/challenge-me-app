package com.course.challengeme.data
import androidx.compose.ui.graphics.Color

data class Challenge(
    val id: String,
    val title: String,
    val memberCount: Int,
    val myPoints: Int,
    val cardColor: Color,
    val myRank: Int? = null,
    val endDateLabel: String? = null
)