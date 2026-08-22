package com.course.challengeme.data

import android.content.Context
import android.location.Geocoder
import android.net.Uri
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

class ProofRepo {
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val auth = FirebaseAuth.getInstance()

    companion object {
        const val BASE_POINTS = 10L
        const val PHOTO_BONUS = 5L
        const val LOCATION_BONUS = 5L
        const val TEAM_BONUS = 5L
    }

    private fun startOfTodayTimestamp(): Timestamp {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return Timestamp(calendar.time)
    }

    /**
     * showing a location name and not coordinates
     */
    private suspend fun resolveLocationName(context: Context, lat: Double, lng: Double): String? =
        withContext(Dispatchers.IO) {
            try {
                if (!Geocoder.isPresent()) return@withContext null
                @Suppress("DEPRECATION")
                val addresses = Geocoder(context, Locale.getDefault()).getFromLocation(lat, lng, 1)
                val address = addresses?.firstOrNull() ?: return@withContext null
                listOfNotNull(
                    address.subLocality ?: address.thoroughfare,
                    address.locality
                ).joinToString(", ").ifBlank { null }
            } catch (e: Exception) {
                null
            }
        }

    suspend fun hasSubmittedToday(challengeId: String, userId: String): Boolean {
        return try {
            val snapshot = db.collection("updates")
                .whereEqualTo("challengeId", challengeId)
                .whereEqualTo("userId", userId)
                .whereGreaterThanOrEqualTo("createdAt", startOfTodayTimestamp())
                .limit(1)
                .get()
                .await()
            !snapshot.isEmpty
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getRecentUpdates(challengeId: String, limit: Long = 20): Result<List<ProofModel>> {
        return try {
            val snapshot = db.collection("updates")
                .whereEqualTo("challengeId", challengeId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .await()
            Result.success(snapshot.documents.mapNotNull { it.toObject(ProofModel::class.java) })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Submitting a proof with text/photo/ location
     * Points = 10 (base: any check-in) + 5 (photo) + 5 (location),
     * plus a +5 team bonus when 2 or more members checked in in a day
     */
    suspend fun submitProof(
        challengeId: String,
        context: Context,
        text: String?,
        photoUri: Uri?,
        yAxis: Double?,
        xAxis: Double?
    ): Result<ProofModel> {
        val userId = auth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("Not logged in"))

        val hasText = !text.isNullOrBlank()
        val hasPhoto = photoUri != null
        val hasLocation = yAxis != null && xAxis != null

        if (!hasText && !hasPhoto && !hasLocation) {
            return Result.failure(IllegalStateException("Add some text, a photo, or your location"))
        }

        return try {
            coroutineScope {
                // Photo upload, team-bonus check, and location-name
                val photoUrlDeferred = async {
                    if (hasPhoto) {
                        val ref = storage.reference.child("update_photos/$challengeId/$userId/${UUID.randomUUID()}.jpg")
                        ref.putFile(photoUri!!).await()
                        ref.downloadUrl.await().toString()
                    } else null
                }
                val teamBonusDeferred = async {
                    val todaySnapshot = db.collection("updates")
                        .whereEqualTo("challengeId", challengeId)
                        .whereGreaterThanOrEqualTo("createdAt", startOfTodayTimestamp())
                        .get()
                        .await()
                    val checkedInToday = todaySnapshot.documents.mapNotNull { it.getString("userId") }.toMutableSet()
                    checkedInToday.add(userId) // this submission counts toward today too
                    checkedInToday.size >= 2
                }
                val locationNameDeferred = async {
                    if (hasLocation) resolveLocationName(context, yAxis!!, xAxis!!) else null
                }

                val photoUrl = photoUrlDeferred.await()
                val teamBonusApplies = teamBonusDeferred.await()
                val locationName = locationNameDeferred.await()

                var points = BASE_POINTS
                if (hasPhoto) points += PHOTO_BONUS
                if (hasLocation) points += LOCATION_BONUS
                if (teamBonusApplies) points += TEAM_BONUS

                val update = ProofModel(
                    challengeId = challengeId,
                    userId = userId,
                    type = listOfNotNull(
                        if (hasText) "text" else null,
                        if (hasPhoto) "photo" else null,
                        if (hasLocation) "location" else null
                    ).joinToString(","),
                    textContent = text,
                    photoUrl = photoUrl,
                    y = yAxis,
                    x = xAxis,
                    locationName = locationName,
                    pointsAwarded = points,
                    createdAt = Timestamp.now()
                )

                // Creating the proof document and incrementing points
                val addDeferred = async { db.collection("updates").add(update).await() }
                val pointsUpdateDeferred = async {
                    db.collection("challenges").document(challengeId)
                        .update("memberPoints.$userId", FieldValue.increment(points))
                        .await()
                }

                val docRef = addDeferred.await()
                pointsUpdateDeferred.await()

                Result.success(update.copy(id = docRef.id))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getWeeklyLeaderboard(challengeId: String): Result<List<Pair<String, Long>>> {
        return try {
            val sevenDaysAgo = Timestamp(Date(System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000))
            val snapshot = db.collection("updates")
                .whereEqualTo("challengeId", challengeId)
                .whereGreaterThanOrEqualTo("createdAt", sevenDaysAgo)
                .get()
                .await()
            val updates = snapshot.documents.mapNotNull { it.toObject(ProofModel::class.java) }
            val totals = updates.groupBy { it.userId }
                .mapValues { (_, ups) -> ups.sumOf { it.pointsAwarded } }
            Result.success(totals.entries.sortedByDescending { it.value }.map { it.key to it.value })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUpdateCountForUser(userId: String): Result<Int> {
        return try {
            val snapshot = db.collection("updates")
                .whereEqualTo("userId", userId)
                .get()
                .await()
            Result.success(snapshot.size())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


}