package com.example.helpinghand.data.household

import com.example.helpinghand.AppLogger
import com.example.helpinghand.data.model.HouseholdMember
import com.google.firebase.auth.FirebaseAuth
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

    suspend fun ensureUserDocument(): String? {
        val user = auth.currentUser ?: return null
        val docRef = usersCol.document(user.uid)
        val snapshot = docRef.get().await()
        if (!snapshot.exists()) {
            docRef.set(
                mapOf(
                    "email" to (user.email ?: ""),
                    "displayName" to (user.displayName ?: ""),
                    "householdId" to null
                )
            ).await()
        }
        return user.uid
    }

    suspend fun getOrCreateHouseholdId(): String? {
        val uid = ensureUserDocument() ?: return null
        val userRef = usersCol.document(uid)
        val snapshot = userRef.get().await()
        val existing = snapshot.getString("householdId")
        if (existing != null) return existing

        // create new household
        val newDoc = householdsCol.document()
        newDoc.set(
            mapOf(
                "name" to "${snapshot.getString("displayName") ?: "Household"}'s household",
                "members" to listOf(uid)
            )
        ).await()

        userRef.update("householdId", newDoc.id).await()
        return newDoc.id
    }


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
                val memberIds = snapshot.get("members") as? List<*> ?: emptyList<Any>()
                if (memberIds.isEmpty()) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                db.runTransaction { tx ->
                    memberIds.mapNotNull { id ->
                        val uid = id as? String ?: return@mapNotNull null
                        val userSnap = tx.get(usersCol.document(uid))
                        val email = userSnap.getString("email") ?: ""
                        val name = userSnap.getString("displayName") ?: email
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

    suspend fun addMemberByEmail(householdId: String, email: String): Boolean {
        val trimmedEmail = email.trim().lowercase()
        if (trimmedEmail.isBlank()) return false

        // find user by email
        val userQuery = usersCol.whereEqualTo("email", trimmedEmail).get().await()
        if (userQuery.isEmpty) {
            return false
        }
        val userDoc = userQuery.documents.first()
        val uid = userDoc.id

        // add their uid to household.members
        val householdRef = householdsCol.document(householdId)
        db.runTransaction { tx ->
            val snap = tx.get(householdRef)
            val current = snap.get("members") as? List<*> ?: emptyList<Any>()
            if (!current.contains(uid)) {
                val updated = current.toMutableList().apply { add(uid) }
                tx.update(householdRef, "members", updated)
            }
        }.await()

        // update user's householdId if they didn't have one
        if (!userDoc.contains("householdId") || userDoc.getString("householdId") == null) {
            usersCol.document(uid).update("householdId", householdId).await()
        }
        return true
    }
}
