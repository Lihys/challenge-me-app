package com.course.challengeme.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.course.challengeme.data.repos.ChallengeRepo
import com.course.challengeme.data.models.LeaderboardEntry
import com.course.challengeme.data.models.LeaderboardMode
import com.course.challengeme.data.repos.ProofRepo
import com.course.challengeme.data.repos.UserRepo
import com.course.challengeme.data.models.buildLeaderboardEntries
import com.course.challengeme.ui.components.LeaderboardPodium
import com.course.challengeme.ui.components.LeaderboardToggle
import com.course.challengeme.ui.theme.AppBackground
import com.course.challengeme.ui.theme.AppText
import com.course.challengeme.ui.theme.MyBlue
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

@Composable
fun LeaderboardPage(navController: NavController, challengeId: String?) {
    var weeklyEntries by remember { mutableStateOf<List<LeaderboardEntry>>(emptyList()) }
    var totalEntries by remember { mutableStateOf<List<LeaderboardEntry>>(emptyList()) }
    var leaderboardMode by remember { mutableStateOf(LeaderboardMode.WEEKLY) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val challengeRepo = remember { ChallengeRepo() }
    val proofRepo = remember { ProofRepo() }
    val userRepo = remember { UserRepo() }
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    LaunchedEffect(challengeId) {
        if (challengeId == null) {
            errorMessage = "No challenge selected"
            isLoading = false
            return@LaunchedEffect
        }
        coroutineScope {
            val challengeDeferred = async { challengeRepo.getChallenge(challengeId) }
            val weeklyDeferred = async { proofRepo.getWeeklyLeaderboard(challengeId) }

            challengeDeferred.await()
                .onSuccess { challenge ->
                    val names = userRepo.getUsersByIds(challenge.memberIds)
                    totalEntries = buildLeaderboardEntries(challenge.memberIds, challenge.memberPoints, names)

                    weeklyDeferred.await().onSuccess { weeklyPairs ->
                        weeklyEntries = buildLeaderboardEntries(challenge.memberIds, weeklyPairs.toMap(), names)
                    }
                }
                .onFailure { errorMessage = it.localizedMessage ?: "Couldn't load leaderboard" }
        }
        isLoading = false
    }

    val entries = if (leaderboardMode == LeaderboardMode.TOTAL) totalEntries else weeklyEntries

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = AppText)
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text("Leaderboard", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = AppText)
        }

        when {
            isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MyBlue)
            }
            errorMessage != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(errorMessage ?: "", color = MaterialTheme.colorScheme.error)
            }
            entries.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No members yet", color = AppText.copy(alpha = 0.6f))
            }
            else -> {
                val topThree = entries.take(3)
                val myEntry = entries.find { it.userId == currentUserId }

                LeaderboardToggle(
                    selected = leaderboardMode,
                    onSelect = { leaderboardMode = it },
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .padding(top = 8.dp)
                )

                LeaderboardPodium(topThree = topThree, modifier = Modifier.padding(vertical = 24.dp))

                myEntry?.let { me ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(MyBlue)
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(me.name, color = Color.White, fontWeight = FontWeight.SemiBold)
                        Text("Points: ${me.points}", color = Color.White, fontSize = 13.sp)
                        Text("Rank: ${me.rank}", color = Color.White, fontSize = 13.sp)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(entries) { entry ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = entry.rank.toString().padStart(2, '0'),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = AppText,
                                modifier = Modifier.width(36.dp)
                            )
                            Column {
                                Text(entry.name, fontWeight = FontWeight.SemiBold, color = AppText)
                                Text("${entry.points} points", fontSize = 12.sp, color = AppText.copy(alpha = 0.6f))
                            }
                        }
                    }
                }
            }
        }
    }
}