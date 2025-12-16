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

    val items: StateFlow<List<ShoppingItem>> =
        dao.getAllItems()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addItem(name: String) = viewModelScope.launch(Dispatchers.IO) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return@launch
        try {
            syncRepo.addItem(trimmed)
        } catch (e: Exception) {
            AppLogger.e(AppLogger.TAG_DB, "addItem FAILED: ${e.message}", e)
        }
    }

    fun toggleChecked(item: ShoppingItem, checked: Boolean) = viewModelScope.launch(Dispatchers.IO) {
        try {
            syncRepo.toggleChecked(item, checked)
        } catch (e: Exception) {
            AppLogger.e(AppLogger.TAG_DB, "toggleChecked FAILED: ${e.message}", e)
        }
    }

    fun deleteChecked() = viewModelScope.launch(Dispatchers.IO) {
        try {
            syncRepo.deleteChecked()
        } catch (e: Exception) {
            AppLogger.e(AppLogger.TAG_DB, "deleteChecked FAILED: ${e.message}", e)
        }
    }


    override fun onCleared() {
        super.onCleared()
        syncRepo.clear()
    }

    fun onHouseholdIdChanged(newHouseholdId: String?) {
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

class ShoppingSyncRepository(
    private val dao: ShoppingItemDao,
    private val householdRepo: HouseholdRepository = HouseholdRepository(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    companion object { private const val TAG = "ShoppingSyncRepository" }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val householdsCol = db.collection("households")

    private var listener: ListenerRegistration? = null
    private var cachedHouseholdId: String? = null

    private suspend fun ensureHouseholdAndListener(): String? {
        if (cachedHouseholdId != null && listener != null) return cachedHouseholdId
        return try {
            val hid = householdRepo.getOrCreateHouseholdId()
            if (hid.isNullOrBlank()) null
            else {
                cachedHouseholdId = hid
                startListening(hid)
                hid
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "ensureHouseholdAndListener FAILED: ${e.message}", e)
            null
        }
    }

    private fun startListening(householdId: String) {
        listener?.remove()
        listener = householdsCol.document(householdId)
            .collection("shopping_items")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener

                val items = snapshot.documents.mapNotNull { doc ->
                    val text = doc.getString("text") ?: return@mapNotNull null
                    val checked = doc.getBoolean("isChecked") ?: false
                    ShoppingItem(id = doc.id, text = text, isChecked = checked)
                }

                // IMPORTANT: do not deleteAll here, it wipes local-only inserts (like meals-added items).
                scope.launch {
                    try { dao.insertAll(items) }
                    catch (e: Exception) { AppLogger.e(TAG, "listener upsert FAILED: ${e.message}", e) }
                }
            }
    }

    suspend fun addItem(text: String) {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return

        val localId = UUID.randomUUID().toString()

        // Local first
        dao.insertAll(listOf(ShoppingItem(id = localId, text = trimmed, isChecked = false)))

        // Best-effort remote
        val hid = ensureHouseholdAndListener() ?: return
        try {
            householdsCol.document(hid)
                .collection("shopping_items")
                .document(localId)
                .set(mapOf("text" to trimmed, "isChecked" to false))
                .await()
        } catch (e: Exception) {
            AppLogger.e(TAG, "addItem Firestore FAILED: ${e.message}", e)
        }
    }

    suspend fun toggleChecked(item: ShoppingItem, checked: Boolean) {
        // Local first so UI always updates
        dao.update(item.copy(isChecked = checked))

        val hid = ensureHouseholdAndListener() ?: return
        try {
            householdsCol.document(hid)
                .collection("shopping_items")
                .document(item.id)
                .update("isChecked", checked)
                .await()
        } catch (e: Exception) {
            AppLogger.e(TAG, "toggleChecked Firestore FAILED: ${e.message}", e)
        }
    }

    suspend fun deleteChecked() {
        // Grab checked ids before deleting locally
        val checkedLocal = dao.getAllItemsNow().filter { it.isChecked }

        // Local first so button always works
        dao.deleteChecked()

        val hid = ensureHouseholdAndListener() ?: return
        try {
            val col = householdsCol.document(hid).collection("shopping_items")
            checkedLocal.forEach { item ->
                col.document(item.id).delete().await()
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "deleteChecked Firestore FAILED: ${e.message}", e)
        }
    }

    fun clear() {
        listener?.remove()
        listener = null
        cachedHouseholdId = null
    }

    fun setHouseholdId(newHouseholdId: String?) {
        val normalized = newHouseholdId?.trim()?.takeIf { it.isNotBlank() }
        if (normalized == cachedHouseholdId) return
        listener?.remove()
        listener = null
        cachedHouseholdId = normalized
        if (normalized != null) startListening(normalized)
    }
}
