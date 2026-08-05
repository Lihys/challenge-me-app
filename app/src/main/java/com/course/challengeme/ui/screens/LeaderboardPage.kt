package com.course.challengeme.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import com.course.challengeme.data.ChallengeRepo
import com.course.challengeme.data.LeaderboardEntry
import com.course.challengeme.data.UserRepo
import com.course.challengeme.ui.components.LeaderboardPodium
import com.course.challengeme.ui.theme.AppBackground
import com.course.challengeme.ui.theme.AppText
import com.course.challengeme.ui.theme.ButtonDark
import com.course.challengeme.ui.theme.ChallengeBgTan
import com.google.firebase.auth.FirebaseAuth

@Composable
fun LeaderboardPage(navController: NavController, challengeId: String?) {
    var entries by remember { mutableStateOf<List<LeaderboardEntry>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val challengeRepo = remember { ChallengeRepo() }
    val userRepo = remember { UserRepo() }
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    LaunchedEffect(challengeId) {
        if (challengeId == null) {
            errorMessage = "No challenge selected"
            isLoading = false
            return@LaunchedEffect
        }
        challengeRepo.getChallenge(challengeId)
            .onSuccess { challenge ->
                val names = userRepo.getUsersByIds(challenge.memberIds)
                entries = challenge.memberIds
                    .map { uid -> uid to (challenge.memberPoints[uid] ?: 0L) }
                    .sortedByDescending { it.second }
                    .mapIndexed { index, (uid, points) ->
                        LeaderboardEntry(
                            userId = uid,
                            name = names[uid] ?: "Unknown",
                            points = points,
                            rank = index + 1
                        )
                    }
            }
            .onFailure { errorMessage = it.localizedMessage ?: "Couldn't load leaderboard" }
        isLoading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
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
                CircularProgressIndicator(color = ButtonDark)
            }
            errorMessage != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(errorMessage ?: "", color = MaterialTheme.colorScheme.error)
            }
            entries.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No members yet", color = AppText.copy(alpha = 0.6f))
            }
            else -> {
                val topThree = entries.take(3)
                val rest = entries.drop(3)
                val myEntry = entries.find { it.userId == currentUserId }

                LeaderboardPodium(topThree = topThree, modifier = Modifier.padding(vertical = 24.dp))

                myEntry?.let { me ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(ButtonDark)
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.size(44.dp).clip(CircleShape).background(ChallengeBgTan),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(me.name.firstOrNull()?.uppercase() ?: "?", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(me.name, color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
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
                    items(rest) { entry ->
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