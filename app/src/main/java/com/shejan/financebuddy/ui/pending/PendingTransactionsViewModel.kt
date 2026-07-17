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

    /** All pending SMS-detected transactions, ordered newest first. */
    val pendingList: StateFlow<List<PendingSmsTransactionEntity>> =
        pendingSmsDao.getAllPending()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Badge count for the bottom nav / notification dot. */
    val pendingCount: StateFlow<Int> =
        pendingSmsDao.getPendingCount()
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
     * and removes it from the pending queue.
     *
     * @param pending     The pending entry to confirm (possibly with user edits already applied).
     * @param edited      An optional override of the pending entry if the user edited it in the sheet.
     */
    fun confirm(pending: PendingSmsTransactionEntity, edited: PendingSmsTransactionEntity = pending) {
        viewModelScope.launch(Dispatchers.IO) {
            val transaction = TransactionEntity(
                amount        = edited.amount,
                type          = edited.type,
                category      = edited.category,
                timestamp     = edited.timestamp,
                fromAccountId = edited.fromAccountId,
                toAccountId   = edited.toAccountId,
                note          = edited.note
            )
            transactionDao.insertTransaction(transaction)  // also adjusts account balances
            pendingSmsDao.deletePending(pending)
        }
    }

    /**
     * Dismisses a pending entry without saving anything.
     */
    fun dismiss(pending: PendingSmsTransactionEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            pendingSmsDao.deletePending(pending)
        }
    }

    /**
     * Saves edits the user made to a pending entry (before confirming).
     */
    fun update(updated: PendingSmsTransactionEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            pendingSmsDao.updatePending(updated)
        }
    }

    /**
     * Dismiss all pending entries at once.
     */
    fun dismissAll() {
        viewModelScope.launch(Dispatchers.IO) {
            pendingSmsDao.clearAll()
        }
    }
}
