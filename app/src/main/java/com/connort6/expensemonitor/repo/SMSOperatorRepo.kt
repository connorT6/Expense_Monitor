package com.connort6.expensemonitor.repo

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import kotlinx.coroutines.flow.asStateFlow

data class SMSOperator(
    @DocumentId override var id: String = "",
    val address: String = "",
    @ServerTimestamp override var lastUpdated: Timestamp? = null,
    override var deleted: Boolean = false,
) : BaseEntity

data class SMSParser(
    @DocumentId override var id: String = "",
    val smsOperatorId: String = "",
    val accountId: String = "",
    val pattern: String = "",
    val transactionType: TransactionType = TransactionType.CREDIT,
    @ServerTimestamp override var lastUpdated: Timestamp? = null,
    override var deleted: Boolean = false,
) : BaseEntity

enum class SMSParseKeys(val matchingRegex: String, val remove: String) {
    AMOUNT("[\\d,]+\\.\\d{2}", ",");
}

class SMSOperatorRepo private constructor() : MainRepository<SMSOperator>(
    SMSOperator::class.java,
    { it.lastUpdated }, "smsOperator", "operators",
    { list -> list.sortedBy { it.address } }
) {

    val operators = _allData.asStateFlow()

    companion object {
        @Volatile
        private var instance: SMSOperatorRepo? = null

        fun getInstance(): SMSOperatorRepo = instance ?: synchronized(this) {
            instance ?: SMSOperatorRepo().also { instance = it }
        }
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
    MainRepository<SMSParser>(SMSParser::class.java, { it.lastUpdated }, "smsParser", "Parsers") {

    val parsers = _allData.asStateFlow()

    companion object {
        @Volatile
        private var instance: SMSParserRepo? = null

        fun getInstance(): SMSParserRepo = instance ?: synchronized(this) {
            instance ?: SMSParserRepo().also { instance = it }
        }
    }

    suspend fun findBySmsOperatorId(smsOperatorId: String): List<SMSParser> {
        return findAllByQuery { it.whereEqualTo(SMSParser::smsOperatorId.name, smsOperatorId) }
    }

    suspend fun findAllByAccountId(accountId: String): List<SMSParser> {
        return findAllByQuery { it.whereEqualTo(SMSParser::accountId.name, accountId) }
    }

    suspend fun findAllByOperatorIdAndAccountId(
        smsOperatorId: String,
        accountId: String
    ): List<SMSParser> {
        return findAllByQuery {
            it.whereEqualTo(SMSParser::smsOperatorId.name, smsOperatorId)
                .whereEqualTo(SMSParser::accountId.name, accountId)
        }
    }

}
