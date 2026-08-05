package com.course.challengeme.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.course.challengeme.data.ChallengeRepo
import com.course.challengeme.data.ChallengeModel
import com.course.challengeme.navigations.Navigation
import com.course.challengeme.ui.components.PrimaryButton
import com.course.challengeme.ui.theme.AppBackground
import com.course.challengeme.ui.theme.AppText
import com.course.challengeme.ui.theme.ButtonDark
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun ChallengePage(navController: NavController, challengeId: String?) {
    var challenge by remember { mutableStateOf<ChallengeModel?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val challengeRepository = remember { ChallengeRepo() }
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    LaunchedEffect(challengeId) {
        if (challengeId != null) {
            challengeRepository.getChallenge(challengeId)
                .onSuccess { challenge = it }
                .onFailure { errorMessage = it.localizedMessage ?: "Couldn't load challenge" }
            isLoading = false
        } else {
            errorMessage = "No challenge selected"
            isLoading = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
    ) {
        when {
            isLoading -> {
                CircularProgressIndicator(
                    color = ButtonDark,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            errorMessage != null -> {
                Text(
                    text = errorMessage ?: "",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            challenge != null -> {
                val c = challenge!!
                val isOwner = c.ownerId == currentUserId
                val dateLabel = c.endDate?.toDate()?.let {
                    SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(it)
                } ?: "No end date"

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                ) {
                    Text(c.title, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = AppText)

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        "Ends $dateLabel",
                        fontSize = 14.sp,
                        color = AppText.copy(alpha = 0.6f)
                    )

                    c.prize?.let {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Prize: $it", fontSize = 14.sp, color = AppText.copy(alpha = 0.6f))
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        "${c.memberIds.size} member${if (c.memberIds.size == 1) "" else "s"}",
                        fontSize = 14.sp,
                        color = AppText.copy(alpha = 0.6f)
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // Placeholder — daily update submission (text/photo/location) comes next
                    PrimaryButton(
                        text = "Submit Today's Update",
                        onClick = { /* next step */ }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = {
                            navController.navigate(Navigation.Leaderboard.createRoute(c.id))
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("View Leaderboard", color = AppText)
                    }

                    if (isOwner) {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = {
                                navController.navigate(Navigation.InviteCode.createRoute(c.id))
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Show Invite Code", color = AppText)
                        }
                    }
                }
            }
        }
    }
}