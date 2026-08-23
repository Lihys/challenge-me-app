package com.course.challengeme.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Logout
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
import com.course.challengeme.data.ProofRepo
import com.course.challengeme.data.UserRepo
import com.course.challengeme.navigations.Navigation
import com.course.challengeme.ui.components.MemberAvatar
import com.course.challengeme.ui.components.TextField
import com.course.challengeme.ui.theme.AppBackground
import com.course.challengeme.ui.theme.AppText
import com.course.challengeme.ui.theme.ButtonDark
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

private data class AccountStats(
    val totalPoints: Long = 0,
    val checkIns: Int = 0,
    val challenges: Int = 0,
    val challengesWon: Long = 0
)

@Composable
fun MyAccount(navController: NavController) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var photoUrl by remember { mutableStateOf<String?>(null) }
    var stats by remember { mutableStateOf(AccountStats()) }
    var isLoading by remember { mutableStateOf(true) }

    var isEditing by remember { mutableStateOf(false) }
    var editedName by remember { mutableStateOf("") }
    var pendingPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }

    val userRepository = remember { UserRepo() }
    val challengeRepository = remember { ChallengeRepo() }
    val proofRepository = remember { ProofRepo() }
    val auth = FirebaseAuth.getInstance()
    val userId = auth.currentUser?.uid
    val coroutineScope = rememberCoroutineScope()

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> if (uri != null) pendingPhotoUri = uri }

    suspend fun loadProfile() {
        if (userId == null) return
        userRepository.getUserProfile(userId).onSuccess { (n, e, p) ->
            name = n
            email = e
            photoUrl = p
        }
    }

    LaunchedEffect(userId) {
        if (userId != null) {
            coroutineScope {
                val profileDeferred = async { loadProfile() }
                val challengesDeferred = async { challengeRepository.getChallengesForUser(userId) }
                val checkInCountDeferred = async { proofRepository.getUpdateCountForUser(userId) }
                val winsDeferred = async { userRepository.getWinsCount(userId) }

                profileDeferred.await()

                val challenges = challengesDeferred.await()
                val checkInCount = checkInCountDeferred.await()
                val wins = winsDeferred.await()

                challenges.onSuccess { list ->
                    val totalPoints = list.sumOf { it.memberPoints[userId] ?: 0L }
                    stats = AccountStats(
                        totalPoints = totalPoints,
                        checkIns = checkInCount.getOrDefault(0),
                        challenges = list.size,
                        challengesWon = wins.getOrDefault(0L)
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
                // Avatar, name and email (can edit)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box {
                        MemberAvatar(
                            name = name,
                            photoUrl = pendingPhotoUri?.toString() ?: photoUrl,
                            size = 64.dp
                        )
                        if (isEditing) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.35f))
                                    .clickable { imagePicker.launch("image/*") },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = "Change photo", tint = Color.White)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        if (isEditing) {
                            TextField(value = editedName, onValueChange = { editedName = it }, label = "Name")
                        } else {
                            Text(name, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AppText)
                        }
                        Text(email, fontSize = 13.sp, color = AppText.copy(alpha = 0.6f))
                    }

                    if (isEditing) {
                        IconButton(
                            onClick = {
                                if (userId == null) return@IconButton
                                val trimmed = editedName.trim()
                                if (trimmed.isBlank()) {
                                    saveError = "Name can't be empty"
                                    return@IconButton
                                }
                                coroutineScope.launch {
                                    isSaving = true
                                    saveError = null
                                    userRepository.updateProfile(userId, trimmed, pendingPhotoUri)
                                        .onSuccess {
                                            loadProfile()
                                            pendingPhotoUri = null
                                            isEditing = false
                                        }
                                        .onFailure {
                                            saveError = it.localizedMessage ?: "Couldn't save changes"
                                        }
                                    isSaving = false
                                }
                            },
                            enabled = !isSaving
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(color = ButtonDark, modifier = Modifier.size(20.dp))
                            } else {
                                Icon(Icons.Default.Check, contentDescription = "Save", tint = ButtonDark)
                            }
                        }
                        IconButton(
                            onClick = {
                                isEditing = false
                                pendingPhotoUri = null
                                saveError = null
                            },
                            enabled = !isSaving
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel", tint = AppText)
                        }
                    } else {
                        IconButton(onClick = {
                            editedName = name
                            isEditing = true
                        }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit profile", tint = AppText)
                        }
                    }
                }

                saveError?.let {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
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
                    StatCard("Challenges Won", stats.challengesWon.toString(), Modifier.weight(1f))
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