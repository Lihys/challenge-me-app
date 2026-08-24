package com.course.challengeme.data.repos

import com.course.challengeme.data.models.ChallengeModel
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

// talks with the firestore for us
// it's screens -> chellenge repo -> firestore

class ChallengeRepo {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // returns the challenge id (string)
    suspend fun createChallenge(
        title: String,
        description: String?,
        prize: String?,
        endDate: Timestamp
    ): Result<String> {
        return try {

            // if there's a logged in user, get its id
            // if it's null use this fallback (?:)
            val userId = auth.currentUser?.uid
                ?: return Result.failure(IllegalStateException("Not logged in"))

            val inviteCode = generateInviteCode()

            // creating the model (passing the arguments)
            val challenge = ChallengeModel(
                title = title,
                description = description,
                prize = prize,
                endDate = endDate,
                inviteCode = inviteCode,
                ownerId = userId,
                memberIds = listOf(userId), // bc the creator is the first member !
                createdAt = Timestamp.now()
            )

            // creates a new document with an automatic generated id, under collections
            val docRef = db.collection("challenges").document()
            // and this writes the challenge into firebase
            docRef.set(challenge.copy(id = docRef.id)).await() //it's a copy of this but we change the id to the generated one

            Result.success(docRef.id) // if everything works we get this id
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // returns the challenge
    suspend fun getChallenge(challengeId: String): Result<ChallengeModel> {
        return try {
            // getting challenges/our document
            val snapshot = db.collection("challenges").document(challengeId).get().await()
            // with this we convert the data into the challengemodel object
            val challenge = snapshot.toObject(ChallengeModel::class.java)
                ?: return Result.failure(IllegalStateException("Challenge not found"))
            Result.success(challenge)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    private fun generateInviteCode(): String {
        val chars = "ABCDEFGHIJLKLMNPQRSTUVWXYZ123456789" // 0 and o are confusing so we use the others :)
        //map is to do something to evry item
        //we generate a 6 digit code
        return (1..6).map { chars.random() }.joinToString("")
    }

    suspend fun joinChallengeViaCode(inviteCode: String): Result<String> {
        return try {

            // if there's a logged in user, get its id
            // if it's null use this fallback (?:)
            val userId = auth.currentUser?.uid
                ?: return Result.failure(IllegalStateException("Not logged in"))

            // we search in firestore for the code the user enters
            val querySnapshot = db.collection("challenges")
                .whereEqualTo("inviteCode", inviteCode.uppercase())
                .limit(1)
                .get()
                .await()

            // first one we find but it is unique. if we dont find it it returns null
            val challengeDoc = querySnapshot.documents.firstOrNull()
                ?: return Result.failure(IllegalStateException("No challenge found with that code"))

            val challengeId = challengeDoc.id
            val challenge = challengeDoc.toObject(ChallengeModel::class.java)

            // if the user is already in that challenge
            if (challenge != null && userId in challenge.memberIds) {
                return Result.failure(IllegalStateException("You're already in this challenge"))
            }

            // add this user id to the array
            db.collection("challenges").document(challengeId)
                .update("memberIds",
                    FieldValue.arrayUnion(userId))
                .await()

            Result.success(challengeId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    //getting all the challenges the user joined (list of their challenges)
    suspend fun getChallengesForUser(userId: String): Result<List<ChallengeModel>> {
        return try {
            val querySnapshot = db.collection("challenges")
                .whereArrayContains("memberIds", userId)
                .get()
                .await()

            //for every doc we convert to challengeModel
            val challenges = querySnapshot.documents.mapNotNull {
                it.toObject(ChallengeModel::class.java)//"it" is the current item
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
            // rransaction is to deal with race conditions (we don;t want to users to get the win)
            //so we do all of these together- reading, deciding the winner, setting the winner, incrementing the wins
            //without this the winner will get +2 points

            val winnerId = db.runTransaction { transaction ->
                val challengeRef = db.collection("challenges").document(challengeId)
                val snapshot = transaction.get(challengeRef)
                val challenge = snapshot.toObject(ChallengeModel::class.java)
                    ?: throw IllegalStateException("Challenge not found")

                //if winner exists don't do anything
                val existingWinnerId = challenge.winnerId
                if (existingWinnerId != null) {
                    return@runTransaction existingWinnerId
                }

                //challenge not over
                val endDate = challenge.endDate
                if (endDate == null || endDate > Timestamp.now()) {
                    return@runTransaction null // not over yet
                }

                //max points is the winner
                //"it" here is the member id bc this is the list we are looking at
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