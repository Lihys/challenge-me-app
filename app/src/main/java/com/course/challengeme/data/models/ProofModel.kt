package com.course.challengeme.data.models

import com.google.firebase.Timestamp

data class ProofModel(
    val id: String = "",
    val challengeId: String = "",
    val userId: String = "",
    val type: String = "", // "text", "photo", or "location"
    val textContent: String? = null,
    val photoUrl: String? = null,

    // x,y on the map
    val x: Double? = null,
    val y: Double? = null,

    // readable place names that we get at submit time!
    // Null if the geo-coding thingy failed
    val locationName: String? = null,

    val pointsAwarded: Long = 0,
    val createdAt: Timestamp? = null
)