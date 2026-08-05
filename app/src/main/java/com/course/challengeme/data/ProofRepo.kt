package com.course.challengeme.data

import android.content.Context
import android.net.Uri
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.UUID

class ProofRepo {
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val auth = FirebaseAuth.getInstance()

    companion object {
        const val BASE_POINTS = 10L
        const val PROOF_BONUS = 5L // extra points for photo or location proof
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
        val snapshot = db.collection("updates")
            .whereEqualTo("challengeId", challengeId)
            .whereEqualTo("userId", userId)
            .whereGreaterThanOrEqualTo("createdAt", startOfTodayTimestamp())
            .limit(1)
            .get()
            .await()
        return !snapshot.isEmpty
    }

    private suspend fun awardPointsAndSave(
        challengeId: String,
        userId: String,
        type: String,
        points: Long,
        textContent: String? = null,
        photoUrl: String? = null,
        x: Double? = null ,
        y: Double? = null,
    ): Result<Unit> {
        return try {
            val update = ProofModel(
                challengeId = challengeId,
                userId = userId,
                type = type,
                textContent = textContent,
                photoUrl = photoUrl,
                x = x,
                y = y,
                pointsAwarded = points,
                createdAt = Timestamp.now()
            )
            db.collection("updates").add(update).await()

            db.collection("challenges").document(challengeId)
                .update("memberPoints.$userId", FieldValue.increment(points))
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun submitTextUpdate(challengeId: String, text: String): Result<Unit> {
        val userId = auth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("Not logged in"))
        if (hasSubmittedToday(challengeId, userId)) {
            return Result.failure(IllegalStateException("Already submitted today"))
        }
        return awardPointsAndSave(challengeId, userId, "text", BASE_POINTS, textContent = text)
    }

    suspend fun submitPhotoUpdate(challengeId: String, context: Context, imageUri: Uri): Result<Unit> {
        val userId = auth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("Not logged in"))
        if (hasSubmittedToday(challengeId, userId)) {
            return Result.failure(IllegalStateException("Already submitted today"))
        }
        return try {
            val ref = storage.reference.child("update_photos/$challengeId/$userId/${UUID.randomUUID()}.jpg")
            ref.putFile(imageUri).await()
            val downloadUrl = ref.downloadUrl.await().toString()

            awardPointsAndSave(
                challengeId, userId, "photo", BASE_POINTS + PROOF_BONUS, photoUrl = downloadUrl
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun submitLocationUpdate(challengeId: String, xAxis: Double, yAxis: Double): Result<Unit> {
        val userId = auth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("Not logged in"))
        if (hasSubmittedToday(challengeId, userId)) {
            return Result.failure(IllegalStateException("Already submitted today"))
        }
        return awardPointsAndSave(
            challengeId, userId, "location", BASE_POINTS + PROOF_BONUS,
            x = xAxis, y = yAxis,
        )
    }
}