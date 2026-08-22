package com.course.challengeme.data

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class ChallengeRepo {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    suspend fun createChallenge(
        title: String,
        description: String?,
        prize: String?,
        endDate: Timestamp
    ): Result<String> {
        return try {
            val userId = auth.currentUser?.uid
                ?: return Result.failure(IllegalStateException("Not logged in"))

            val inviteCode = generateInviteCode()

            val challenge = ChallengeModel(
                title = title,
                description = description,
                prize = prize,
                endDate = endDate,
                inviteCode = inviteCode,
                ownerId = userId,
                memberIds = listOf(userId),
                createdAt = Timestamp.now()
            )

            val docRef = db.collection("challenges").document()
            docRef.set(challenge.copy(id = docRef.id)).await()

            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getChallenge(challengeId: String): Result<ChallengeModel> {
        return try {
            val snapshot = db.collection("challenges").document(challengeId).get().await()
            val challenge = snapshot.toObject(ChallengeModel::class.java)
                ?: return Result.failure(IllegalStateException("Challenge not found"))
            Result.success(challenge)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    private fun generateInviteCode(): String {
        val chars = "ABCDEFGHIJLKLMNPQRSTUVWXYZ123456789" // 0 and o are confusing so we use the others :)
        return (1..6).map { chars.random() }.joinToString("")
    }

    suspend fun joinChallengeViaCode(inviteCode: String): Result<String> {
        return try {
            val userId = auth.currentUser?.uid
                ?: return Result.failure(IllegalStateException("Not logged in"))

            val querySnapshot = db.collection("challenges")
                .whereEqualTo("inviteCode", inviteCode.uppercase())
                .limit(1)
                .get()
                .await()

            val challengeDoc = querySnapshot.documents.firstOrNull()
                ?: return Result.failure(IllegalStateException("No challenge found with that code"))

            val challengeId = challengeDoc.id
            val challenge = challengeDoc.toObject(ChallengeModel::class.java)

            if (challenge != null && userId in challenge.memberIds) {
                return Result.failure(IllegalStateException("You're already in this challenge"))
            }

            db.collection("challenges").document(challengeId)
                .update("memberIds", FieldValue.arrayUnion(userId))
                .await()

            Result.success(challengeId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getChallengesForUser(userId: String): Result<List<ChallengeModel>> {
        return try {
            val querySnapshot = db.collection("challenges")
                .whereArrayContains("memberIds", userId)
                .get()
                .await()

            val challenges = querySnapshot.documents.mapNotNull {
                it.toObject(ChallengeModel::class.java)
            }
            Result.success(challenges)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * if winnerId is already set, we don't add anything,
     * but basically this only runs when a user opens the challenge after it ends ...
     * then it adds a win to the winner
     */
    suspend fun claimWinIfNeeded(challengeId: String): Result<String?> {
        return try {
            val winnerId = db.runTransaction { transaction ->
                val challengeRef = db.collection("challenges").document(challengeId)
                val snapshot = transaction.get(challengeRef)
                val challenge = snapshot.toObject(ChallengeModel::class.java)
                    ?: throw IllegalStateException("Challenge not found")

                val existingWinnerId = challenge.winnerId
                if (existingWinnerId != null) {
                    return@runTransaction existingWinnerId
                }

                val endDate = challenge.endDate
                if (endDate == null || endDate > Timestamp.now()) {
                    return@runTransaction null // not over yet
                }

                val winner = challenge.memberIds
                    .maxByOrNull { challenge.memberPoints[it] ?: 0L }
                    ?: return@runTransaction null // no winner

                transaction.update(challengeRef, "winnerId", winner)

                val winnerRef = db.collection("users").document(winner)
                transaction.update(winnerRef, "wins", FieldValue.increment(1))

                winner
            }.await()

            Result.success(winnerId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

}