package com.course.challengeme.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.course.challengeme.data.ChallengeRepo
import com.course.challengeme.data.ProofRepo
import com.course.challengeme.data.UserRepo
import com.course.challengeme.navigations.Navigation
import com.course.challengeme.ui.theme.AppBackground
import com.course.challengeme.ui.theme.AppText
import com.course.challengeme.ui.theme.ButtonDark
import com.course.challengeme.ui.theme.ChallengeBgTan
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

private data class AccountStats(
    val totalPoints: Long = 0,
    val checkIns: Int = 0,
    val challenges: Int = 0
)

@Composable
fun MyAccount(navController: NavController) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var stats by remember { mutableStateOf(AccountStats()) }
    var isLoading by remember { mutableStateOf(true) }

    val userRepository = remember { UserRepo() }
    val challengeRepository = remember { ChallengeRepo() }
    val proofRepository = remember { ProofRepo() }
    val auth = FirebaseAuth.getInstance()
    val userId = auth.currentUser?.uid
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(userId) {
        if (userId != null) {
            coroutineScope {
                val profileDeferred = async { userRepository.getUserProfile(userId) }
                val challengesDeferred = async { challengeRepository.getChallengesForUser(userId) }
                val checkInCountDeferred = async { proofRepository.getUpdateCountForUser(userId) }

                profileDeferred.await().onSuccess { (n, e) ->
                    name = n
                    email = e
                }

                val challenges = challengesDeferred.await()
                val checkInCount = checkInCountDeferred.await()

                challenges.onSuccess { list ->
                    val totalPoints = list.sumOf { it.memberPoints[userId] ?: 0L }
                    stats = AccountStats(
                        totalPoints = totalPoints,
                        checkIns = checkInCount.getOrDefault(0),
                        challenges = list.size
                    )
                }
            }
        }
        isLoading = false
    }

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
            Text("My Account", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = AppText)
        }

        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = ButtonDark)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                // Avatar + name/email + edit
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(ChallengeBgTan),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = name.firstOrNull()?.uppercase() ?: "?",
                            color = AppBackground,
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(name, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AppText)
                        Text(email, fontSize = 13.sp, color = AppText.copy(alpha = 0.6f))
                    }
                    IconButton(onClick = { /* profile editing — future step */ }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit profile", tint = AppText)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Stats grid — 3 columns, 2 rows
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard("Total Points", stats.totalPoints.toString(), Modifier.weight(1f))
                    StatCard("Check-ins", stats.checkIns.toString(), Modifier.weight(1f))
                    StatCard("Challenges", stats.challenges.toString(), Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard("Weekly Wins", "—", Modifier.weight(1f))
                    StatCard("Challenges Won", "—", Modifier.weight(1f))
                    Spacer(modifier = Modifier.weight(1f))
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            OutlinedButton(
                onClick = {
                    coroutineScope.launch {
                        auth.signOut()
                        navController.navigate(Navigation.Login.route) {
                            popUpTo(0)
                        }
                    }
                },
                shape = RoundedCornerShape(28.dp),
                border = BorderStroke(1.dp, ButtonDark),
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                Icon(Icons.Default.Logout, contentDescription = null, tint = ButtonDark, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sign Out", color = ButtonDark, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .border(BorderStroke(1.dp, ButtonDark.copy(alpha = 0.25f)), RoundedCornerShape(14.dp))
            .padding(vertical = 14.dp, horizontal = 10.dp)
    ) {
        Text(label, fontSize = 11.sp, color = AppText.copy(alpha = 0.6f))
        Spacer(modifier = Modifier.height(4.dp))
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AppText)
    }
}