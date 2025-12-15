package com.example.helpinghand.data.household

import com.example.helpinghand.AppLogger
import com.example.helpinghand.data.model.HouseholdMember
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class HouseholdRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val usersCol = db.collection("users")
    private val householdsCol = db.collection("households")

    /**
     * Ensures users/{uid} exists and normalizes email + displayName fields.
     * Returns uid or null if not logged in.
     */
    suspend fun ensureUserDocument(): String? {
        val user = auth.currentUser ?: return null
        val docRef = usersCol.document(user.uid)
        val snapshot = docRef.get().await()

        val email = (user.email ?: "").trim()
        val emailLower = email.lowercase()
        val displayName = (user.displayName ?: "").trim()

        if (!snapshot.exists()) {
            docRef.set(
                mapOf(
                    "email" to email,
                    "emailLower" to emailLower,
                    "displayName" to displayName,
                    "householdId" to null
                )
            ).await()
        } else {
            val updates = mutableMapOf<String, Any>()
            if (snapshot.getString("email") != email) updates["email"] = email
            if (snapshot.getString("emailLower") != emailLower) updates["emailLower"] = emailLower
            if (snapshot.getString("displayName") != displayName) updates["displayName"] = displayName
            if (updates.isNotEmpty()) docRef.update(updates).await()
        }

        return user.uid
    }

    /**
     * Returns existing householdId for current user, or creates a new solo household.
     */
    suspend fun getOrCreateHouseholdId(): String? {
        val uid = ensureUserDocument() ?: return null
        val userRef = usersCol.document(uid)
        val snapshot = userRef.get().await()

        val existing = snapshot.getString("householdId")
        if (!existing.isNullOrBlank()) return existing

        val newDoc = householdsCol.document()
        val nameBase = snapshot.getString("displayName")?.takeIf { it.isNotBlank() } ?: "Household"
        newDoc.set(
            mapOf(
                "name" to "$nameBase's household",
                "members" to listOf(uid)
            )
        ).await()

        userRef.update("householdId", newDoc.id).await()
        return newDoc.id
    }

    /**
     * Live list of members for a household.
     */
    fun observeHouseholdMembers(householdId: String): Flow<List<HouseholdMember>> = callbackFlow {
        val reg = householdsCol.document(householdId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    AppLogger.e(AppLogger.TAG_VM, "observeHouseholdMembers error: ${error.message}", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                if (snapshot == null || !snapshot.exists()) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val memberIds =
                    (snapshot.get("members") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                if (memberIds.isEmpty()) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                db.runTransaction { tx ->
                    memberIds.mapNotNull { uid ->
                        val userSnap = tx.get(usersCol.document(uid))
                        val email = userSnap.getString("email") ?: ""
                        val name = userSnap.getString("displayName")?.takeIf { it.isNotBlank() } ?: email
                        HouseholdMember(uid = uid, displayName = name, email = email)
                    }
                }.addOnSuccessListener { list ->
                    trySend(list)
                }.addOnFailureListener { e ->
                    AppLogger.e(AppLogger.TAG_VM, "observeHouseholdMembers transaction error: ${e.message}", e)
                    trySend(emptyList())
                }
            }

        awaitClose { reg.remove() }
    }

    /**
     * Join an existing household by its id (shareable code).
     * FORCE-sets the current user's householdId to the target household.
     */
    suspend fun joinHousehold(targetHouseholdId: String): Boolean {
        return try {
            val uid = ensureUserDocument() ?: return false
            val hid = targetHouseholdId.trim()
            if (hid.isBlank()) return false

            val targetRef = householdsCol.document(hid)
            val targetSnap = targetRef.get().await()
            if (!targetSnap.exists()) {
                AppLogger.e(AppLogger.TAG_VM, "joinHousehold: target household does not exist: $hid")
                return false
            }

            val userRef = usersCol.document(uid)

            db.runTransaction { tx ->
                // READS FIRST
                val uSnap = tx.get(userRef)
                val oldHouseholdId = uSnap.getString("householdId")

                val oldRef =
                    if (!oldHouseholdId.isNullOrBlank() && oldHouseholdId != hid) householdsCol.document(oldHouseholdId)
                    else null
                val oldSnap = oldRef?.let { tx.get(it) }

                // WRITES AFTER
                tx.update(targetRef, "members", FieldValue.arrayUnion(uid))
                tx.update(userRef, "householdId", hid)

                if (oldRef != null && oldSnap != null && oldSnap.exists()) {
                    tx.update(oldRef, "members", FieldValue.arrayRemove(uid))
                }
            }.await()

            AppLogger.d(AppLogger.TAG_VM, "joinHousehold: SUCCESS uid=$uid -> hid=$hid")
            true
        } catch (e: Exception) {
            AppLogger.e(AppLogger.TAG_VM, "joinHousehold: FAILED: ${e.message}", e)
            false
        }
    }

    /**
     * Leave current household and immediately create a new solo household for the current user.
     * Returns the new householdId, or null if not logged in / failed.
     */
    suspend fun leaveAndCreateSoloHousehold(): String? {
        return try {
            val uid = ensureUserDocument() ?: return null
            val userRef = usersCol.document(uid)

            val oldHouseholdId = userRef.get().await().getString("householdId")

            val newDoc = householdsCol.document()
            newDoc.set(
                mapOf(
                    "name" to "My household",
                    "members" to listOf(uid)
                )
            ).await()

            db.runTransaction { tx ->
                // READS FIRST (only if needed)
                val oldRef =
                    if (!oldHouseholdId.isNullOrBlank() && oldHouseholdId != newDoc.id) householdsCol.document(oldHouseholdId)
                    else null
                val oldSnap = oldRef?.let { tx.get(it) }

                // WRITES AFTER
                if (oldRef != null && oldSnap != null && oldSnap.exists()) {
                    tx.update(oldRef, "members", FieldValue.arrayRemove(uid))
                }
                tx.update(userRef, "householdId", newDoc.id)
            }.await()

            newDoc.id
        } catch (e: Exception) {
            AppLogger.e(AppLogger.TAG_VM, "leaveAndCreateSoloHousehold: FAILED: ${e.message}", e)
            null
        }
    }
}
