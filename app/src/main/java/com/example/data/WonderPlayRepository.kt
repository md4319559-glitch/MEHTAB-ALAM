package com.example.data

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlin.math.max

class WonderPlayRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun observeProfile(userId: String): Flow<PlayerProfile?> = flow {
        require(userId.isNotBlank()) { "User ID must not be blank" }
        val path = "users/$userId"
        emitAll(
            db.collection("users").document(userId)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            PlayerProfile::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else {
                        null
                    }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    fun observeRecentSessions(userId: String): Flow<List<GameSession>> = flow {
        require(userId.isNotBlank()) { "User ID must not be blank" }
        val path = "users/$userId/gameSessions"
        emitAll(
            db.collection("users").document(userId).collection("gameSessions")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(10)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        GameSession::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun ensureProfileExists(rawDisplayName: String?): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}"
        return try {
            val uid = requireUserId()
            val profileRef = db.collection("users").document(uid)
            docPath = profileRef.path
            val snapshot = profileRef.get().await()
            if (!snapshot.exists()) {
                val safeName = sanitizeDisplayName(rawDisplayName)
                val payload = mapOf(
                    "userId" to uid,
                    "displayName" to safeName,
                    "totalStars" to 3L, // Welcome starter stars!
                    "gamesPlayed" to 0L,
                    "balloonBestScore" to 0L,
                    "memoryBestScore" to 0L,
                    "mathBestScore" to 0L,
                    "xylophoneBestScore" to 0L,
                    "unlockedStickers" to emptyList<String>(),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                profileRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun recordGameResult(
        gameType: GameType,
        score: Int,
        starsEarned: Int,
        difficulty: DifficultyLevel
    ): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}"
        return try {
            val uid = requireUserId()
            val safeScore = score.coerceIn(0, 100000).toLong()
            val safeStars = starsEarned.coerceIn(0, 3).toLong()

            val profileRef = db.collection("users").document(uid)
            val sessionRef = profileRef.collection("gameSessions").document()
            docPath = profileRef.path

            val fallbackName = sanitizeDisplayName(auth.currentUser?.displayName)

            db.runTransaction { transaction ->
                val snapshot = transaction.get(profileRef)
                if (!snapshot.exists()) {
                    val newProfile = mutableMapOf<String, Any>(
                        "userId" to uid,
                        "displayName" to fallbackName,
                        "totalStars" to (3L + safeStars),
                        "gamesPlayed" to 1L,
                        "balloonBestScore" to if (gameType == GameType.BALLOON_POP) safeScore else 0L,
                        "memoryBestScore" to if (gameType == GameType.MEMORY_MATCH) safeScore else 0L,
                        "mathBestScore" to if (gameType == GameType.MATH_SAFARI) safeScore else 0L,
                        "xylophoneBestScore" to if (gameType == GameType.XYLOPHONE) safeScore else 0L,
                        "unlockedStickers" to emptyList<String>(),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    transaction.set(profileRef, newProfile)
                } else {
                    val currentStars = snapshot.getLong("totalStars") ?: 0L
                    val currentGames = snapshot.getLong("gamesPlayed") ?: 0L
                    val updates = mutableMapOf<String, Any>(
                        "totalStars" to (currentStars + safeStars).coerceAtMost(1_000_000L),
                        "gamesPlayed" to (currentGames + 1L).coerceAtMost(1_000_000L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    when (gameType) {
                        GameType.BALLOON_POP -> {
                            val prev = snapshot.getLong("balloonBestScore") ?: 0L
                            updates["balloonBestScore"] = max(prev, safeScore)
                        }
                        GameType.MEMORY_MATCH -> {
                            val prev = snapshot.getLong("memoryBestScore") ?: 0L
                            updates["memoryBestScore"] = max(prev, safeScore)
                        }
                        GameType.MATH_SAFARI -> {
                            val prev = snapshot.getLong("mathBestScore") ?: 0L
                            updates["mathBestScore"] = max(prev, safeScore)
                        }
                        GameType.XYLOPHONE -> {
                            val prev = snapshot.getLong("xylophoneBestScore") ?: 0L
                            updates["xylophoneBestScore"] = max(prev, safeScore)
                        }
                    }
                    transaction.update(profileRef, updates)
                }

                val sessionPayload = mapOf(
                    "userId" to uid,
                    "gameType" to gameType.wireValue,
                    "score" to safeScore,
                    "starsEarned" to safeStars,
                    "difficulty" to difficulty.wireValue,
                    "createdAt" to FieldValue.serverTimestamp()
                )
                transaction.set(sessionRef, sessionPayload)
            }.await()

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun unlockSticker(sticker: StickerItem): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}"
        return try {
            val uid = requireUserId()
            val profileRef = db.collection("users").document(uid)
            docPath = profileRef.path

            db.runTransaction { transaction ->
                val snapshot = transaction.get(profileRef)
                check(snapshot.exists()) { "Player profile not found yet." }
                val currentStars = snapshot.getLong("totalStars") ?: 0L
                val rawList = snapshot.get("unlockedStickers") as? List<*> ?: emptyList<Any>()
                val currentStickers = rawList.filterIsInstance<String>()

                if (currentStickers.contains(sticker.id)) {
                    return@runTransaction
                }
                check(currentStars >= sticker.starCost) {
                    "Need ${sticker.starCost} stars to unlock ${sticker.name}!"
                }
                check(currentStickers.size < 20) {
                    "Sticker album is full!"
                }

                transaction.update(
                    profileRef,
                    mapOf(
                        "totalStars" to (currentStars - sticker.starCost),
                        "unlockedStickers" to (currentStickers + sticker.id),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateDisplayName(newName: String): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}"
        return try {
            val uid = requireUserId()
            val trimmed = newName.trim().take(60)
            require(trimmed.isNotEmpty()) { "Name cannot be empty" }
            val profileRef = db.collection("users").document(uid)
            docPath = profileRef.path
            profileRef.update(
                mapOf(
                    "displayName" to trimmed,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    private fun sanitizeDisplayName(raw: String?): String {
        val cleaned = raw?.trim().orEmpty()
        return if (cleaned.isEmpty()) "Little Explorer" else cleaned.take(60)
    }
}
