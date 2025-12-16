package com.example.helpinghand.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.helpinghand.AppLogger
import com.example.helpinghand.data.dao.ShoppingItemDao
import com.example.helpinghand.data.household.HouseholdRepository
import com.example.helpinghand.data.model.ShoppingItem
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

class ShoppingCartViewModel(
    private val dao: ShoppingItemDao,
    private val syncRepo: ShoppingSyncRepository
) : ViewModel() {

    // Items from Room
    val items: StateFlow<List<ShoppingItem>> =
        dao.getAllItems()
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                emptyList()
            )

    fun addItem(name: String) = viewModelScope.launch {
        val trimmed = name.trim()
        AppLogger.d(
            AppLogger.TAG_VM,
            "addItem called: name=\"$trimmed\""
        )
        if (trimmed.isBlank()) {
            AppLogger.d(
                AppLogger.TAG_VM,
                "addItem: blank name, aborting"
            )
            return@launch
        }

        AppLogger.d(
            AppLogger.TAG_ASYNC,
            "addItem: coroutine started for name=\"$trimmed\" (via ShoppingSyncRepository)"
        )
        try {
            syncRepo.addItem(trimmed)
            AppLogger.d(
                AppLogger.TAG_DB,
                "addItem: successfully delegated to ShoppingSyncRepository for \"$trimmed\""
            )
        } catch (e: Exception) {
            AppLogger.e(
                AppLogger.TAG_DB,
                "addItem: FAILED in syncRepo for name=\"$trimmed\" message=${e.message}",
                e
            )
        } finally {
            AppLogger.d(
                AppLogger.TAG_ASYNC,
                "addItem: coroutine finished for name=\"$trimmed\""
            )
        }
    }

    fun toggleChecked(item: ShoppingItem, checked: Boolean) = viewModelScope.launch {
        AppLogger.d(
            AppLogger.TAG_ASYNC,
            "toggleChecked: coroutine started for id=${item.id}, checked=$checked (via ShoppingSyncRepository)"
        )
        try {
            syncRepo.toggleChecked(item, checked)
            AppLogger.d(
                AppLogger.TAG_DB,
                "toggleChecked: successfully delegated to ShoppingSyncRepository for id=${item.id}"
            )
        } catch (e: Exception) {
            AppLogger.e(
                AppLogger.TAG_DB,
                "toggleChecked: FAILED in syncRepo for id=${item.id} message=${e.message}",
                e
            )
        } finally {
            AppLogger.d(
                AppLogger.TAG_ASYNC,
                "toggleChecked: coroutine finished for id=${item.id}"
            )
        }
    }

    fun deleteChecked() = viewModelScope.launch {
        AppLogger.d(
            AppLogger.TAG_ASYNC,
            "deleteChecked: coroutine started (via ShoppingSyncRepository)"
        )
        try {
            syncRepo.deleteChecked()
            AppLogger.d(
                AppLogger.TAG_DB,
                "deleteChecked: successfully delegated to ShoppingSyncRepository"
            )
        } catch (e: Exception) {
            AppLogger.e(
                AppLogger.TAG_DB,
                "deleteChecked: FAILED in syncRepo message=${e.message}",
                e
            )
        } finally {
            AppLogger.d(
                AppLogger.TAG_ASYNC,
                "deleteChecked: coroutine finished"
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        syncRepo.clear()
    }

    fun onHouseholdIdChanged(newHouseholdId: String?) {
        AppLogger.d(AppLogger.TAG_VM, "ShoppingCartViewModel: onHouseholdIdChanged -> $newHouseholdId")
        syncRepo.setHouseholdId(newHouseholdId)
    }

}

class ShoppingCartViewModelFactory(
    private val dao: ShoppingItemDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ShoppingCartViewModel::class.java)) {
            val syncRepo = ShoppingSyncRepository(dao)
            @Suppress("UNCHECKED_CAST")
            return ShoppingCartViewModel(dao, syncRepo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

// -----------------------------
// Firebase Sync implementation
// -----------------------------

class ShoppingSyncRepository(
    private val dao: ShoppingItemDao,
    private val householdRepo: HouseholdRepository = HouseholdRepository(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    companion object {
        private const val TAG = "ShoppingSyncRepository"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val householdsCol = db.collection("households")

    private var listener: ListenerRegistration? = null
    private var cachedHouseholdId: String? = null

    /**
     * Ensure we have a valid householdId and a snapshot listener attached.
     * Called from every public operation so we are not reliant on init timing.
     */
    private suspend fun ensureHouseholdAndListener(): String? {
        // If we already have an id and a listener, we are done
        if (cachedHouseholdId != null && listener != null) {
            return cachedHouseholdId
        }

        return try {
            val hid = householdRepo.getOrCreateHouseholdId()
            if (hid == null) {
                AppLogger.e(
                    TAG,
                    "ensureHouseholdAndListener: householdId is null, cannot start sync",
                    null
                )
                null
            } else {
                AppLogger.d(
                    TAG,
                    "ensureHouseholdAndListener: obtained householdId=$hid, attaching listener"
                )
                cachedHouseholdId = hid
                startListening(hid)
                hid
            }
        } catch (e: Exception) {
            AppLogger.e(
                TAG,
                "ensureHouseholdAndListener: FAILED to resolve householdId message=${e.message}",
                e
            )
            null
        }
    }

    private fun startListening(householdId: String) {
        AppLogger.d(TAG, "startListening: attaching snapshot listener for householdId=$householdId")
        listener?.remove()
        listener = householdsCol.document(householdId)
            .collection("shopping_items")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    AppLogger.e(
                        TAG,
                        "startListening: snapshot error for householdId=$householdId message=${error.message}",
                        error
                    )
                    return@addSnapshotListener
                }
                if (snapshot == null) {
                    AppLogger.e(
                        TAG,
                        "startListening: snapshot is null for householdId=$householdId",
                        null
                    )
                    return@addSnapshotListener
                }

                AppLogger.d(
                    TAG,
                    "startListening: received snapshot with ${snapshot.size()} docs for householdId=$householdId"
                )

                val items = snapshot.documents.mapNotNull { doc ->
                    val text = doc.getString("text") ?: run {
                        AppLogger.e(
                            TAG,
                            "startListening: skipping doc ${doc.id} missing 'text' field",
                            null
                        )
                        return@mapNotNull null
                    }
                    val checked = doc.getBoolean("isChecked") ?: false
                    ShoppingItem(
                        id = doc.id,
                        text = text,
                        isChecked = checked
                    )
                }

                scope.launch {
                    try {
                        AppLogger.d(
                            TAG,
                            "startListening: replacing local shopping_items with ${items.size} items"
                        )
                        dao.deleteAll()
                        if (items.isNotEmpty()) {
                            dao.insertAll(items)
                        }
                    } catch (e: Exception) {
                        AppLogger.e(
                            TAG,
                            "startListening: FAILED to sync snapshot to Room message=${e.message}",
                            e
                        )
                    }
                }
            }
    }

    suspend fun addItem(text: String) {
        val trimmed = text.trim()
        if (trimmed.isBlank()) {
            AppLogger.d(TAG, "addItem: blank text, aborting")
            return
        }

        // --- UI TEST + OFFLINE FRIENDLY BEHAVIOR ---
        // Always insert locally first so UI updates even if Firebase is unavailable.
        // Use a stable id so that if Firestore later succeeds, the snapshot will match.
        val localId = UUID.randomUUID().toString()
        try {
            dao.insertAll(
                listOf(
                    ShoppingItem(
                        id = localId,
                        text = trimmed,
                        isChecked = false
                    )
                )
            )
            AppLogger.d(TAG, "addItem: inserted into Room immediately id=$localId")
        } catch (e: Exception) {
            AppLogger.e(TAG, "addItem: FAILED local Room insert message=${e.message}", e)
            // If Room insert failed, remote write won't help the UI test anyway.
            return
        }

        // Best-effort remote write.
        AppLogger.d(TAG, "addItem: preparing Firestore write for id=$localId text=\"$trimmed\"")
        val hid = ensureHouseholdAndListener()
        if (hid == null) {
            AppLogger.e(TAG, "addItem: householdId is null, keeping local-only item id=$localId", null)
            return
        }

        try {
            val col = householdsCol.document(hid).collection("shopping_items")
            val docRef = col.document(localId)
            val data = mapOf(
                "text" to trimmed,
                "isChecked" to false
            )

            AppLogger.d(TAG, "addItem: setting Firestore doc id=${docRef.id}")
            docRef.set(data).await()
            AppLogger.d(TAG, "addItem: Firestore write success for id=${docRef.id}")
            // Room will update (and normalize) via snapshot listener.
        } catch (e: Exception) {
            AppLogger.e(TAG, "addItem: Firestore write FAILED for id=$localId message=${e.message}", e)
            // Keep the local item. Don't rethrow: tests and offline usage should still work.
        }
    }

    suspend fun toggleChecked(item: ShoppingItem, checked: Boolean) {
        AppLogger.d(
            TAG,
            "toggleChecked: preparing to update isChecked=$checked for id=${item.id}"
        )
        val hid = ensureHouseholdAndListener() ?: return

        try {
            val docRef = householdsCol
                .document(hid)
                .collection("shopping_items")
                .document(item.id)

            docRef.update("isChecked", checked).await()
            AppLogger.d(TAG, "toggleChecked: Firestore update success for id=${item.id}")
        } catch (e: Exception) {
            AppLogger.e(
                TAG,
                "toggleChecked: FAILED Firestore update for id=${item.id} message=${e.message}",
                e
            )
            throw e
        }
    }

    suspend fun deleteChecked() {
        AppLogger.d(TAG, "deleteChecked: preparing to delete checked items from Firestore")
        val hid = ensureHouseholdAndListener() ?: return

        try {
            val col = householdsCol.document(hid).collection("shopping_items")

            val checkedLocal = dao.getAllItemsNow().filter { it.isChecked }
            AppLogger.d(TAG, "deleteChecked: found ${checkedLocal.size} checked items locally")

            checkedLocal.forEach { item ->
                AppLogger.d(TAG, "deleteChecked: deleting Firestore doc id=${item.id}")
                col.document(item.id).delete().await()
            }
            AppLogger.d(TAG, "deleteChecked: finished deleting checked items from Firestore")
            // Listener will clear them out of Room
        } catch (e: Exception) {
            AppLogger.e(TAG, "deleteChecked: FAILED Firestore deletes message=${e.message}", e)
            throw e
        }
    }

    fun clear() {
        AppLogger.d(TAG, "clear: removing snapshot listener and clearing cachedHouseholdId")
        listener?.remove()
        listener = null
        cachedHouseholdId = null
    }

    fun setHouseholdId(newHouseholdId: String?) {
        val normalized = newHouseholdId?.trim()?.takeIf { it.isNotBlank() }

        // No change, do nothing
        if (normalized == cachedHouseholdId) return

        AppLogger.d(TAG, "setHouseholdId: switching shopping sync from $cachedHouseholdId -> $normalized")

        // Kill the old listener
        listener?.remove()
        listener = null

        // Update cache
        cachedHouseholdId = normalized

        // Attach new listener if we have an id
        if (normalized != null) {
            startListening(normalized)
        }
    }

}
