package com.course.challengeme.data

import android.content.Context
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
import java.util.UUID

class ProofRepo {
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val auth = FirebaseAuth.getInstance()

    companion object {
        const val TEXT_POINTS = 10L
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
     * Submits one check-in with any combination of text / photo / location
     * Points = 10 (text) + 5 (photo) + 5 (location), plus a same-day team bonus!!
     */
    suspend fun submitProof(
        challengeId: String,
        context: Context,
        text: String?,
        photoUri: Uri?,
        yAxis: Double?,
        xAxis: Double?
    ): Result<Long> {
        val userId = auth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("Not logged in"))

        if (hasSubmittedToday(challengeId, userId)) {
            return Result.failure(IllegalStateException("You already checked in today"))
        }

        val hasText = !text.isNullOrBlank()
        val hasPhoto = photoUri != null
        val hasLocation = yAxis != null && xAxis != null

        if (!hasText && !hasPhoto && !hasLocation) {
            return Result.failure(IllegalStateException("Add some text, a photo, or your location"))
        }

        return try {
            var photoUrl: String? = null
            if (hasPhoto) {
                val ref = storage.reference.child("update_photos/$challengeId/$userId/${UUID.randomUUID()}.jpg")
                ref.putFile(photoUri!!).await()
                photoUrl = ref.downloadUrl.await().toString()
            }

            var points = 0L
            if (hasText) points += TEXT_POINTS
            if (hasPhoto) points += PHOTO_BONUS
            if (hasLocation) points += LOCATION_BONUS

            // Team bonus check — distinct members who already checked in today, before this one
            val todaySnapshot = db.collection("updates")
                .whereEqualTo("challengeId", challengeId)
                .whereGreaterThanOrEqualTo("createdAt", startOfTodayTimestamp())
                .get()
                .await()
            val distinctTodayUserIds = todaySnapshot.documents
                .mapNotNull { it.getString("userId") }
                .toSet()
            val teamBonusApplies = distinctTodayUserIds.isNotEmpty() // someone else already checked in today so a bonus
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
                pointsAwarded = points,
                createdAt = Timestamp.now()
            )
            db.collection("updates").add(update).await()

            db.collection("challenges").document(challengeId)
                .update("memberPoints.$userId", FieldValue.increment(points))
                .await()

            Result.success(points)
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
}