package com.connort6.expensemonitor.repo

import com.google.firebase.firestore.Exclude

data class SMSOperator(
    val address: String = "",
    val parsers: List<SMSParser> = listOf(),
    @get:Exclude val accountId: String = "",
)

data class SMSParser(
    val accountId: String,
    val pattern: String = "",
    val transactionType: TransactionType = TransactionType.CREDIT
)

enum class SMSParseKeys(val matchingRegex: String) {
    AMOUNT("\\d+\\.\\d{2}");
}