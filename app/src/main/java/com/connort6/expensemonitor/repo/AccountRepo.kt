package com.connort6.expensemonitor.repo

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import jakarta.inject.Singleton
import kotlinx.coroutines.tasks.await

data class Account(
    var id: String = "", val name: String = "", var balance: Double = 0.0, var deleted: Boolean = false
)

@Singleton
class AccountRepo private constructor(
) {
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val collection = firestore.collection("asd")

    suspend fun createAccount(account: Account): String {
        val existing = collection.whereEqualTo(Account::name.name, account.name).get().await()
        if (!existing.isEmpty) {
            return existing.toObjects(Account::class.java).first().id
        }
        val document = collection.add(account).await()
        collection.document(document.id).update("id", document.id).await()
        return document.id
    }

    suspend fun update(account: Account): String? {
        val updated = collection.document(account.id).set(account).await()
        return account.id
    }

    suspend fun getAllAccounts(): List<Account> {
        val snapshot = collection.whereEqualTo(Account::deleted.name, false).get(Source.CACHE).await()
        return snapshot.toObjects(Account::class.java)
    }

    suspend fun deleteAccount(accId: String) {
        collection.document(accId).update(Account::deleted.name, true).await()
    }

    companion object {
        @Volatile
        private var instance: AccountRepo? = null

        fun getInstance(): AccountRepo = instance ?: synchronized(this) {
            instance ?: AccountRepo().also { instance = it }
        }
    }
}