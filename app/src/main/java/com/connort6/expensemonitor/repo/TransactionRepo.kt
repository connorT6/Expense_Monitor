package com.connort6.expensemonitor.repo

import android.util.Log
import com.connort6.expensemonitor.mainCollection
import com.google.firebase.Timestamp
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ServerTimestamp
import com.google.firebase.firestore.Source
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.tasks.await


data class Transaction(
    val accountId: String,
    val categoryId: String,
    val amount: Double,
    val transactionType: TransactionType,
    val isSwap: Boolean = false,
    val swapAccountId: String? = null,
    val createdTime: Timestamp = Timestamp.now(),
    val smsId: String? = null,
    @ServerTimestamp val lastUpdated: Timestamp? = null,
    val deleted: Boolean = false,
    @get:Exclude
    var account: Account? = null,
    var docId: String = ""
) {

    constructor() : this("", "", 0.0, TransactionType.DEBIT)

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

    private val _transactions = MutableStateFlow<List<Transaction>>(mutableListOf())
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
                    checkDataAvailable()
                    return@addOnSuccessListener
                }
                listenToChanges(sortByUpTime.first().lastUpdated ?: Timestamp.now())
                _transactions.update {
                    sortByUpTime.sortedByDescending { it.createdTime }
                }
            }
    }

    private fun checkDataAvailable() {
        val snapshot =
            collection.whereEqualTo(Transaction::deleted.name, false)
                .orderBy(Transaction::lastUpdated.name, Query.Direction.DESCENDING).get(Source.SERVER)
        snapshot.let { it ->
            it.addOnSuccessListener { querySnapshot ->
                val accountsOrderedUpTime =
                    querySnapshot.toObjects(Transaction::class.java) // accounts orders by last updated time
                if (accountsOrderedUpTime.isNotEmpty()) {
                    listenToChanges(accountsOrderedUpTime.first().lastUpdated ?: Timestamp.now())
                    val sortedByDescending = accountsOrderedUpTime.sortedByDescending { it.createdTime }
                    _transactions.value = sortedByDescending
                } else {
                    listenToChanges(Timestamp.now())
                }
            }
        }
    }

    suspend fun getByQuery(queryBuilder: (CollectionReference) -> Query): Set<Transaction> {
        val query = queryBuilder(collection)
        return query.get(Source.CACHE).await().toObjects(Transaction::class.java).toSet()
    }


    fun saveTransactionTransactional(
        transaction: Transaction,
        tr: com.google.firebase.firestore.Transaction
    ) {
        val docRef = collection.document()
        tr.set(docRef, transaction.copy(docId = docRef.id))
    }

    suspend fun createTransaction(transaction: Transaction) {

        if (transaction.smsId != null) {
            val existing =
                collection.whereEqualTo(Transaction::smsId.name, transaction.smsId).get().await()
                    .toObjects(Transaction::class.java)
            if (existing.isNotEmpty()) {
                return
            }
        }

        val docRef = collection.document()
        docRef.set(transaction.copy(docId = docRef.id)).await()
    }

    suspend fun getDocRef(): DocumentReference {
        return collection.document()
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
                val sortByUpTime = value.toObjects(Transaction::class.java)
                if (sortByUpTime.isEmpty()) {
                    return@addSnapshotListener
                }
                val updatedIds = sortByUpTime.map { it.docId }

                _transactions.update { transactions ->
                    transactions.filter { it.docId !in updatedIds }
                        .plus(sortByUpTime)
                        .sortedByDescending { it.createdTime }
                }

            }
    }

}