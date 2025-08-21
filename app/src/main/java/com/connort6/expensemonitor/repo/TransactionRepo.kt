package com.connort6.expensemonitor.repo

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.ServerTimestamp
import kotlinx.coroutines.flow.asStateFlow


data class Transaction(
    val accountId: String = "",
    val categoryId: String =  "",
    val amount: Double = 0.0,
    val transactionType: TransactionType = TransactionType.CREDIT,
    val isSwap: Boolean = false,
    val swapAccountId: String? = null,
    val createdTime: Timestamp = Timestamp.now(),
    val smsId: String? = null,
    val remark: String? = null,
    @ServerTimestamp override var lastUpdated: Timestamp? = null,
    override var deleted: Boolean = false,
    @get:Exclude
    var account: Account? = null,
    @get:Exclude
    var category: Category? = null,
    @DocumentId
    override var id: String = ""
) : BaseEntity {


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
    { it.lastUpdated }, "transaction", "transactions",
    { list -> list.sortedByDescending { it.createdTime } }
) {
    val transactions = _allData.asStateFlow()

    companion object {
        @Volatile
        private var instance: TransactionRepo? = null
        fun getInstance(): TransactionRepo = instance ?: synchronized(this) {
            instance ?: TransactionRepo().also { instance = it }
        }
    }

}