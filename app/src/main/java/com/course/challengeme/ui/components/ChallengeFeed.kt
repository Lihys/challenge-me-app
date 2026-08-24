package com.course.challengeme.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.course.challengeme.data.ProofModel
import com.course.challengeme.data.ProofRepo
import com.course.challengeme.data.TeamBonusModel
import com.course.challengeme.data.UserSummary

// mamanges the real check-ins and the team-bonus row into a sorted feed :)

sealed class FeedItem {
    abstract val sortSeconds: Long
    data class Checkin(val proof: ProofModel) : FeedItem() {
        override val sortSeconds get() = proof.createdAt?.seconds ?: 0L
    }
    data class Bonus(val bonus: TeamBonusModel) : FeedItem() {
        override val sortSeconds get() = bonus.updatedAt?.seconds ?: 0L
    }
}

private fun buildFeedItems(
    recentUpdates: List<ProofModel>,
    dailyBonus: TeamBonusModel?
): List<FeedItem> {
    val items = mutableListOf<FeedItem>()
    items += recentUpdates.map { FeedItem.Checkin(it) }
    dailyBonus?.takeIf { it.checkedInMemberIds.size >= 2 }?.let { items += FeedItem.Bonus(it) }
    return items.sortedWith(
        compareByDescending<FeedItem> { it.sortSeconds }
            .thenByDescending { it is FeedItem.Bonus } // ties go to the bonus banner
    )
}

@Composable
fun ChallengeFeed(
    recentUpdates: List<ProofModel>,
    dailyBonus: TeamBonusModel?,
    memberNames: Map<String, UserSummary>,
    modifier: Modifier = Modifier
) {
    val feedItems = remember(recentUpdates, dailyBonus) {
        buildFeedItems(recentUpdates, dailyBonus)
    }

    Column(modifier = modifier) {
        feedItems.forEach { item ->
            when (item) {
                is FeedItem.Checkin -> CheckIn(
                    update = item.proof,
                    memberName = memberNames[item.proof.userId]?.name ?: "Unknown",
                    avatarUrl = memberNames[item.proof.userId]?.photoUrl
                )
                is FeedItem.Bonus -> TeamBonusBanner(
                    memberCount = item.bonus.checkedInMemberIds.size,
                    bonusPoints = ProofRepo.TEAM_BONUS
                )
            }
        }
    }
}