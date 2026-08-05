package com.course.challengeme.data

data class Challenge(
    val id: String,
    val title: String,
    val memberCount: Int,
    val myPoints: Int,
    val cardColor: androidx.compose.ui.graphics.Color
)