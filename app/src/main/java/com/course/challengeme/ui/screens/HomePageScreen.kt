package com.course.challengeme.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.course.challengeme.data.Challenge
import com.course.challengeme.navigations.Navigation
import com.course.challengeme.ui.components.ChallengeCard
import com.course.challengeme.ui.theme.AppBackground
import com.course.challengeme.ui.theme.AppText
import com.course.challengeme.ui.theme.ButtonDark
import com.course.challengeme.ui.theme.ChallengeBgMauve
import com.course.challengeme.ui.theme.ChallengeBgRed
import com.course.challengeme.ui.theme.ChallengeBgTan

// hardcoded data to change ofc
private val placeholderChallenges = listOf(
    Challenge("1", "No Skipping Class", 6, 120, ChallengeBgRed),
    Challenge("2", "Gym 5x a Week", 4, 85, ChallengeBgTan),
    Challenge("3", "Daily Reading", 3, 40, ChallengeBgMauve),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomePageScreen(navController: NavController) {
    Scaffold(
        containerColor = AppBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text("Your Challenges", color = AppText, fontWeight = FontWeight.Bold)
                },
                actions = {
                    IconButton(onClick = { navController.navigate(Navigation.MyAccount.route) }) {
                        Icon(Icons.Default.AccountCircle, contentDescription = "My Account", tint = AppText)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppBackground)
            )
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                SmallFloatingActionButton(
                    onClick = { navController.navigate(Navigation.JoinChallenge.route) },
                    containerColor = ButtonDark,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Filled.ConfirmationNumber, contentDescription = "Join a challenge")
                }
                Spacer(modifier = Modifier.height(12.dp))
                FloatingActionButton(
                    onClick = { navController.navigate(Navigation.CreateChallenge.route) },
                    containerColor = ButtonDark,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Create a challenge")
                }
            }
        }
    ) { innerPadding ->
        if (placeholderChallenges.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No challenges yet. Create one or join with a code!",
                    color = AppText.copy(alpha = 0.6f),
                    fontSize = 15.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(placeholderChallenges) { challenge ->
                    ChallengeCard(
                        challenge = challenge,
                        onClick = {
                            navController.navigate(Navigation.ChallengePage.createRoute(challenge.id))
                        }
                    )
                }
            }
        }
    }
}