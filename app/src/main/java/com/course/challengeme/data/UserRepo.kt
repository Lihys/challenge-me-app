package com.course.challengeme.data

import android.net.Uri
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.UUID

data class UserSummary(
    val name: String = "Unknown",
    val photoUrl: String? = null
)

class UserRepo {
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    suspend fun saveUserProfile(userId: String, name: String, email: String) {
        db.collection("users").document(userId)
            .set(mapOf("name" to name, "email" to email))
            .await()
    }

    suspend fun getUsersByIds(userIds: List<String>): Map<String, UserSummary> {
        if (userIds.isEmpty()) return emptyMap()
        val result = mutableMapOf<String, UserSummary>()
        // Firestore supports 10 values per send max, so we group for larger numbers
        userIds.chunked(10).forEach { chunk ->
            val snapshot = db.collection("users")
                .whereIn(FieldPath.documentId(), chunk)
                .get()
                .await()
            snapshot.documents.forEach { doc ->
                result[doc.id] = UserSummary(
                    name = doc.getString("name") ?: "Unknown",
                    photoUrl = doc.getString("photoUrl")
                )
            }
        }
        return result
    }

    suspend fun getUserProfile(userId: String): Result<Triple<String, String, String?>> {
        return try {
            val doc = db.collection("users").document(userId).get().await()
            val name = doc.getString("name") ?: "Unknown"
            val email = doc.getString("email") ?: ""
            val photoUrl = doc.getString("photoUrl")
            Result.success(Triple(name, email, photoUrl))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    suspend fun updateProfile(userId: String, name: String, photoUri: Uri?): Result<String?> {
        return try {
            var uploadedUrl: String? = null
            if (photoUri != null) {
                val ref = storage.reference.child("profile_photos/$userId/${UUID.randomUUID()}.jpg")
                ref.putFile(photoUri).await()
                uploadedUrl = ref.downloadUrl.await().toString()
            }

            val updates = mutableMapOf<String, Any>("name" to name)
            if (uploadedUrl != null) updates["photoUrl"] = uploadedUrl

            db.collection("users").document(userId)
                .set(updates, SetOptions.merge())
                .await()

            Result.success(uploadedUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * the number of challenges this user has won
     */
    suspend fun getWinsCount(userId: String): Result<Long> {
        return try {
            val doc = db.collection("users").document(userId).get().await()
            Result.success(doc.getLong("wins") ?: 0L)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}