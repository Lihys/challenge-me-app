package com.course.challengeme.ui.components
import androidx.compose.ui.graphics.Color

//this is for the ui
data class Challenge(
    val id: String,
    val title: String,
    val memberCount: Int,
    val myPoints: Int,
    val cardColor: Color,
    val myRank: Int? = null,
    val endDateLabel: String? = null
)