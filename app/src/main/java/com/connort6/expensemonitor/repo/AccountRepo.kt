package com.connort6.expensemonitor.repo

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp
import jakarta.inject.Singleton
import kotlinx.coroutines.flow.asStateFlow

data class Account(
    override var id: String = "",
    val name: String = "",
    var balance: Double = 0.0,
    override var deleted: Boolean = false,
    @ServerTimestamp override var lastUpdated: Timestamp? = null,
    val order: Int = 0,
    val iconName: String = "",
//    val smsSenders: List<SMSOperator> = listOf(),
) : BaseEntity

@Singleton
class AccountRepo private constructor() : MainRepository<Account>(
    Account::class.java,
    { it.lastUpdated }, "accounts", "Accounts",
    { list -> list.sortedByDescending { it.order } }
) {
    val accountFlow = _allData.asStateFlow()

    suspend fun createAccount(account: Account): String {
        val existing = findByQuery { it.whereEqualTo(Account::name.name, account.name) }
        if (existing != null) {
            return existing.id
        }

        Log.d("REPO", "createAccount: ")

        return saveOrUpdate(account).id
    }
    companion object {
        @Volatile
        private var instance: AccountRepo? = null

        fun getInstance(): AccountRepo = instance ?: synchronized(this) {
            instance ?: AccountRepo().also { instance = it }
        }
    }

}