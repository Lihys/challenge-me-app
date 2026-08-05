package com.course.challengeme.data

data class LeaderboardEntry(
    val userId: String,
    val name: String,
    val points: Long,
    val rank: Int
)