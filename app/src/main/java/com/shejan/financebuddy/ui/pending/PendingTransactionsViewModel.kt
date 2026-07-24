package com.shejan.financebuddy.ui.pending

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shejan.financebuddy.data.db.FinanceDatabase
import com.shejan.financebuddy.data.db.PendingSmsTransactionEntity
import com.shejan.financebuddy.data.db.TransactionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PendingTransactionsViewModel(private val database: FinanceDatabase) : ViewModel() {

    private val pendingSmsDao  = database.pendingSmsDao()
    private val transactionDao = database.transactionDao()

    /** All pending SMS-detected transactions (status = PENDING), ordered newest first. */
    val pendingList: StateFlow<List<PendingSmsTransactionEntity>> =
        pendingSmsDao.getAllPending()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Confirmed SMS transactions (status = CONFIRMED). */
    val confirmedList: StateFlow<List<PendingSmsTransactionEntity>> =
        pendingSmsDao.getConfirmedList()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Dismissed SMS transactions (status = DISMISSED). */
    val dismissedList: StateFlow<List<PendingSmsTransactionEntity>> =
        pendingSmsDao.getDismissedList()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Badge count for pending items. */
    val pendingCount: StateFlow<Int> =
        pendingSmsDao.getPendingCount()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    /** Count for confirmed items. */
    val confirmedCount: StateFlow<Int> =
        pendingSmsDao.getConfirmedCount()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    /** Count for dismissed items. */
    val dismissedCount: StateFlow<Int> =
        pendingSmsDao.getDismissedCount()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    /** SMS Sender Mappings flow */
    val mappingsList: StateFlow<List<com.shejan.financebuddy.data.db.SmsSenderMappingEntity>> =
        database.smsSenderMappingDao().getAllMappingsFlow()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _potentialSenders = kotlinx.coroutines.flow.MutableStateFlow<List<com.shejan.financebuddy.sms.PotentialSender>>(emptyList())
    val potentialSenders: StateFlow<List<com.shejan.financebuddy.sms.PotentialSender>> = _potentialSenders

    fun loadPotentialSenders(context: android.content.Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val senders = com.shejan.financebuddy.sms.SmsSyncHelper.findPotentialUnknownSenders(context, database)
            _potentialSenders.value = senders
        }
    }

    fun addMapping(senderAddress: String, accountId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            database.smsSenderMappingDao().insertMapping(
                com.shejan.financebuddy.data.db.SmsSenderMappingEntity(
                    senderAddress = senderAddress.lowercase().trim(),
                    accountId = accountId
                )
            )
        }
    }

    fun deleteMapping(mapping: com.shejan.financebuddy.data.db.SmsSenderMappingEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            database.smsSenderMappingDao().deleteMapping(mapping)
        }
    }

    fun syncSenderHistory(context: android.content.Context, senderAddress: String, accountId: Int, onComplete: (Int) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val count = com.shejan.financebuddy.sms.SmsSyncHelper.syncPreviousSmsForSender(context, database, senderAddress, accountId)
            launch(Dispatchers.Main) {
                onComplete(count)
            }
        }
    }

    /**
     * Confirms a pending entry: inserts it as a real transaction (updating balances)
     * and updates its status to "CONFIRMED".
     */
    fun confirm(pending: PendingSmsTransactionEntity, edited: PendingSmsTransactionEntity = pending) {
        viewModelScope.launch(Dispatchers.IO) {
            val updatedPending = edited.copy(status = "CONFIRMED")
            val transaction = TransactionEntity(
                amount        = updatedPending.amount,
                type          = updatedPending.type,
                category      = updatedPending.category,
                timestamp     = updatedPending.timestamp,
                fromAccountId = updatedPending.fromAccountId,
                toAccountId   = updatedPending.toAccountId,
                note          = updatedPending.note
            )
            transactionDao.insertTransaction(transaction)  // also adjusts account balances
            pendingSmsDao.updatePending(updatedPending)
        }
    }

    /**
     * Dismisses a pending entry by marking status = "DISMISSED".
     */
    fun dismiss(pending: PendingSmsTransactionEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            pendingSmsDao.updateStatus(pending.id, "DISMISSED")
        }
    }

    /**
     * Restores a dismissed or confirmed entry back to "PENDING".
     */
    fun restore(pending: PendingSmsTransactionEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            pendingSmsDao.updateStatus(pending.id, "PENDING")
        }
    }

    /**
     * Permanently deletes a pending entry.
     */
    fun deletePermanently(pending: PendingSmsTransactionEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            pendingSmsDao.deletePending(pending)
        }
    }

    /**
     * Saves edits the user made to a pending entry.
     */
    fun update(updated: PendingSmsTransactionEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            pendingSmsDao.updatePending(updated)
        }
    }

    /**
     * Marks all currently pending entries as DISMISSED.
     */
    fun dismissAll() {
        viewModelScope.launch(Dispatchers.IO) {
            pendingSmsDao.dismissAllPending()
        }
    }

    /**
     * Confirms all currently pending entries in batch.
     */
    fun confirmAll() {
        viewModelScope.launch(Dispatchers.IO) {
            val items = pendingList.value
            items.forEach { item ->
                val updated = item.copy(status = "CONFIRMED")
                val transaction = TransactionEntity(
                    amount        = updated.amount,
                    type          = updated.type,
                    category      = updated.category,
                    timestamp     = updated.timestamp,
                    fromAccountId = updated.fromAccountId,
                    toAccountId   = updated.toAccountId,
                    note          = updated.note
                )
                transactionDao.insertTransaction(transaction)
                pendingSmsDao.updatePending(updated)
            }
        }
    }
}
