package com.connort6.expensemonitor.repo

import android.util.Log
import com.connort6.expensemonitor.mainCollection
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ServerTimestamp
import com.google.firebase.firestore.Source
import jakarta.inject.Singleton
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

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
    { it.lastUpdated },
    { list -> list.sortedByDescending { it.order } }
) {
    private val accDoc = mainCollection.document("accounts")
    override var collection = accDoc.collection("Accounts")
    val accountFlow = _allData.asStateFlow()

    init {
        loadAll()
    }
    suspend fun createAccount(account: Account): String {
        val existing =
            collection.whereEqualTo(Account::name.name, account.name).get(Source.CACHE).await()
        if (!existing.isEmpty) {
            return existing.toObjects(Account::class.java).first().id
        }

        Log.d("REPO", "createAccount: ")

        val db = FirebaseFirestore.getInstance()
        return db.runTransaction({ transaction ->
            val docRef = collection.document()
            transaction.set(docRef, account.copy(id = docRef.id))
            docRef.id
        }).await()
    }

    suspend fun deleteAccount(accId: String) {
        val db = FirebaseFirestore.getInstance()
        db.runTransaction({ transaction ->

            val docRef = collection.document(accId)
            val deletingAcc = transaction.get(docRef).toObject(Account::class.java)

            if (deletingAcc == null) {
                return@runTransaction
            }

            transaction.set(docRef, deletingAcc.copy(deleted = true, lastUpdated = null))

            docRef.id
        }).await()
        Log.d("REPO", "deleteAccount: ")
    }


    companion object {
        @Volatile
        private var instance: AccountRepo? = null

        fun getInstance(): AccountRepo = instance ?: synchronized(this) {
            instance ?: AccountRepo().also { instance = it }
        }
    }

}