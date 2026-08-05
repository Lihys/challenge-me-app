package com.course.challengeme.data

import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class UserRepo {
    private val db = FirebaseFirestore.getInstance()

    suspend fun saveUserProfile(userId: String, name: String, email: String) {
        db.collection("users").document(userId)
            .set(mapOf("name" to name, "email" to email))
            .await()
    }

    suspend fun getUsersByIds(userIds: List<String>): Map<String, String> {
        if (userIds.isEmpty()) return emptyMap()
        val result = mutableMapOf<String, String>()
        // Firestore whereIn supports max 10 values per query — chunk for larger groups
        userIds.chunked(10).forEach { chunk ->
            val snapshot = db.collection("users")
                .whereIn(FieldPath.documentId(), chunk)
                .get()
                .await()
            snapshot.documents.forEach { doc ->
                result[doc.id] = doc.getString("name") ?: "Unknown"
            }
        }
        return result
    }
}