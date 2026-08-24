package com.course.challengeme.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.android.identity.util.UUID
import com.course.challengeme.data.models.ChallengeModel
import com.course.challengeme.data.models.LeaderboardEntry
import com.course.challengeme.data.models.LeaderboardMode
import com.course.challengeme.data.models.ProofModel
import com.course.challengeme.data.models.TeamBonusModel
import com.course.challengeme.data.models.buildLeaderboardEntries
import com.course.challengeme.data.repos.ChallengeRepo
import com.course.challengeme.data.repos.ProofRepo
import com.course.challengeme.data.repos.UserRepo
import com.course.challengeme.data.repos.UserSummary
import com.course.challengeme.navigations.Navigation
import com.course.challengeme.ui.components.CelebrationScreen
import com.course.challengeme.ui.components.ChallengeFeed
import com.course.challengeme.ui.components.ChallengeHeaderSection
import com.course.challengeme.ui.components.CheckInForm
import com.course.challengeme.ui.theme.AppBackground
import com.course.challengeme.ui.theme.AppText
import com.course.challengeme.ui.theme.Maroon
import com.google.android.gms.location.LocationServices
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Locale

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChallengePage(navController: NavController, challengeId: String?) {
    var challenge by remember { mutableStateOf<ChallengeModel?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var weeklyEntries by remember { mutableStateOf<List<LeaderboardEntry>>(emptyList()) }
    var totalEntries by remember { mutableStateOf<List<LeaderboardEntry>>(emptyList()) }
    var leaderboardMode by remember { mutableStateOf(LeaderboardMode.WEEKLY) }
    var recentUpdates by remember { mutableStateOf<List<ProofModel>>(emptyList()) }
    var memberNames by remember { mutableStateOf<Map<String, UserSummary>>(emptyMap()) }

    val challengeRepository = remember { ChallengeRepo() }
    val userRepository = remember { UserRepo() }
    val updateRepository = remember { ProofRepo() }
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    var checkInText by remember { mutableStateOf("") }
    var attachedPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var attachedLocation by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }
    var submitError by remember { mutableStateOf<String?>(null) }
    var showCheckInHistory by remember { mutableStateOf(false) }
    var dailyBonus by remember { mutableStateOf<TeamBonusModel?>(null) }

    suspend fun loadEverything(id: String) = coroutineScope {
        val challengeDeferred = async { challengeRepository.getChallenge(id) }
        val updatesDeferred = async { updateRepository.getRecentUpdates(id) }
        val weeklyDeferred = async { updateRepository.getWeeklyLeaderboard(id) }
        val bonusDeferred = async { updateRepository.getTeamBonus(id) }

        challengeDeferred.await()
            .onSuccess { c ->
                challenge = c
                val names = userRepository.getUsersByIds(c.memberIds)
                memberNames = names
                totalEntries = buildLeaderboardEntries(c.memberIds, c.memberPoints, names)

                weeklyDeferred.await().onSuccess { weeklyPairs ->
                    weeklyEntries = buildLeaderboardEntries(c.memberIds, weeklyPairs.toMap(), names)
                }

                val isExpired = c.endDate?.let { it < Timestamp.now() } ?: false
                if (isExpired && c.winnerId == null) {
                    challengeRepository.claimWinIfNeeded(id).onSuccess { winnerId ->
                        if (winnerId != null) {
                            challenge = challenge?.copy(winnerId = winnerId)
                        }
                    }
                }
            }
            .onFailure { errorMessage = it.localizedMessage ?: "Couldn't load challenge" }

        updatesDeferred.await().onSuccess { recentUpdates = it }
        bonusDeferred.await().onSuccess { dailyBonus = it }
    }

    LaunchedEffect(challengeId) {
        if (challengeId != null) {
            loadEverything(challengeId)
        } else {
            errorMessage = "No challenge selected"
        }
        isLoading = false
    }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> attachedPhotoUri = uri }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            coroutineScope.launch {
                try {
                    val fusedClient = LocationServices.getFusedLocationProviderClient(context)
                    val location = fusedClient.lastLocation.await()
                    if (location != null) {
                        attachedLocation = location.latitude to location.longitude
                    } else {
                        submitError = "Couldn't get your location. try again"
                    }
                } catch (e: Exception) {
                    submitError = e.localizedMessage ?: "Location error"
                }
            }
        } else {
            submitError = "Location permission needed to attach your location"
        }
    }

    fun submitCheckIn() {
        if (challengeId == null) return
        val myId = currentUserId ?: return

        val text = checkInText.ifBlank { null }
        val photoUri = attachedPhotoUri
        val location = attachedLocation

        var estimatedPoints = ProofRepo.BASE_POINTS
        if (photoUri != null) estimatedPoints += ProofRepo.PHOTO_BONUS
        if (location != null) estimatedPoints += ProofRepo.LOCATION_BONUS

        val tempId = "pending-${UUID.randomUUID()}"
        val optimisticUpdate = ProofModel(
            id = tempId,
            challengeId = challengeId,
            userId = myId,
            textContent = text,
            photoUrl = photoUri?.toString(),
            y = location?.first,
            x = location?.second,
            pointsAwarded = estimatedPoints,
            createdAt = Timestamp.now()
        )

        recentUpdates = listOf(optimisticUpdate) + recentUpdates
        challenge = challenge?.let { c ->
            c.copy(memberPoints = c.memberPoints + (myId to ((c.memberPoints[myId] ?: 0L) + estimatedPoints)))
        }
        checkInText = ""
        attachedPhotoUri = null
        attachedLocation = null

        isSubmitting = true
        coroutineScope.launch {
            updateRepository.submitProof(
                challengeId = challengeId,
                context = context,
                text = text,
                photoUri = photoUri,
                yAxis = location?.first,
                xAxis = location?.second
            ).onSuccess { savedUpdate ->
                recentUpdates = recentUpdates.map { if (it.id == tempId) savedUpdate else it }
                val delta = savedUpdate.pointsAwarded - estimatedPoints
                if (delta != 0L) {
                    challenge = challenge?.let { c ->
                        c.copy(memberPoints = c.memberPoints + (myId to ((c.memberPoints[myId] ?: 0L) + delta)))
                    }
                }
                challengeRepository.getChallenge(challengeId).onSuccess { fresh -> challenge = fresh }
                updateRepository.getTeamBonus(challengeId).onSuccess { bonus -> dailyBonus = bonus }
            }.onFailure {
                recentUpdates = recentUpdates.filterNot { it.id == tempId }
                challenge = challenge?.let { c ->
                    c.copy(memberPoints = c.memberPoints + (myId to ((c.memberPoints[myId] ?: 0L) - estimatedPoints)))
                }
                submitError = it.localizedMessage ?: "Couldn't post update"
            }
            isSubmitting = false
        }
    }

    Box(modifier = Modifier
        .fillMaxSize()
        .background(AppBackground)
        .statusBarsPadding()) {
        when {
            isLoading -> CircularProgressIndicator(color = Maroon, modifier = Modifier.align(Alignment.Center))
            errorMessage != null -> Text(
                errorMessage ?: "",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.align(Alignment.Center)
            )
            challenge != null -> {
                val c = challenge!!
                val dateLabel = c.endDate?.toDate()?.let {
                    SimpleDateFormat("MMM d", Locale.getDefault()).format(it)
                } ?: "no end date"
                val isCompleted = c.endDate?.let { it < Timestamp.now() } ?: false

                if (isCompleted && !showCheckInHistory) {
                    CelebrationScreen(
                        memberNames[c.winnerId]?.name ?: totalEntries.firstOrNull()?.name ?: "",
                        onSeePastCheckIns = { showCheckInHistory = true },
                        onBack = { navController.popBackStack() }
                    )
                    return@Box
                }

                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = AppText)
                        }
                        TextButton(onClick = { navController.navigate(Navigation.Leaderboard.createRoute(c.id)) }) {
                            Text("See full leaderboard", color = Maroon, fontSize = 13.sp)
                        }
                    }

                    LazyColumn(
                        modifier = Modifier.weight(1f).padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        item {
                            val leaderboardEntries = if (leaderboardMode == LeaderboardMode.TOTAL) totalEntries else weeklyEntries
                            val myRank = leaderboardEntries.find { it.userId == currentUserId }?.rank

                            ChallengeHeaderSection(
                                challenge = c,
                                leaderboardEntries = leaderboardEntries,
                                leaderboardMode = leaderboardMode,
                                onModeSelect = { leaderboardMode = it },
                                myRank = myRank,
                                dateLabel = dateLabel,
                                onInviteCodeClick = { navController.navigate(Navigation.InviteCode.createRoute(c.id)) }
                            )
                        }

                        item {
                            ChallengeFeed(
                                recentUpdates = recentUpdates,
                                dailyBonus = dailyBonus,
                                memberNames = memberNames
                            )
                        }

                        item { Spacer(modifier = Modifier.height(12.dp)) }
                    }

                    if (!isCompleted) {
                        CheckInForm(
                            checkInText = checkInText,
                            onCheckInTextChange = { checkInText = it },
                            attachedPhotoUri = attachedPhotoUri,
                            onPickPhoto = { imagePicker.launch("image/*") },
                            onRemovePhoto = { attachedPhotoUri = null },
                            attachedLocation = attachedLocation,
                            onRequestLocation = { locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) },
                            isSubmitting = isSubmitting,
                            submitError = submitError,
                            onSubmit = { submitCheckIn() }
                        )
                    }
                }
            }
        }
    }
}