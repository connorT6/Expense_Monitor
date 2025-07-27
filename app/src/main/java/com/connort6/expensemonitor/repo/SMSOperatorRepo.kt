package com.connort6.expensemonitor.repo

import com.connort6.expensemonitor.mainCollection
import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp
import kotlinx.coroutines.flow.asStateFlow

data class SMSOperator(
    override val id: String = "",
    val address: String = "",
    val parsers: List<SMSParser> = listOf(),
    @ServerTimestamp override val lastUpdated: Timestamp? = null,
    override val deleted: Boolean = false,
) : BaseEntity

data class SMSParser(
    val accountId: String = "",
    val pattern: String = "",
    val transactionType: TransactionType = TransactionType.CREDIT
)

enum class SMSParseKeys(val matchingRegex: String) {
    AMOUNT("\\d+\\.\\d{2}");
}

class SMSOperatorRepo private constructor() : MainRepository<SMSOperator>(
    SMSOperator::class.java,
    { it.lastUpdated },
    { list -> list.sortedBy { it.address } }
) {
    private val mainDocRef = mainCollection.document("smsOperator")
    private val collection = mainDocRef.collection("operators")


    val operators = _allData.asStateFlow()

    init {
        loadAll()
    }

    fun loadAll() {
        super.loadAll(collection)
    }

}