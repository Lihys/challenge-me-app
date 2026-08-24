package com.course.challengeme.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.course.challengeme.data.repos.ChallengeRepo
import com.course.challengeme.navigations.Navigation
import com.course.challengeme.ui.components.TextField
import com.course.challengeme.ui.components.PrimaryButton
import com.course.challengeme.ui.theme.AppBackground
import com.course.challengeme.ui.theme.AppText
import com.course.challengeme.ui.theme.MyBlue
import kotlinx.coroutines.launch

@Composable
fun JoinViaCode(navController: NavController) {
    var code by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val challengeRepository = remember { ChallengeRepo() }
    val coroutineScope = rememberCoroutineScope()

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
            Text("Join a Challenge", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = AppText)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Enter the invite code a friend shared with you",
                fontSize = 14.sp,
                color = AppText.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(32.dp))

            TextField(
                value = code,
                onValueChange = { code = it.uppercase() },
                label = "Invite code"
            )

            errorMessage?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))

            PrimaryButton(
                text = "Join",
                isLoading = isLoading,
                enabled = code.isNotBlank(),
                containerColor = MyBlue,
                onClick = {
                    coroutineScope.launch {
                        isLoading = true
                        errorMessage = null
                        challengeRepository.joinChallengeViaCode(code)
                            .onSuccess { challengeId ->
                                navController.navigate(Navigation.ChallengePage.createRoute(challengeId)) {
                                    popUpTo(Navigation.Home.route)
                                }
                            }
                            .onFailure {
                                errorMessage = it.localizedMessage ?: "Couldn't join challenge"
                            }
                        isLoading = false
                    }
                }
            )
        }
    }
}