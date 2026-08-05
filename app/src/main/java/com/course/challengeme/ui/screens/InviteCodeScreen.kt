package com.course.challengeme.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.text.letterSpacing
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.course.challengeme.data.ChallengeRepo
import com.course.challengeme.navigations.Navigation
import com.course.challengeme.ui.components.PrimaryButton
import com.course.challengeme.ui.theme.AppBackground
import com.course.challengeme.ui.theme.AppText
import com.course.challengeme.ui.theme.ButtonDark

@Composable
fun InviteCodeScreen(navController: NavController, challengeId: String?) {
    var inviteCode by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    val challengeRepository = remember { ChallengeRepo() }
    val clipboardManager = LocalClipboardManager.current

    LaunchedEffect(challengeId) {
        if (challengeId != null) {
            challengeRepository.getChallenge(challengeId)
                .onSuccess { inviteCode = it.inviteCode }
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("You're all set!", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = AppText)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Share this code so friends can join",
            fontSize = 14.sp,
            color = AppText.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(40.dp))

        if (isLoading) {
            CircularProgressIndicator(color = ButtonDark)
        } else {
            Text(
                text = inviteCode ?: "—",
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 8.sp,
                color = ButtonDark
            )

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedButton(
                onClick = {
                    inviteCode?.let {
                        clipboardManager.setText(AnnotatedString(it))
                    }
                }
            ) {
                Text("Copy code", color = AppText)
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        PrimaryButton(
            text = "Go to Home",
            onClick = {
                navController.navigate(Navigation.Home.route) {
                    popUpTo(Navigation.Home.route) { inclusive = true }
                }
            }
        )
    }
}