package com.connort6.expensemonitor.repo

import com.connort6.expensemonitor.mainCollection
import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ServerTimestamp
import com.google.firebase.firestore.Source
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class Transaction(
    val accountId: String,
    val amount: Double,
    val transactionType: TransactionType,
    val isSwap: Boolean = false,
    val swapAccountId: String? = null,
    val createdTime: Timestamp = Timestamp.now(),
    @ServerTimestamp val lastUpdated: Timestamp? = null,
    val deleted: Boolean = false,
    @get:Exclude
    var account: Account? = null,
    var docId: String = ""
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Transaction
        if (docId.isEmpty()) {
            return lastUpdated == other.lastUpdated
        }
        return docId == other.docId
    }

    override fun hashCode(): Int {
        if (docId.isEmpty()) {
            return super.hashCode()
        }
        return docId.hashCode()
    }
}

enum class TransactionType {
    CREDIT,
    DEBIT
}


class TransactionRepo private constructor() {

    private val transactionDocRef = mainCollection.document("transaction")
    private val collection = transactionDocRef.collection("transactions")

    private val _transactions = MutableStateFlow<Set<Transaction>>(mutableSetOf())
    val transactions = _transactions.asStateFlow()

    companion object {
        @Volatile
        private var instance: TransactionRepo? = null
        fun getInstance(): TransactionRepo = TransactionRepo.instance ?: synchronized(this) {
            TransactionRepo.instance ?: TransactionRepo().also { TransactionRepo.instance = it }
        }
    }

    init {
        transactionDocRef.get().addOnSuccessListener {
            if (!it.exists()) {
                transactionDocRef.set(ProcessedSmsDetails()).addOnSuccessListener {}
            }
            getAllTransactions()
        }
    }

    private fun getAllTransactions() {
        collection.whereEqualTo(Transaction::deleted.name, false)
            .orderBy(Transaction::lastUpdated.name, Query.Direction.DESCENDING).get(Source.CACHE)
            .addOnSuccessListener { snapshot ->
                val sortByUpTime = snapshot.toObjects(Transaction::class.java)
                if (sortByUpTime.isEmpty()) {
                    listenToChanges(Timestamp.now())
                    return@addOnSuccessListener
                }
                listenToChanges(sortByUpTime.first().lastUpdated ?: Timestamp.now())
                _transactions.update {
                    sortByUpTime.sortedByDescending { it.createdTime }.toSet()
                }
            }
    }


    private fun listenToChanges(timestamp: Timestamp) {
        collection.whereGreaterThan(Transaction::lastUpdated.name, timestamp)
            .orderBy(Transaction::lastUpdated.name, Query.Direction.DESCENDING)
            .addSnapshotListener { value, error ->
                if (error != null) {
                    return@addSnapshotListener
                }
                if (value == null) {
                    return@addSnapshotListener
                }
                val sortByUpTime = value.toObjects(Transaction::class.java).toSet()
                if (sortByUpTime.isNotEmpty()) {
                    _transactions.update { transactions ->
                        transactions.toMutableSet().apply {
                            removeAll(sortByUpTime)
                            addAll(sortByUpTime.filter { !it.deleted })
                            sortedByDescending { it.createdTime }
                        }
                    }
                }
            }
    }

}