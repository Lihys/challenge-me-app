package com.course.challengeme.ui.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.course.challengeme.ui.theme.AppBackground
import com.course.challengeme.ui.theme.MyBlue
import com.course.challengeme.ui.theme.Maroon

@Composable
fun CheckInForm(
    checkInText: String,
    onCheckInTextChange: (String) -> Unit,
    attachedPhotoUri: Uri?,
    onPickPhoto: () -> Unit,
    onRemovePhoto: () -> Unit,
    attachedLocation: Pair<Double, Double>?,
    onRequestLocation: () -> Unit,
    isSubmitting: Boolean,
    submitError: String?,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
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
            onValueChange = onCheckInTextChange,
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
                onClick = onPickPhoto,
                label = { Text(if (attachedPhotoUri != null) "Photo ✓" else "Photo +5", fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(14.dp)) }
            )
            FilterChip(
                selected = attachedLocation != null,
                onClick = onRequestLocation,
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
                TextButton(onClick = onRemovePhoto) {
                    Text("Remove", color = Maroon, fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onSubmit,
            enabled = !isSubmitting,
            colors = ButtonDefaults.buttonColors(containerColor = MyBlue),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isSubmitting) "Posting..." else "Post update")
        }
    }
}