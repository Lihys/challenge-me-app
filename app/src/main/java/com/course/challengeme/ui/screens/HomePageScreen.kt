package com.course.challengeme.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.course.challengeme.data.Challenge
import com.course.challengeme.data.ChallengeRepo
import com.course.challengeme.data.UserRepo
import com.course.challengeme.navigations.Navigation
import com.course.challengeme.ui.components.ChallengeCard
import com.course.challengeme.ui.components.MemberAvatar
import com.course.challengeme.ui.theme.AppBackground
import com.course.challengeme.ui.theme.AppText
import com.course.challengeme.ui.theme.ButtonDark
import com.course.challengeme.ui.theme.ChallengeBgMauve
import com.course.challengeme.ui.theme.ChallengeBgRed
import com.course.challengeme.ui.theme.ChallengeBgTan
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomePageScreen(navController: NavController) {
    var challenges by remember { mutableStateOf<List<Challenge>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var myName by remember { mutableStateOf("") }
    var myPhotoUrl by remember { mutableStateOf<String?>(null) }

    val challengeRepository = remember { ChallengeRepo() }
    val userRepository = remember { UserRepo() }
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    val cardColors = listOf(ChallengeBgRed, ChallengeBgTan, ChallengeBgMauve)

    LaunchedEffect(currentUserId) {
        if (currentUserId != null) {
            userRepository.getUserProfile(currentUserId).onSuccess { (n, _, p) ->
                myName = n
                myPhotoUrl = p
            }

            challengeRepository.getChallengesForUser(currentUserId)
                .onSuccess { models ->
                    challenges = models.mapIndexed { index, model ->
                        Challenge(
                            id = model.id,
                            title = model.title,
                            memberCount = model.memberIds.size,
                            myPoints = (model.memberPoints[currentUserId] ?: 0L).toInt(),
                            cardColor = cardColors[index % cardColors.size]
                        )
                    }
                }
                .onFailure { errorMessage = it.localizedMessage ?: "Couldn't load challenges" }
        } else {
            errorMessage = "Not logged in"
        }
        isLoading = false
    }

    Scaffold(
        containerColor = AppBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text("Your Challenges", color = AppText, fontWeight = FontWeight.Bold)
                },
                actions = {
                    IconButton(onClick = { navController.navigate(Navigation.MyAccount.route) }) {
                        MemberAvatar(name = myName, photoUrl = myPhotoUrl, size = 32.dp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            when {
                isLoading -> CircularProgressIndicator(color = ButtonDark)
                errorMessage != null -> Text(
                    text = errorMessage ?: "",
                    color = MaterialTheme.colorScheme.error
                )
                challenges.isEmpty() -> Text(
                    text = "No challenges yet. Create one or join with a code!",
                    color = AppText.copy(alpha = 0.6f),
                    fontSize = 15.sp
                )
                else -> LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    items(challenges) { challenge ->
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
}