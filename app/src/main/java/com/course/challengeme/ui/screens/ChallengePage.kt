package com.course.challengeme.ui.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.course.challengeme.data.ProofRepo
import com.course.challengeme.ui.theme.AppText
import com.course.challengeme.ui.theme.ButtonDark
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubmitUpdateSheet(
    challengeId: String,
    onDismiss: () -> Unit,
    onSubmitted: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val updateRepository = remember { ProofRepo() }

    var mode by remember { mutableStateOf<String?>(null) } // "text" | null (choosing)
    var textInput by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                isSubmitting = true
                updateRepository.submitPhotoUpdate(challengeId, context, uri)
                    .onSuccess { onSubmitted() }
                    .onFailure { errorMessage = it.localizedMessage ?: "Couldn't submit" }
                isSubmitting = false
            }
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val fusedClient = LocationServices.getFusedLocationProviderClient(context)
            coroutineScope.launch {
                isSubmitting = true
                try {
                    val location = fusedClient.lastLocation.await()
                    if (location != null) {
                        updateRepository.submitLocationUpdate(challengeId, location.latitude, location.longitude)
                            .onSuccess { onSubmitted() }
                            .onFailure { errorMessage = it.localizedMessage ?: "Couldn't submit" }
                    } else {
                        errorMessage = "Couldn't get your location — try again"
                    }
                } catch (e: Exception) {
                    errorMessage = e.localizedMessage ?: "Location error"
                }
                isSubmitting = false
            }
        } else {
            errorMessage = "Location permission is needed for this proof type"
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text("Submit Today's Update", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AppText)
            Spacer(modifier = Modifier.height(16.dp))

            errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (mode == "text") {
                TextField(value = textInput, onValueChange = { textInput = it }, label = "What did you do?")
                Spacer(modifier = Modifier.height(12.dp))
                PrimaryButton(
                    text = "Submit",
                    isLoading = isSubmitting,
                    enabled = textInput.isNotBlank(),
                    onClick = {
                        coroutineScope.launch {
                            isSubmitting = true
                            updateRepository.submitTextUpdate(challengeId, textInput)
                                .onSuccess { onSubmitted() }
                                .onFailure { errorMessage = it.localizedMessage ?: "Couldn't submit" }
                            isSubmitting = false
                        }
                    }
                )
            } else {
                OutlinedButton(onClick = { mode = "text" }, modifier = Modifier.fillMaxWidth()) {
                    Text("Text update", color = AppText)
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { imagePicker.launch("image/*") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Attach a photo (+${ProofRepo.PROOF_BONUS} pts)", color = AppText)
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = {
                        val hasPermission = ContextCompat.checkSelfPermission(
                            context, Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                        if (hasPermission) {
                            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                        } else {
                            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Share location (+${ProofRepo.PROOF_BONUS} pts)", color = AppText)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}