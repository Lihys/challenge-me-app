package com.course.challengeme.data.repos

import android.content.Context
import android.location.Geocoder
import android.net.Uri
import com.course.challengeme.data.models.ProofModel
import com.course.challengeme.data.models.TeamBonusModel
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
import java.text.SimpleDateFormat

class ProofRepo {
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // companion objects are compile time constants
    companion object {
        const val BASE_POINTS = 10L
        const val PHOTO_BONUS = 5L
        const val LOCATION_BONUS = 5L
        const val TEAM_BONUS = 5L
    }

    // returning the day's 00:00
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
                if (!Geocoder.isPresent())//if not available
                    return@withContext null
                @Suppress("DEPRECATION")//to ignore warnings

                val addresses = Geocoder(context, Locale.getDefault()).getFromLocation(lat, lng, 1)
                val address = addresses?.firstOrNull() ?: return@withContext null
                // to get location,City name as one string
                listOfNotNull(
                    address.subLocality ?: address.thoroughfare,
                    address.locality
                ).joinToString(", ").ifBlank { null }
            } catch (e: Exception) {
                null
            }
        }


    // getting the last 20 proofs. we set 20 as hardcoded default but we can ask for anything
    // sorted from new to old
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
     * submit proof with text/photo/ location
     * Points = 10 (base: any check-in) + 5 (photo) + 5 (location),
     *  +5 team bonus when 2 or more members checked in in a day
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

        //what did the user submit
        val hasText = !text.isNullOrBlank()
        val hasPhoto = photoUri != null
        val hasLocation = yAxis != null && xAxis != null


        // if nothing was provided we don;t accept
        if (!hasText && !hasPhoto && !hasLocation) {
            return Result.failure(IllegalStateException("Add some text, a photo, or your location"))
        }

        return try {
            coroutineScope {
                // Photo upload, team-bonus check, and location name

                // uploading the photo and resolving the location together to take less time
                // deffered allows us to conitinue until called to it then we wait

                // !! tells the compiler it cannot be null ( we checked...)
                val photoUrlDeferred = async {
                    if (hasPhoto) {
                        val ref = storage.reference.child("update_photos/$challengeId/$userId/${UUID.randomUUID()}.jpg")
                        ref.putFile(photoUri!!).await()
                        ref.downloadUrl.await().toString()
                    }
                    else
                        null
                }

                val locationNameDeferred = async {
                    if (hasLocation)
                        resolveLocationName(context, yAxis!!, xAxis!!)
                    else
                        null
                }

                val photoUrl = photoUrlDeferred.await()
                val locationName = locationNameDeferred.await()

                var points = BASE_POINTS

                if (hasPhoto)
                    points += PHOTO_BONUS
                if (hasLocation)
                    points += LOCATION_BONUS

                val update = ProofModel(
                    challengeId = challengeId,
                    userId = userId,
                    type = listOfNotNull(
                        if (hasText)
                            "text"
                        else
                            null,
                        if (hasPhoto)
                            "photo"
                        else
                            null,
                        if (hasLocation)
                            "location"
                        else
                            null
                    ).joinToString(","),
                    textContent = text,
                    photoUrl = photoUrl,
                    y = yAxis,
                    x = xAxis,
                    locationName = locationName,
                    pointsAwarded = points,
                    createdAt = Timestamp.now()
                )

                // Creating the proof document and updating points
                // it's async / concurrency
                val addDeferred = async { db.collection("updates").add(update).await() }
                val pointsUpdateDeferred = async {
                    db.collection("challenges").document(challengeId)
                        .update("memberPoints.$userId", FieldValue.increment(points))
                        .await()
                }
                val teamBonusDeferred = async { applyTeamBonus(challengeId, userId) }

                val docRef = addDeferred.await()
                pointsUpdateDeferred.await()
                teamBonusDeferred.await()

                Result.success(update.copy(id = docRef.id))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    suspend fun getWeeklyLeaderboard(challengeId: String): Result<List<Pair<String, Long>>> {
        return try {
            // current time - 7 days
            val sevenDaysAgo = Timestamp(Date(System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000))

            val updatesSnapshot = db.collection("updates")
                .whereEqualTo("challengeId", challengeId)
                .whereGreaterThanOrEqualTo("createdAt", sevenDaysAgo)
                .get()
                .await()
            val updates = updatesSnapshot.documents.mapNotNull { it.toObject(ProofModel::class.java) }
            // we group by the user, ao we basically sum up the points
            //_ means we only need the values
            val totals = updates.groupBy { it.userId }
                .mapValues { (_, ups) -> ups.sumOf { it.pointsAwarded } }
                .toMutableMap()//mutable bc totals[memberid] modifies it

            // adding team bonuses to the leaderboard

            val bonusSnapshot = db.collection("challenges").document(challengeId)
                .collection("dailyBonuses")
                .whereGreaterThanOrEqualTo("updatedAt", sevenDaysAgo)
                .get()
                .await()
            bonusSnapshot.documents
                .mapNotNull { it.toObject(TeamBonusModel::class.java) }
                .forEach { bonus ->
                    bonus.bonusAwardedMemberIds.forEach { memberId ->
                        totals[memberId] = (totals[memberId] ?: 0L) + TEAM_BONUS
                    }
                }

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

    private fun todayDateKey(): String =
        SimpleDateFormat("dd-MM-yyyy", Locale.UK).format(Date())

    /**
     * if more than 2 members checked in, each user that checks in gets the bonus
     * already awarded members are skipped so it doesn't matter that we call it again and again
     */
     private suspend fun applyTeamBonus(challengeId: String, userId: String): Result<TeamBonusModel> {
        return try {
            val dateKey = todayDateKey()
            val bonusRef = db.collection("challenges").document(challengeId)
                .collection("dailyBonuses").document(dateKey)
            val challengeRef = db.collection("challenges").document(challengeId)

            val updated = db.runTransaction { transaction ->
                val snapshot = transaction.get(bonusRef)
                val existing = snapshot.toObject(TeamBonusModel::class.java)
                    ?: TeamBonusModel(dateKey = dateKey)

                val checkedIn = existing.checkedInMemberIds.toMutableSet().apply { add(userId) }
                val alreadyAwarded = existing.bonusAwardedMemberIds.toMutableSet()

                if (checkedIn.size >= 2) {
                    (checkedIn - alreadyAwarded).forEach { memberId ->
                        transaction.update(challengeRef, "memberPoints.$memberId", FieldValue.increment(
                            TEAM_BONUS
                        ))
                    }
                    alreadyAwarded.addAll(checkedIn)
                }

                val result = TeamBonusModel(
                    dateKey = dateKey,
                    checkedInMemberIds = checkedIn.toList(),
                    bonusAwardedMemberIds = alreadyAwarded.toList(),
                    updatedAt = Timestamp.now()
                )
                transaction.set(bonusRef, result)
                result
            }.await()

            Result.success(updated)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTeamBonus(challengeId: String): Result<TeamBonusModel?> {
        return try {
            val snapshot = db.collection("challenges").document(challengeId)
                .collection("dailyBonuses").document(todayDateKey())
                .get()
                .await()
            Result.success(snapshot.toObject(TeamBonusModel::class.java))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


}