package com.course.challengeme.data

import com.google.firebase.Timestamp

data class ChallengeModel(
    val id: String = "",
    val title: String = "",
    val prize: String? = null,
    val endDate: Timestamp? = null,
    val inviteCode: String = "",
    val ownerId: String = "",
    val memberIds: List<String> = emptyList(),
    val memberPoints: Map<String, Long> = emptyMap(),
    val winnerId: String? = null,
    val createdAt: Timestamp? = null
)