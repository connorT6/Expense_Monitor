package com.connort6.expensemonitor.repo

import com.connort6.expensemonitor.mainCollection
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import kotlinx.coroutines.flow.asStateFlow

data class SMSOperator(
    @DocumentId override var id: String = "",
    val address: String = "",
    @ServerTimestamp override var lastUpdated: Timestamp? = null,
    override val deleted: Boolean = false,
) : BaseEntity

data class SMSParser(
    @DocumentId override var id: String = "",
    val smsOperatorId: String = "",
    val accountId: String = "",
    val pattern: String = "",
    val transactionType: TransactionType = TransactionType.CREDIT,
    @ServerTimestamp override var lastUpdated: Timestamp? = null,
    override val deleted: Boolean = false,
) : BaseEntity

enum class SMSParseKeys(val matchingRegex: String) {
    AMOUNT("\\d+\\.\\d{2}");
}

class SMSOperatorRepo private constructor() : MainRepository<SMSOperator>(
    SMSOperator::class.java,
    { it.lastUpdated },
    { list -> list.sortedBy { it.address } }
) {
    private val mainDocRef = mainCollection.document("smsOperator")
    override var collection = mainDocRef.collection("operators")


    val operators = _allData.asStateFlow()

    companion object {
        @Volatile
        private var instance: SMSOperatorRepo? = null

        fun getInstance(): SMSOperatorRepo = instance ?: synchronized(this) {
            instance ?: SMSOperatorRepo().also { instance = it }
        }
    }

    init {
        loadAll()
    }

    suspend fun findByAddress(address: String): SMSOperator? {
        return findByQuery { it.whereEqualTo(SMSOperator::address.name, address) }
    }

    suspend fun findOrCreateByAddress(address: String): SMSOperator {
        return findByQuery { it.whereEqualTo(SMSOperator::address.name, address) }
            ?: saveOrUpdate(SMSOperator(address = address))
    }

}

class SMSParserRepo private constructor() :
    MainRepository<SMSParser>(SMSParser::class.java, { it.lastUpdated }) {
    private val mainDocRef = mainCollection.document("smsParser")
    override var collection = mainDocRef.collection("Parsers")


    val parsers = _allData.asStateFlow()

    companion object {
        @Volatile
        private var instance: SMSParserRepo? = null

        fun getInstance(): SMSParserRepo = instance ?: synchronized(this) {
            instance ?: SMSParserRepo().also { instance = it }
        }
    }

    init {
        loadAll()
    }

    suspend fun findBySmsOperatorId(smsOperatorId: String): List<SMSParser> {
        return findAllByQuery { it.whereEqualTo(SMSParser::smsOperatorId.name, smsOperatorId) }
    }

}
