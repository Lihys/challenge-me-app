package com.course.challengeme.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.course.challengeme.data.ChallengeRepo
import com.course.challengeme.navigations.Navigation
import com.course.challengeme.ui.components.TextField
import com.course.challengeme.ui.components.PrimaryButton
import com.course.challengeme.ui.theme.AppBackground
import com.course.challengeme.ui.theme.AppText
import com.course.challengeme.ui.theme.ButtonDark
import com.google.firebase.Timestamp
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewChallengeScreen(navController: NavController) {
    var title by remember { mutableStateOf("") }
    var prize by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState()
    val selectedDateMillis = datePickerState.selectedDateMillis

    val ChallengeCreationService = remember { ChallengeRepo() }
    val coroutineScope = rememberCoroutineScope()

    val dateLabel = selectedDateMillis?.let {
        SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(it)
    } ?: "Select an end date"

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
            Text("New Challenge", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = AppText)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Set the goal, the deadline, and what the prize is",
                fontSize = 14.sp,
                color = AppText.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(32.dp))

            TextField(value = title, onValueChange = {
                title = it
            }, label = "Challenge name")
            Spacer(modifier = Modifier.height(12.dp))
            TextField(value = prize, onValueChange = { prize = it }, label = "Prize (optional)")

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(dateLabel, color = AppText)
            }

            errorMessage?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))

            PrimaryButton(
                text = "Create Challenge",
                isLoading = isLoading,
                enabled = title.isNotBlank() && selectedDateMillis != null,
                onClick = {
                    val endDate = selectedDateMillis?.let { Timestamp(it / 1000, 0) }
                    if (endDate == null) {
                        errorMessage = "Please select an end date"
                        return@PrimaryButton
                    }
                    coroutineScope.launch {
                        isLoading = true
                        errorMessage = null
                        ChallengeCreationService.createChallenge(
                            title = title,
                            prize = prize.ifBlank { null },
                            endDate = endDate
                        ).onSuccess { challengeId ->
                            navController.navigate(Navigation.InviteCode.createRoute(challengeId)) {
                                popUpTo(Navigation.Home.route)
                            }
                        }.onFailure {
                            errorMessage = it.localizedMessage ?: "Couldn't create challenge"
                        }
                        isLoading = false
                    }
                }
            )
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("OK", color = ButtonDark)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel", color = AppText)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}