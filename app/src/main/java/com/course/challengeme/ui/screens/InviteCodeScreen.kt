package com.course.challengeme.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.course.challengeme.data.repos.ChallengeRepo
import com.course.challengeme.navigations.Navigation
import com.course.challengeme.ui.components.PrimaryButton
import com.course.challengeme.ui.theme.AppBackground
import com.course.challengeme.ui.theme.AppText
import com.course.challengeme.ui.theme.Maroon
import com.course.challengeme.ui.theme.MyBlue

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
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = AppText)
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Share this code so friends can join!",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = AppText,
                textAlign = TextAlign.Center)

            Spacer(modifier = Modifier.height(40.dp))

            if (isLoading) {
                CircularProgressIndicator(color = Maroon)
            } else {
                Text(
                    text = inviteCode ?: "—",
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 8.sp,
                    color = Maroon
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
                containerColor = MyBlue,
                onClick = {
                    navController.navigate(Navigation.Home.route) {
                        popUpTo(Navigation.Home.route) { inclusive = true }
                    }
                }
            )
        }
    }
}