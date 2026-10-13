package com.example.data.repository

import android.content.Context
import com.example.data.model.Khata
import com.example.data.model.KhataHistory
import com.example.data.model.Person
import com.example.data.model.ShareRequest
import com.example.data.model.Transaction
import com.example.data.model.UserProfile
import com.example.data.model.UserIdCheckResult
import com.example.data.model.UsernameRecord
import com.example.util.OperationType
import com.example.util.handleFirestoreError
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import com.example.R
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.Date
import kotlin.math.roundToLong

class KhataRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    constructor(context: Context) : this(
        db = try {
            FirebaseFirestore.getInstance(context.getString(R.string.firestore_database_id))
        } catch (_: Exception) {
            FirebaseFirestore.getInstance()
        },
        auth = FirebaseAuth.getInstance()
    )

    fun getCurrentUid(): String? = auth.currentUser?.uid

    fun getCurrentEmail(): String? = auth.currentUser?.email

    private fun requireUserId(): String {
        return auth.currentUser?.uid ?: throw IllegalStateException("User must be authenticated")
    }

    // --- USER PROFILE & USERNAME MANAGEMENT ---

    suspend fun checkUserIdStatus(customUserId: String): UserIdCheckResult {
        val cleanId = customUserId.trim().lowercase()
        if (cleanId.length < 3) {
            return UserIdCheckResult.Error(
                IllegalArgumentException("User ID must be at least 3 characters"),
                "User ID must be at least 3 characters"
            )
        }
        if (!cleanId.matches(Regex("^[a-z0-9_]+$"))) {
            return UserIdCheckResult.Error(
                IllegalArgumentException("Invalid User ID format"),
                "User ID can only contain lowercase letters, numbers, and underscores"
            )
        }
        return try {
            val doc = db.collection("user_ids").document(cleanId).get().await()
            if (doc.exists()) {
                UserIdCheckResult.Taken
            } else {
                UserIdCheckResult.Available
            }
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, "user_ids/$cleanId")
            UserIdCheckResult.Error(e, e.localizedMessage ?: "Unable to check User ID")
        }
    }

    suspend fun checkUserIdAvailable(customUserId: String): Result<Boolean> {
        val cleanId = customUserId.trim().lowercase()
        if (cleanId.length < 3) {
            return Result.failure(IllegalArgumentException("User ID must be at least 3 characters"))
        }
        return try {
            val doc = db.collection("user_ids").document(cleanId).get().await()
            Result.success(!doc.exists())
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, "user_ids/$cleanId")
            Result.failure(e)
        }
    }

    suspend fun getUserProfile(uid: String): UserProfile? {
        val doc = db.collection("users").document(uid).get().await()
        return if (doc.exists()) {
            doc.toObject(UserProfile::class.java)?.let { profile ->
                if (profile.uid.isBlank()) profile.copy(uid = doc.id) else profile
            }
        } else null
    }

    suspend fun registerUserProfile(name: String, email: String, customUserId: String): Result<UserProfile> {
        return try {
            val uid = requireUserId()
            val cleanId = customUserId.trim().lowercase()

            val usernameDoc = db.collection("user_ids").document(cleanId)
            val userDoc = db.collection("users").document(uid)

            val profile = UserProfile(
                uid = uid,
                name = name.trim(),
                email = email.trim(),
                customUserId = cleanId
            )

            db.runTransaction { transaction ->
                val existingUsername = transaction.get(usernameDoc)
                if (existingUsername.exists()) {
                    val existingUid = existingUsername.getString("uid")
                    if (existingUid != null && existingUid != uid) {
                        throw FirebaseFirestoreException(
                            "User ID '$cleanId' is already claimed by another user.",
                            FirebaseFirestoreException.Code.ALREADY_EXISTS
                        )
                    }
                }

                transaction.set(usernameDoc, mapOf(
                    "customUserId" to cleanId,
                    "uid" to uid,
                    "createdAt" to FieldValue.serverTimestamp()
                ))

                transaction.set(userDoc, mapOf(
                    "uid" to uid,
                    "name" to name.trim(),
                    "email" to email.trim(),
                    "customUserId" to cleanId,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ))
                null
            }.await()

            Result.success(profile)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "users")
            Result.failure(e)
        }
    }

    suspend fun updateUserProfileName(newName: String): Result<Unit> {
        return try {
            val uid = requireUserId()
            db.collection("users").document(uid).update(
                mapOf(
                    "name" to newName.trim(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users")
            Result.failure(e)
        }
    }

    suspend fun searchUserByCustomId(customUserId: String): Result<UserProfile?> {
        return try {
            val cleanId = customUserId.trim().lowercase()
            val usernameDoc = db.collection("user_ids").document(cleanId).get().await()
            if (!usernameDoc.exists()) {
                return Result.success(null)
            }
            val targetUid = usernameDoc.getString("uid") ?: return Result.success(null)
            val userDoc = db.collection("users").document(targetUid).get().await()
            if (!userDoc.exists()) {
                return Result.success(null)
            }
            val profile = userDoc.toObject(UserProfile::class.java)?.let {
                if (it.uid.isBlank()) it.copy(uid = userDoc.id) else it
            }
            Result.success(profile)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, "user_ids/$customUserId")
            Result.failure(e)
        }
    }

    // --- KHATAS MANAGEMENT ---

    fun observeUserKhatas(): Flow<List<Khata>> {
        val uid = auth.currentUser?.uid ?: return emptyFlow()
        return db.collection("khatas")
            .whereArrayContains("memberUids", uid)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    val isDel = doc.getBoolean("isDeleted") ?: doc.getBoolean("deleted") ?: false
                    doc.toObject(Khata::class.java)?.let { khata ->
                        val populated = if (khata.id.isBlank()) khata.copy(id = doc.id, isDeleted = isDel) else khata.copy(isDeleted = isDel)
                        populated
                    }
                }.filter { !it.isDeleted } // Exclude soft-deleted khatas from normal views
            }
            .catch { e ->
                handleFirestoreError(e as Exception, OperationType.LIST, "khatas")
                throw e
            }
    }

    fun observeTrashKhatas(): Flow<List<Khata>> {
        val uid = auth.currentUser?.uid ?: return emptyFlow()
        return db.collection("khatas")
            .whereArrayContains("memberUids", uid)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    val isDel = doc.getBoolean("isDeleted") ?: doc.getBoolean("deleted") ?: false
                    doc.toObject(Khata::class.java)?.let { khata ->
                        val populated = if (khata.id.isBlank()) khata.copy(id = doc.id, isDeleted = isDel) else khata.copy(isDeleted = isDel)
                        populated
                    }
                }.filter { it.isDeleted && it.ownerUid == uid }
            }
            .catch { e ->
                handleFirestoreError(e as Exception, OperationType.LIST, "khatas/trash")
                throw e
            }
    }

    suspend fun createKhata(name: String, ownerName: String): Result<String> {
        return try {
            val uid = requireUserId()
            val khataRef = db.collection("khatas").document()
            val khataId = khataRef.id

            val khataData = mapOf(
                "id" to khataId,
                "name" to name.trim(),
                "ownerUid" to uid,
                "ownerName" to ownerName.trim(),
                "memberUids" to listOf(uid),
                "members" to mapOf(uid to ownerName.trim()),
                "totalPeople" to 0L,
                "totalLena" to 0.0,
                "totalDena" to 0.0,
                "netBalance" to 0.0,
                "isDeleted" to false,
                "deleted" to false,
                "deletedAt" to null,
                "deletedBy" to null,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            khataRef.set(khataData).await()

            logHistory(
                khataId = khataId,
                actionType = "KHATA_CREATED",
                description = "Khata '${name.trim()}' created",
                performedByUid = uid,
                performedByName = ownerName
            )

            Result.success(khataId)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "khatas")
            Result.failure(e)
        }
    }

    suspend fun updateKhataName(khataId: String, newName: String, editorName: String): Result<Unit> {
        return try {
            val uid = requireUserId()
            db.collection("khatas").document(khataId).update(
                mapOf(
                    "name" to newName.trim(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            logHistory(
                khataId = khataId,
                actionType = "KHATA_UPDATED",
                description = "Khata renamed to '${newName.trim()}' by $editorName",
                performedByUid = uid,
                performedByName = editorName
            )

            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "khatas/$khataId")
            Result.failure(e)
        }
    }

    suspend fun softDeleteKhata(khataId: String, performerName: String): Result<Unit> {
        return try {
            val uid = requireUserId()
            val khataDoc = db.collection("khatas").document(khataId).get().await()
            val ownerUid = khataDoc.getString("ownerUid")
            val khataName = khataDoc.getString("name") ?: "Khata"

            if (ownerUid != uid) {
                return Result.failure(IllegalStateException("Only the owner can move this khata to Trash."))
            }

            db.collection("khatas").document(khataId).update(
                mapOf(
                    "isDeleted" to true,
                    "deleted" to true,
                    "deletedAt" to FieldValue.serverTimestamp(),
                    "deletedBy" to performerName,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            logHistory(
                khataId = khataId,
                actionType = "KHATA_MOVED_TO_TRASH",
                description = "Khata '$khataName' moved to Trash by $performerName",
                performedByUid = uid,
                performedByName = performerName
            )

            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "khatas/$khataId")
            Result.failure(e)
        }
    }

    suspend fun recoverKhata(khataId: String, performerName: String): Result<Unit> {
        return try {
            val uid = requireUserId()
            val khataDoc = db.collection("khatas").document(khataId).get().await()
            val ownerUid = khataDoc.getString("ownerUid")
            val khataName = khataDoc.getString("name") ?: "Khata"

            if (ownerUid != uid) {
                return Result.failure(IllegalStateException("Only the owner can recover this khata."))
            }

            db.collection("khatas").document(khataId).update(
                mapOf(
                    "isDeleted" to false,
                    "deleted" to false,
                    "deletedAt" to null,
                    "deletedBy" to null,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            logHistory(
                khataId = khataId,
                actionType = "KHATA_RECOVERED",
                description = "Khata '$khataName' recovered from Trash by $performerName",
                performedByUid = uid,
                performedByName = performerName
            )

            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "khatas/$khataId")
            Result.failure(e)
        }
    }

    suspend fun permanentlyDeleteKhata(khataId: String): Result<Unit> {
        return try {
            val uid = requireUserId()
            val khataRef = db.collection("khatas").document(khataId)
            val khataDoc = khataRef.get().await()
            val ownerUid = khataDoc.getString("ownerUid")

            if (ownerUid != uid) {
                return Result.failure(IllegalStateException("Only the owner can permanently delete this khata."))
            }

            // Clean up subcollections: people, transactions, history
            val peopleSnap = khataRef.collection("people").get().await()
            val txSnap = khataRef.collection("transactions").get().await()
            val histSnap = khataRef.collection("history").get().await()

            val batch = db.batch()
            for (p in peopleSnap.documents) {
                batch.delete(p.reference)
            }
            for (t in txSnap.documents) {
                batch.delete(t.reference)
            }
            for (h in histSnap.documents) {
                batch.delete(h.reference)
            }
            batch.delete(khataRef)
            batch.commit().await()

            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "khatas/$khataId")
            Result.failure(e)
        }
    }

    suspend fun leaveSharedKhata(khataId: String, memberName: String): Result<Unit> {
        return try {
            val uid = requireUserId()
            val khataRef = db.collection("khatas").document(khataId)
            val khataDoc = khataRef.get().await()

            val ownerUid = khataDoc.getString("ownerUid")
            val khataName = khataDoc.getString("name") ?: "Khata"

            if (ownerUid == uid) {
                return Result.failure(IllegalStateException("Owner cannot leave their own khata."))
            }

            // Remove only self
            khataRef.update(
                mapOf(
                    "memberUids" to FieldValue.arrayRemove(uid),
                    "members.$uid" to FieldValue.delete(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            logHistory(
                khataId = khataId,
                actionType = "MEMBER_LEFT",
                description = "$memberName left shared khata '$khataName'",
                performedByUid = uid,
                performedByName = memberName
            )

            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "khatas/$khataId")
            Result.failure(e)
        }
    }

    // --- PEOPLE MANAGEMENT ---

    fun observePeople(khataId: String): Flow<List<Person>> {
        if (auth.currentUser == null) return emptyFlow()
        return db.collection("khatas").document(khataId).collection("people")
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    val isDel = doc.getBoolean("isDeleted") ?: doc.getBoolean("deleted") ?: false
                    doc.toObject(Person::class.java)?.let { person ->
                        val populated = if (person.id.isBlank()) person.copy(id = doc.id, isDeleted = isDel) else person.copy(isDeleted = isDel)
                        populated
                    }
                }.filter { !it.isDeleted }
            }
            .catch { e ->
                handleFirestoreError(e as Exception, OperationType.LIST, "khatas/$khataId/people")
                throw e
            }
    }

    fun observeTrashPeople(khataId: String): Flow<List<Person>> {
        if (auth.currentUser == null) return emptyFlow()
        return db.collection("khatas").document(khataId).collection("people")
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    val isDel = doc.getBoolean("isDeleted") ?: doc.getBoolean("deleted") ?: false
                    doc.toObject(Person::class.java)?.let { person ->
                        val populated = if (person.id.isBlank()) person.copy(id = doc.id, isDeleted = isDel) else person.copy(isDeleted = isDel)
                        populated
                    }
                }.filter { it.isDeleted }
            }
            .catch { e ->
                handleFirestoreError(e as Exception, OperationType.LIST, "khatas/$khataId/people/trash")
                throw e
            }
    }

    suspend fun addPerson(khataId: String, name: String, phone: String, performerName: String): Result<String> {
        return try {
            val uid = requireUserId()
            val personRef = db.collection("khatas").document(khataId).collection("people").document()
            val personId = personRef.id

            val personData = mapOf(
                "id" to personId,
                "khataId" to khataId,
                "name" to name.trim(),
                "phone" to phone.trim(),
                "totalLena" to 0.0,
                "totalDena" to 0.0,
                "netBalance" to 0.0,
                "isDeleted" to false,
                "deleted" to false,
                "deletedAt" to null,
                "deletedBy" to null,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            personRef.set(personData).await()

            recomputeKhataTotals(khataId)

            logHistory(
                khataId = khataId,
                actionType = "PERSON_ADDED",
                description = "Added '$name' to khata",
                personName = name.trim(),
                performedByUid = uid,
                performedByName = performerName
            )

            Result.success(personId)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "khatas/$khataId/people")
            Result.failure(e)
        }
    }

    suspend fun updatePerson(khataId: String, personId: String, name: String, phone: String, performerName: String): Result<Unit> {
        return try {
            val uid = requireUserId()
            db.collection("khatas").document(khataId).collection("people").document(personId).update(
                mapOf(
                    "name" to name.trim(),
                    "phone" to phone.trim(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            logHistory(
                khataId = khataId,
                actionType = "PERSON_UPDATED",
                description = "Updated person details for '$name'",
                personName = name.trim(),
                performedByUid = uid,
                performedByName = performerName
            )

            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "khatas/$khataId/people/$personId")
            Result.failure(e)
        }
    }

    suspend fun softDeletePerson(khataId: String, personId: String, personName: String, performerName: String): Result<Unit> {
        return try {
            val uid = requireUserId()
            db.collection("khatas").document(khataId).collection("people").document(personId).update(
                mapOf(
                    "isDeleted" to true,
                    "deleted" to true,
                    "deletedAt" to FieldValue.serverTimestamp(),
                    "deletedBy" to performerName,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            // Recalculate khata totals so soft-deleted person is excluded from active totalPeople and balance
            recomputeKhataTotals(khataId)

            logHistory(
                khataId = khataId,
                actionType = "PERSON_MOVED_TO_TRASH",
                description = "Person '$personName' moved to Trash by $performerName",
                personName = personName,
                performedByUid = uid,
                performedByName = performerName
            )

            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "khatas/$khataId/people/$personId")
            Result.failure(e)
        }
    }

    suspend fun recoverPerson(khataId: String, personId: String, personName: String, performerName: String): Result<Unit> {
        return try {
            val uid = requireUserId()
            db.collection("khatas").document(khataId).collection("people").document(personId).update(
                mapOf(
                    "isDeleted" to false,
                    "deleted" to false,
                    "deletedAt" to null,
                    "deletedBy" to null,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            // Restore person balance and khata totals
            recomputePersonBalances(khataId, personId)
            recomputeKhataTotals(khataId)

            logHistory(
                khataId = khataId,
                actionType = "PERSON_RECOVERED",
                description = "Person '$personName' recovered from Trash by $performerName",
                personName = personName,
                performedByUid = uid,
                performedByName = performerName
            )

            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "khatas/$khataId/people/$personId")
            Result.failure(e)
        }
    }

    suspend fun permanentlyDeletePerson(khataId: String, personId: String, personName: String, performerName: String): Result<Unit> {
        return try {
            val uid = requireUserId()
            // Delete all transactions for this person
            val txDocs = db.collection("khatas").document(khataId)
                .collection("transactions")
                .whereEqualTo("personId", personId)
                .get().await()

            val batch = db.batch()
            for (doc in txDocs.documents) {
                batch.delete(doc.reference)
            }
            batch.delete(db.collection("khatas").document(khataId).collection("people").document(personId))
            batch.commit().await()

            // Recompute khata balances
            recomputeKhataTotals(khataId)

            logHistory(
                khataId = khataId,
                actionType = "PERSON_PERMANENTLY_DELETED",
                description = "Permanently deleted '$personName' and all related hisaab",
                personName = personName,
                performedByUid = uid,
                performedByName = performerName
            )

            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "khatas/$khataId/people/$personId")
            Result.failure(e)
        }
    }

    // Default deletePerson means soft delete as per specifications
    suspend fun deletePerson(khataId: String, personId: String, personName: String, performerName: String): Result<Unit> {
        return softDeletePerson(khataId, personId, personName, performerName)
    }

    // --- TRANSACTIONS MANAGEMENT ---

    fun observeTransactions(khataId: String, personId: String): Flow<List<Transaction>> {
        if (auth.currentUser == null) return emptyFlow()
        return db.collection("khatas").document(khataId)
            .collection("transactions")
            .whereEqualTo("personId", personId)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    val isDel = doc.getBoolean("isDeleted") ?: doc.getBoolean("deleted") ?: false
                    doc.toObject(Transaction::class.java)?.let { tx ->
                        val populated = if (tx.id.isBlank()) tx.copy(id = doc.id, isDeleted = isDel) else tx.copy(isDeleted = isDel)
                        populated
                    }
                }.filter { !it.isDeleted }
                .sortedByDescending {
                    it.timestamp?.toDate()?.time ?: 0L
                }
            }
            .catch { e ->
                handleFirestoreError(e as Exception, OperationType.LIST, "khatas/$khataId/transactions")
                throw e
            }
    }

    fun observeTrashTransactions(khataId: String, personId: String? = null): Flow<List<Transaction>> {
        if (auth.currentUser == null) return emptyFlow()
        val query = if (personId != null) {
            db.collection("khatas").document(khataId).collection("transactions").whereEqualTo("personId", personId)
        } else {
            db.collection("khatas").document(khataId).collection("transactions")
        }
        return query
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    val isDel = doc.getBoolean("isDeleted") ?: doc.getBoolean("deleted") ?: false
                    doc.toObject(Transaction::class.java)?.let { tx ->
                        val populated = if (tx.id.isBlank()) tx.copy(id = doc.id, isDeleted = isDel) else tx.copy(isDeleted = isDel)
                        populated
                    }
                }.filter { it.isDeleted }
                .sortedByDescending {
                    it.deletedAt?.toDate()?.time ?: it.timestamp?.toDate()?.time ?: 0L
                }
            }
            .catch { e ->
                handleFirestoreError(e as Exception, OperationType.LIST, "khatas/$khataId/transactions/trash")
                throw e
            }
    }

    suspend fun addTransaction(
        khataId: String,
        personId: String,
        personName: String,
        amount: Double,
        type: String, // "LENA" (Maine Diye / You Gave) or "DENA" (Maine Liye / You Got)
        note: String,
        dateMillis: Long,
        performerName: String
    ): Result<String> {
        return try {
            val uid = requireUserId()
            require(amount > 0) { "Amount must be greater than zero" }

            val roundedAmount = (amount * 100.0).roundToLong() / 100.0
            val txRef = db.collection("khatas").document(khataId).collection("transactions").document()
            val txId = txRef.id

            val txDate = Timestamp(Date(dateMillis))

            val txData = mapOf(
                "id" to txId,
                "khataId" to khataId,
                "personId" to personId,
                "personName" to personName,
                "amount" to roundedAmount,
                "type" to type,
                "note" to note.trim(),
                "timestamp" to txDate,
                "isDeleted" to false,
                "deleted" to false,
                "deletedAt" to null,
                "deletedBy" to null,
                "createdTimestamp" to FieldValue.serverTimestamp(),
                "createdByUid" to uid,
                "createdByName" to performerName
            )

            txRef.set(txData).await()

            // Update Person balances and Khata balances accurately
            recomputePersonBalances(khataId, personId)
            recomputeKhataTotals(khataId)

            val typeDesc = if (type == "LENA") "Maine Diye (Lena)" else "Maine Liye (Dena)"
            logHistory(
                khataId = khataId,
                actionType = "TRANSACTION_ADDED",
                description = "$performerName added ₹$roundedAmount ($typeDesc) for $personName",
                amount = roundedAmount,
                personName = personName,
                performedByUid = uid,
                performedByName = performerName
            )

            Result.success(txId)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "khatas/$khataId/transactions")
            Result.failure(e)
        }
    }

    suspend fun updateTransaction(
        khataId: String,
        transactionId: String,
        personId: String,
        personName: String,
        newAmount: Double,
        newType: String,
        newNote: String,
        newDateMillis: Long,
        performerName: String
    ): Result<Unit> {
        return try {
            val uid = requireUserId()
            require(newAmount > 0) { "Amount must be greater than zero" }

            val roundedAmount = (newAmount * 100.0).roundToLong() / 100.0
            val txDate = Timestamp(Date(newDateMillis))

            db.collection("khatas").document(khataId).collection("transactions").document(transactionId).update(
                mapOf(
                    "amount" to roundedAmount,
                    "type" to newType,
                    "note" to newNote.trim(),
                    "timestamp" to txDate,
                    "updatedByUid" to uid,
                    "updatedByName" to performerName
                )
            ).await()

            recomputePersonBalances(khataId, personId)
            recomputeKhataTotals(khataId)

            val typeDesc = if (newType == "LENA") "Maine Diye (Lena)" else "Maine Liye (Dena)"
            logHistory(
                khataId = khataId,
                actionType = "TRANSACTION_EDITED",
                description = "$performerName edited transaction to ₹$roundedAmount ($typeDesc) for $personName",
                amount = roundedAmount,
                personName = personName,
                performedByUid = uid,
                performedByName = performerName
            )

            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "khatas/$khataId/transactions/$transactionId")
            Result.failure(e)
        }
    }

    suspend fun softDeleteTransaction(
        khataId: String,
        transactionId: String,
        personId: String,
        personName: String,
        amount: Double,
        performerName: String
    ): Result<Unit> {
        return try {
            val uid = requireUserId()
            db.collection("khatas").document(khataId).collection("transactions").document(transactionId).update(
                mapOf(
                    "isDeleted" to true,
                    "deleted" to true,
                    "deletedAt" to FieldValue.serverTimestamp(),
                    "deletedBy" to performerName,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            // Recomputing automatically ignores soft-deleted transactions!
            recomputePersonBalances(khataId, personId)
            recomputeKhataTotals(khataId)

            logHistory(
                khataId = khataId,
                actionType = "TRANSACTION_MOVED_TO_TRASH",
                description = "$performerName moved transaction of ₹$amount for $personName to Trash",
                amount = amount,
                personName = personName,
                performedByUid = uid,
                performedByName = performerName
            )

            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "khatas/$khataId/transactions/$transactionId")
            Result.failure(e)
        }
    }

    suspend fun recoverTransaction(
        khataId: String,
        transactionId: String,
        personId: String,
        personName: String,
        amount: Double,
        performerName: String
    ): Result<Unit> {
        return try {
            val uid = requireUserId()
            db.collection("khatas").document(khataId).collection("transactions").document(transactionId).update(
                mapOf(
                    "isDeleted" to false,
                    "deleted" to false,
                    "deletedAt" to null,
                    "deletedBy" to null,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            // Restores the transaction back to active balances!
            recomputePersonBalances(khataId, personId)
            recomputeKhataTotals(khataId)

            logHistory(
                khataId = khataId,
                actionType = "TRANSACTION_RECOVERED",
                description = "$performerName recovered transaction of ₹$amount for $personName from Trash",
                amount = amount,
                personName = personName,
                performedByUid = uid,
                performedByName = performerName
            )

            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "khatas/$khataId/transactions/$transactionId")
            Result.failure(e)
        }
    }

    suspend fun permanentlyDeleteTransaction(
        khataId: String,
        transactionId: String,
        personId: String,
        personName: String,
        amount: Double,
        performerName: String
    ): Result<Unit> {
        return try {
            val uid = requireUserId()
            db.collection("khatas").document(khataId).collection("transactions").document(transactionId).delete().await()

            recomputePersonBalances(khataId, personId)
            recomputeKhataTotals(khataId)

            logHistory(
                khataId = khataId,
                actionType = "TRANSACTION_PERMANENTLY_DELETED",
                description = "$performerName permanently deleted transaction of ₹$amount for $personName",
                amount = amount,
                personName = personName,
                performedByUid = uid,
                performedByName = performerName
            )

            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "khatas/$khataId/transactions/$transactionId")
            Result.failure(e)
        }
    }

    // Default deleteTransaction means soft delete as per specifications
    suspend fun deleteTransaction(
        khataId: String,
        transactionId: String,
        personId: String,
        personName: String,
        amount: Double,
        performerName: String
    ): Result<Unit> {
        return softDeleteTransaction(khataId, transactionId, personId, personName, amount, performerName)
    }

    // --- ACCURATE RECOMPUTATION OF BALANCES (INTEGER PAISE ACCURACY) ---

    private suspend fun recomputePersonBalances(khataId: String, personId: String) {
        val snapshot = db.collection("khatas").document(khataId)
            .collection("transactions")
            .whereEqualTo("personId", personId)
            .get().await()

        var lenaPaise = 0L
        var denaPaise = 0L
        var latestTimestamp: Timestamp? = null

        for (doc in snapshot.documents) {
            val isDel = doc.getBoolean("isDeleted") ?: doc.getBoolean("deleted") ?: false
            if (isDel) continue // Soft-deleted transactions do NOT affect active balances!

            val tx = doc.toObject(Transaction::class.java)?.let {
                if (it.id.isBlank()) it.copy(id = doc.id, isDeleted = isDel) else it.copy(isDeleted = isDel)
            } ?: continue

            if (tx.isDeleted) continue

            val paise = (tx.amount * 100.0).roundToLong()
            if (tx.type == "LENA") {
                lenaPaise += paise
            } else {
                denaPaise += paise
            }
            if (latestTimestamp == null || (tx.timestamp != null && tx.timestamp.seconds > latestTimestamp.seconds)) {
                latestTimestamp = tx.timestamp
            }
        }

        val totalLena = lenaPaise / 100.0
        val totalDena = denaPaise / 100.0
        val netBalance = (lenaPaise - denaPaise) / 100.0

        val updates = mutableMapOf<String, Any>(
            "totalLena" to totalLena,
            "totalDena" to totalDena,
            "netBalance" to netBalance,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (latestTimestamp != null) {
            updates["lastTransactionAt"] = latestTimestamp
        }

        db.collection("khatas").document(khataId).collection("people").document(personId).update(updates).await()
    }

    private suspend fun recomputeKhataTotals(khataId: String) {
        val peopleSnap = db.collection("khatas").document(khataId).collection("people").get().await()
        var totalPeople = 0L
        var khataLenaPaise = 0L
        var khataDenaPaise = 0L

        for (doc in peopleSnap.documents) {
            val isDel = doc.getBoolean("isDeleted") ?: doc.getBoolean("deleted") ?: false
            if (isDel) continue // Soft-deleted people do NOT count in active totals!

            val p = doc.toObject(Person::class.java)?.let {
                if (it.id.isBlank()) it.copy(id = doc.id, isDeleted = isDel) else it.copy(isDeleted = isDel)
            } ?: continue

            if (p.isDeleted) continue

            totalPeople++
            khataLenaPaise += (p.totalLena * 100.0).roundToLong()
            khataDenaPaise += (p.totalDena * 100.0).roundToLong()
        }

        val totalLena = khataLenaPaise / 100.0
        val totalDena = khataDenaPaise / 100.0
        val netBalance = (khataLenaPaise - khataDenaPaise) / 100.0

        db.collection("khatas").document(khataId).update(
            mapOf(
                "totalPeople" to totalPeople,
                "totalLena" to totalLena,
                "totalDena" to totalDena,
                "netBalance" to netBalance,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    // --- ACTIVITY HISTORY ---

    fun observeKhataHistory(khataId: String): Flow<List<KhataHistory>> {
        if (auth.currentUser == null) return emptyFlow()
        return db.collection("khatas").document(khataId)
            .collection("history")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(KhataHistory::class.java)?.let { history ->
                        if (history.id.isBlank()) history.copy(id = doc.id) else history
                    }
                }
            }
            .catch { e ->
                handleFirestoreError(e as Exception, OperationType.LIST, "khatas/$khataId/history")
                throw e
            }
    }

    private suspend fun logHistory(
        khataId: String,
        actionType: String,
        description: String,
        amount: Double? = null,
        personName: String? = null,
        performedByUid: String,
        performedByName: String
    ) {
        try {
            val historyRef = db.collection("khatas").document(khataId).collection("history").document()
            val historyData = mutableMapOf<String, Any>(
                "id" to historyRef.id,
                "khataId" to khataId,
                "actionType" to actionType,
                "description" to description,
                "performedByUid" to performedByUid,
                "performedByName" to performedByName,
                "timestamp" to FieldValue.serverTimestamp()
            )
            if (amount != null) historyData["amount"] = amount
            if (personName != null) historyData["personName"] = personName

            historyRef.set(historyData).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "khatas/$khataId/history")
        }
    }

    // --- SHARING & PARTNER SYSTEM ---

    fun observeIncomingShareRequests(): Flow<List<ShareRequest>> {
        val uid = auth.currentUser?.uid ?: return emptyFlow()
        return db.collection("shareRequests")
            .whereEqualTo("toUid", uid)
            .whereEqualTo("status", "PENDING")
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(ShareRequest::class.java)?.let { req ->
                        if (req.id.isBlank()) req.copy(id = doc.id) else req
                    }
                }
            }
            .catch { e ->
                handleFirestoreError(e as Exception, OperationType.LIST, "shareRequests")
                throw e
            }
    }

    fun observeOutgoingShareRequests(): Flow<List<ShareRequest>> {
        val uid = auth.currentUser?.uid ?: return emptyFlow()
        return db.collection("shareRequests")
            .whereEqualTo("fromUid", uid)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(ShareRequest::class.java)?.let { req ->
                        if (req.id.isBlank()) req.copy(id = doc.id) else req
                    }
                }
            }
            .catch { e ->
                handleFirestoreError(e as Exception, OperationType.LIST, "shareRequests")
                throw e
            }
    }

    suspend fun sendShareRequest(
        khataId: String,
        khataName: String,
        targetUser: UserProfile,
        senderName: String,
        senderCustomId: String
    ): Result<String> {
        return try {
            val uid = requireUserId()
            if (targetUser.uid == uid) {
                return Result.failure(IllegalArgumentException("You cannot share a khata with yourself."))
            }

            // Check if already a member
            val khataDoc = db.collection("khatas").document(khataId).get().await()
            val memberUids = khataDoc.get("memberUids") as? List<*> ?: emptyList<String>()
            if (memberUids.contains(targetUser.uid)) {
                return Result.failure(IllegalArgumentException("This user is already an authorized partner for this khata."))
            }

            val reqRef = db.collection("shareRequests").document()
            val reqId = reqRef.id

            val reqData = mapOf(
                "id" to reqId,
                "khataId" to khataId,
                "khataName" to khataName,
                "fromUid" to uid,
                "fromName" to senderName,
                "fromCustomUserId" to senderCustomId,
                "toUid" to targetUser.uid,
                "toName" to targetUser.name,
                "toCustomUserId" to targetUser.customUserId,
                "status" to "PENDING",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            reqRef.set(reqData).await()
            Result.success(reqId)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "shareRequests")
            Result.failure(e)
        }
    }

    suspend fun respondToShareRequest(request: ShareRequest, accept: Boolean, userName: String): Result<Unit> {
        return try {
            val uid = requireUserId()
            val reqRef = db.collection("shareRequests").document(request.id)

            if (accept) {
                val batch = db.batch()
                batch.update(reqRef, mapOf(
                    "status" to "ACCEPTED",
                    "updatedAt" to FieldValue.serverTimestamp()
                ))

                // Add to khata memberUids and members map
                val khataRef = db.collection("khatas").document(request.khataId)
                batch.update(khataRef, mapOf(
                    "memberUids" to FieldValue.arrayUnion(uid),
                    "members.${uid}" to userName,
                    "updatedAt" to FieldValue.serverTimestamp()
                ))
                batch.commit().await()

                logHistory(
                    khataId = request.khataId,
                    actionType = "SHARE_ACCEPTED",
                    description = "$userName accepted share request for khata '${request.khataName}'",
                    performedByUid = uid,
                    performedByName = userName
                )
            } else {
                reqRef.update(
                    mapOf(
                        "status" to "REJECTED",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()

                logHistory(
                    khataId = request.khataId,
                    actionType = "SHARE_REJECTED",
                    description = "$userName rejected share request for khata '${request.khataName}'",
                    performedByUid = uid,
                    performedByName = userName
                )
            }

            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "shareRequests/${request.id}")
            Result.failure(e)
        }
    }

    suspend fun stopSharing(
        khataId: String,
        partnerUid: String,
        partnerName: String,
        ownerName: String
    ): Result<Unit> {
        return try {
            val uid = requireUserId()
            val khataRef = db.collection("khatas").document(khataId)
            val khataDoc = khataRef.get().await()

            val ownerUid = khataDoc.getString("ownerUid")
            if (ownerUid != uid) {
                return Result.failure(IllegalStateException("Only the khata owner can remove partners."))
            }

            // Remove partner from memberUids and members
            khataRef.update(
                mapOf(
                    "memberUids" to FieldValue.arrayRemove(partnerUid),
                    "members.${partnerUid}" to FieldValue.delete(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            logHistory(
                khataId = khataId,
                actionType = "SHARE_STOPPED",
                description = "$ownerName stopped sharing with $partnerName",
                performedByUid = uid,
                performedByName = ownerName
            )

            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "khatas/$khataId")
            Result.failure(e)
        }
    }
}
