package com.course.challengeme.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.android.identity.util.UUID
import com.course.challengeme.data.ChallengeModel
import com.course.challengeme.data.ChallengeRepo
import com.course.challengeme.data.LeaderboardEntry
import com.course.challengeme.data.LeaderboardMode
import com.course.challengeme.data.ProofModel
import com.course.challengeme.data.ProofRepo
import com.course.challengeme.data.TeamBonusModel
import com.course.challengeme.data.UserRepo
import com.course.challengeme.data.buildLeaderboardEntries
import com.course.challengeme.navigations.Navigation
import com.course.challengeme.ui.components.CelebrationScreen
import com.course.challengeme.ui.components.CheckIn
import com.course.challengeme.ui.components.LeaderboardPodium
import com.course.challengeme.ui.components.LeaderboardToggle
import com.course.challengeme.ui.components.TeamBonusBanner
import com.course.challengeme.ui.components.TextField
import com.course.challengeme.ui.theme.AppBackground
import com.course.challengeme.ui.theme.AppText
import com.course.challengeme.ui.theme.BlueLauncherBg
import com.course.challengeme.ui.theme.ButtonDark
import com.google.android.gms.location.LocationServices
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Locale

// Merges real check-ins and the (at most one) live team-bonus row into a
// single sorted feed. The bonus row's sortSeconds comes from its own
// updatedAt, which is bumped every time it changes, so it naturally slides
// to sit above the newest check-in without any check-in needing edits.
private sealed class FeedItem {
    abstract val sortSeconds: Long
    data class Checkin(val proof: ProofModel) : FeedItem() {
        override val sortSeconds get() = proof.createdAt?.seconds ?: 0L
    }
    data class Bonus(val bonus: TeamBonusModel) : FeedItem() {
        override val sortSeconds get() = bonus.updatedAt?.seconds ?: 0L
    }
}

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
    var memberNames by remember { mutableStateOf<Map<String, com.course.challengeme.data.UserSummary>>(emptyMap()) }

    val challengeRepository = remember { ChallengeRepo() }
    val userRepository = remember { UserRepo() }
    val updateRepository = remember { ProofRepo() }
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // Composer state-to check
    var checkInText by remember { mutableStateOf("") }
    var attachedPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var attachedLocation by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }
    var submitError by remember { mutableStateOf<String?>(null) }
    var showCheckInHistory by remember { mutableStateOf(false) }
    var isDescriptionExpanded by remember { mutableStateOf(false) }

    var isDescriptionCutOff by remember { mutableStateOf(false) }
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

    Box(modifier = Modifier
        .fillMaxSize()
        .background(AppBackground)
        .statusBarsPadding()) {
        when {
            isLoading -> CircularProgressIndicator(color = ButtonDark, modifier = Modifier.align(Alignment.Center))
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
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = AppText)
                        }
                        TextButton(onClick = { navController.navigate(Navigation.Leaderboard.createRoute(c.id)) }) {
                            Text("See full leaderboard", color = ButtonDark, fontSize = 13.sp)
                        }
                    }

                    LazyColumn(
                        modifier = Modifier.weight(1f).padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        item {
                            val leaderboardEntries = if (leaderboardMode == LeaderboardMode.TOTAL) totalEntries else weeklyEntries
                            val myRank = leaderboardEntries.find { it.userId == currentUserId }?.rank

                            LeaderboardPodium(topThree = leaderboardEntries.take(3))
                            Spacer(modifier = Modifier.height(12.dp))
                            LeaderboardToggle(
                                selected = leaderboardMode,
                                onSelect = { leaderboardMode = it }
                            )
                            Spacer(modifier = Modifier.height(20.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    c.title,
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BlueLauncherBg,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                TextButton(
                                    onClick = { navController.navigate(Navigation.InviteCode.createRoute(c.id)) },
                                    colors = ButtonDefaults.textButtonColors(
                                        containerColor = BlueLauncherBg,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(24.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.ConfirmationNumber,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Invite code", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = buildString {
                                    myRank?.let {
                                        val label = if (leaderboardMode == LeaderboardMode.TOTAL) "overall" else "this week"
                                        append("You're #$it $label | ")
                                    }
                                    append("${c.memberIds.size} members | ends $dateLabel")
                                },
                                fontSize = 12.sp,
                                color = AppText.copy(alpha = 0.6f)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    c.description?.takeIf { it.isNotBlank() }?.let { desc ->
                                        Text(
                                            text = desc,
                                            fontSize = 13.sp,
                                            color = AppText.copy(alpha = 0.7f),
                                            maxLines = if (isDescriptionExpanded) Int.MAX_VALUE else 2,
                                            overflow = TextOverflow.Ellipsis,
                                            onTextLayout = { result ->
                                                if (!isDescriptionExpanded) {
                                                    isDescriptionCutOff = result.hasVisualOverflow
                                                }
                                            }
                                        )
                                        if (isDescriptionCutOff || isDescriptionExpanded) {
                                            Text(
                                                text = if (isDescriptionExpanded) "Hide description" else "See description",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = ButtonDark,
                                                modifier = Modifier
                                                    .padding(top = 2.dp)
                                                    .clickable { isDescriptionExpanded = !isDescriptionExpanded }
                                            )
                                        }
                                    }
                                }

                                c.prize?.takeIf { it.isNotBlank() }?.let { prize ->
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(CircleShape)
                                            .background(BlueLauncherBg.copy(alpha = 0.1f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = "Prize:",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = BlueLauncherBg,
                                                textAlign = TextAlign.Center
                                            )
                                            Text(
                                                text = prize,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = BlueLauncherBg,
                                                textAlign = TextAlign.Center,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))
                            Text("Check-ins", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AppText)
                            Spacer(modifier = Modifier.height(12.dp))
                            //-----

                        }

                        item {
                            val feedItems = remember(recentUpdates, dailyBonus) {
                                val items = mutableListOf<FeedItem>()
                                items += recentUpdates.map { FeedItem.Checkin(it) }
                                dailyBonus?.takeIf { it.checkedInMemberIds.size >= 2 }?.let { items += FeedItem.Bonus(it) }
                                items.sortedWith(
                                    compareByDescending<FeedItem> { it.sortSeconds }
                                        .thenByDescending { it is FeedItem.Bonus } // ties go to the bonus banner
                                )
                            }

                            Column {
                                feedItems.forEach { item ->
                                    when (item) {
                                        is FeedItem.Checkin -> CheckIn(
                                            update = item.proof,
                                            memberName = memberNames[item.proof.userId]?.name ?: "Unknown",
                                            avatarUrl = memberNames[item.proof.userId]?.photoUrl
                                        )
                                        is FeedItem.Bonus -> TeamBonusBanner(
                                            memberCount = item.bonus.checkedInMemberIds.size,
                                            bonusPoints = ProofRepo.TEAM_BONUS
                                        )
                                    }
                                }
                            }
                        }

                        item { Spacer(modifier = Modifier.height(12.dp)) }
                    }

                    if (!isCompleted) {
                        // Inline composer
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(AppBackground)
                                .padding(16.dp)
                        ) {
                            submitError?.let {
                                Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                                Spacer(modifier = Modifier.height(6.dp))
                            }

                            TextField(
                                value = checkInText,
                                onValueChange = { checkInText = it },
                                label = "I did the challenge today..."
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = false,
                                    onClick = {},
                                    label = { Text("text +10", fontSize = 12.sp) }
                                )
                                FilterChip(
                                    selected = attachedPhotoUri != null,
                                    onClick = { imagePicker.launch("image/*") },
                                    label = { Text(if (attachedPhotoUri != null) "Photo ✓" else "Photo +5", fontSize = 12.sp) },
                                    leadingIcon = { Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                )
                                FilterChip(
                                    selected = attachedLocation != null,
                                    onClick = { locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) },
                                    label = { Text(if (attachedLocation != null) "GPS ✓" else "GPS +5", fontSize = 12.sp) },
                                    leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                )
                            }

                            attachedPhotoUri?.let { uri ->
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    AsyncImage(
                                        model = uri,
                                        contentDescription = "Attached photo",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    TextButton(onClick = { attachedPhotoUri = null }) {
                                        Text("Remove", color = ButtonDark, fontSize = 12.sp)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = {
                                    if (challengeId == null) return@Button
                                    val myId = currentUserId ?: return@Button

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
                                        photoUrl = photoUri?.toString(), // deals with local content directly
                                        y = location?.first,
                                        x = location?.second,
                                        pointsAwarded = estimatedPoints,
                                        createdAt = Timestamp.now()
                                    )

                                    // Show it without waiting
                                    recentUpdates = listOf(optimisticUpdate) + recentUpdates
                                    challenge = challenge?.let { c ->
                                        c.copy(memberPoints = c.memberPoints + (myId to ((c.memberPoints[myId] ?: 0L) + estimatedPoints)))
                                    }
                                    checkInText = ""
                                    attachedPhotoUri = null
                                    attachedLocation = null

                                    // write fr in the background, but also add the look locally so it will actually appear idk how to fix
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

                                            // Pick up the team bonus (and any credit to other members) without a full reload
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
                                },
                                enabled = !isSubmitting,
                                colors = ButtonDefaults.buttonColors(containerColor = BlueLauncherBg),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(if (isSubmitting) "Posting..." else "Post update")
                            }
                        }
                    }
                }
            }
        }
    }
}