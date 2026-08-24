package com.course.challengeme.ui.components

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.course.challengeme.data.ProofModel
import com.course.challengeme.ui.theme.AppBackground
import com.course.challengeme.ui.theme.AppText
import com.course.challengeme.ui.theme.ButtonDark
import com.course.challengeme.ui.theme.ChallengeBgTan
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import androidx.compose.foundation.clickable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage

@Composable
fun CheckIn(
    update: ProofModel,
    memberName: String,
    avatarUrl: String? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val date = update.createdAt?.toDate()
    val today = Calendar.getInstance()
    val updateDay = Calendar.getInstance().apply { date?.let { time = it } }
    val isToday = date != null &&
            today.get(Calendar.DAY_OF_YEAR) == updateDay.get(Calendar.DAY_OF_YEAR) &&
            today.get(Calendar.YEAR) == updateDay.get(Calendar.YEAR)

    val timeLabel = date?.let {
        val prefix = if (isToday) "Today" else SimpleDateFormat("EEE", Locale.getDefault()).format(it)
        val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(it)
        "$prefix $time"
    } ?: ""

    Column(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(ChallengeBgTan.copy(alpha = 0.18f))
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MemberAvatar(name = memberName, photoUrl = avatarUrl, size = 32.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(memberName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = AppText)
                        Text(timeLabel, fontSize = 12.sp, color = AppText.copy(alpha = 0.5f))
                    }
                }
                Text(
                    "+${update.pointsAwarded}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = ButtonDark
                )
            }

            update.textContent?.let { comment ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "\u201C$comment\u201D",
                    fontSize = 14.sp,
                    fontStyle = FontStyle.Italic,
                    color = AppText.copy(alpha = 0.85f)
                )
            }

            if (update.photoUrl != null || (update.y != null && update.x != null)) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    update.photoUrl?.let { url ->
                        var expanded by remember { mutableStateOf(false) }
                        AsyncImage(
                            model = url,
                            contentDescription = "Check-in photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .weight(1f)
                                .height(if (expanded) 260.dp else 140.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { expanded = !expanded }
                        )
                    }

                    if (update.y != null && update.x != null) {
                        val lat = update.y
                        val lng = update.x
                        val displayName = update.locationName ?: "Location shared"

                        AssistChip(
                            onClick = { openInMaps(context, lat, lng, displayName) },
                            label = {
                                Text(
                                    displayName,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(14.dp)) },
                            colors = AssistChipDefaults.assistChipColors(containerColor = AppBackground),
                            modifier = Modifier.widthIn(max = 170.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(color = AppText.copy(alpha = 0.08f))
    }
}

/**
 * letter of their name when no photo
 */
@Composable
fun MemberAvatar(name: String, photoUrl: String?, size: androidx.compose.ui.unit.Dp) {
    if (photoUrl != null) {
        AsyncImage(
            model = photoUrl,
            contentDescription = "Profile photo",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
        )
    } else {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(ChallengeBgTan),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = name.firstOrNull()?.uppercase() ?: "?",
                color = AppBackground,
                fontWeight = FontWeight.Bold,
                fontSize = (size.value * 0.4f).sp
            )
        }
    }
}

/**
 * Opens the location on a maps app if installed ofc
 * if it doesnt, it opens on browser via google maps
 */
private fun openInMaps(context: android.content.Context, lat: Double, lng: Double, label: String) {
    val geoUri = Uri.parse("geo:$lat,$lng?q=$lat,$lng(${Uri.encode(label)})")
    val geoIntent = Intent(Intent.ACTION_VIEW, geoUri)
    try {
        context.startActivity(geoIntent)
    } catch (e: ActivityNotFoundException) {
        val webUri = Uri.parse("https://maps.google.com/?q=$lat,$lng")
        context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
    }
}