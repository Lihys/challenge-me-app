package com.course.challengeme.data

import com.google.firebase.Timestamp

data class TeamBonusModel(
    val dateKey: String = "",
    val checkedInMemberIds: List<String> = emptyList(),
    val bonusAwardedMemberIds: List<String> = emptyList(),
    val updatedAt: Timestamp? = null
)