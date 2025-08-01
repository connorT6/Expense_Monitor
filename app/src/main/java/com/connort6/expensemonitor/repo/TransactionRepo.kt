package com.connort6.expensemonitor.repo

import com.connort6.expensemonitor.mainCollection
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.ServerTimestamp
import kotlinx.coroutines.flow.asStateFlow
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
    @ServerTimestamp override var lastUpdated: Timestamp? = null,
    override val deleted: Boolean = false,
    @get:Exclude
    var account: Account? = null,
    @get:Exclude
    var category: Category? = null,
    @DocumentId
    override var id: String = ""
) : BaseEntity {

    constructor() : this("", "", 0.0, TransactionType.DEBIT)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Transaction
        if (id.isEmpty()) {
            return lastUpdated == other.lastUpdated
        }
        return id == other.id
    }

    override fun hashCode(): Int {
        if (id.isEmpty()) {
            return super.hashCode()
        }
        return id.hashCode()
    }
}

enum class TransactionType {
    CREDIT,
    DEBIT
}


class TransactionRepo private constructor() : MainRepository<Transaction>(
    Transaction::class.java,
    { it.lastUpdated },
    { list -> list.sortedByDescending { it.createdTime } }
) {

    private val transactionDocRef = mainCollection.document("transaction")
    override var collection = transactionDocRef.collection("transactions")

    val transactions = _allData.asStateFlow()

    companion object {
        @Volatile
        private var instance: TransactionRepo? = null
        fun getInstance(): TransactionRepo = instance ?: synchronized(this) {
            instance ?: TransactionRepo().also { instance = it }
        }
    }

    init {
        transactionDocRef.get().addOnSuccessListener {
            if (!it.exists()) {
                transactionDocRef.set(ProcessedSmsDetails()).addOnSuccessListener {}
            }
            loadAll()
        }
    }



    fun saveTransactionTransactional(
        transaction: Transaction,
        tr: com.google.firebase.firestore.Transaction
    ) {
        val docRef = collection.document()
        tr.set(docRef, transaction)
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
        docRef.set(transaction).await()
    }

    suspend fun getDocRef(): DocumentReference {
        return collection.document()
    }


}