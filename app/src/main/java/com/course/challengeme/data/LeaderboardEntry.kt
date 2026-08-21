package com.course.challengeme.data

data class LeaderboardEntry(
    val userId: String,
    val name: String,
    val points: Long,
    val rank: Int
)

enum class LeaderboardMode { WEEKLY, TOTAL }

/**
 * Ranks [userIds] by [points] (a member missing from the map defaults to 0
 * rather than being dropped), attaching display [names]. Both the weekly and
 * total leaderboard views are built through this so ChallengePage and
 * LeaderboardPage never disagree on ranking/tie-breaking.
 */
fun buildLeaderboardEntries(
    userIds: List<String>,
    points: Map<String, Long>,
    names: Map<String, String>
): List<LeaderboardEntry> {
    return userIds
        .map { uid -> uid to (points[uid] ?: 0L) }
        .sortedByDescending { it.second }
        .mapIndexed { index, (uid, pts) ->
            LeaderboardEntry(
                userId = uid,
                name = names[uid] ?: "Unknown",
                points = pts,
                rank = index + 1
            )
        }
}