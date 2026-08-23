package com.course.challengeme.data

data class LeaderboardEntry(
    val userId: String,
    val name: String,
    val points: Long,
    val rank: Int,
    val avatarUrl: String? = null
)

enum class LeaderboardMode { WEEKLY, TOTAL }

fun buildLeaderboardEntries(
    userIds: List<String>,
    points: Map<String, Long>,
    profiles: Map<String, UserSummary>
): List<LeaderboardEntry> {
    return userIds
        .map { uid -> uid to (points[uid] ?: 0L) }
        .sortedByDescending { it.second }
        .mapIndexed { index, (uid, pts) ->
            val profile = profiles[uid]
            LeaderboardEntry(
                userId = uid,
                name = profile?.name ?: "Unknown",
                points = pts,
                rank = index + 1,
                avatarUrl = profile?.photoUrl
            )
        }
}