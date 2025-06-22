package com.connort6.expensemonitor.repo

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ServerTimestamp
import com.google.firebase.firestore.Source
import jakarta.inject.Singleton
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.tasks.await

data class Account(
    var id: String = "",
    val name: String = "",
    var balance: Double = 0.0,
    var deleted: Boolean = false,
    @ServerTimestamp
    val lastUpdated: Timestamp? = null,
    val order: Int = 0
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Account

        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}

@Singleton
class AccountRepo private constructor(
) {
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val collection = firestore.collection("asd")

    private val _accounts = MutableStateFlow<Set<Account>>(mutableSetOf())

    val accountFlow = _accounts.asStateFlow()

    init {
        getAllAccounts()
    }

    suspend fun createAccount(account: Account): String {
        delay(1000)
        val existing = collection.whereEqualTo(Account::name.name, account.name).get().await()
        if (!existing.isEmpty) {
            return existing.toObjects(Account::class.java).first().id
        }
        val document = collection.add(account).await()
        collection.document(document.id).update("id", document.id).await()
        Log.d("REPO", "createAccount: ")
        return document.id
    }

    suspend fun update(account: Account): String? {
        val updated = collection.document(account.id).set(account).await()
        return account.id
    }

    private fun getAllAccounts() {
        Log.d("REPO", "getAllAccounts: ")
        val snapshot = collection.whereEqualTo(Account::deleted.name, false)
            .orderBy(Account::lastUpdated.name, Query.Direction.DESCENDING)
            .get(Source.CACHE)
        snapshot.let { it ->
            it.addOnSuccessListener { querySnapshot ->
                val sortedByDescending = querySnapshot.toObjects(Account::class.java)
                    .sortedByDescending { it.order }
                _accounts.value = sortedByDescending.toSet()
                if (sortedByDescending.isNotEmpty()) {
                    listenToChanges(sortedByDescending.first().lastUpdated ?: Timestamp.now())
                } else {
                    listenToChanges(Timestamp.now())
                }
            }
            it.addOnFailureListener {
                Log.d("REPO", "getAllAccounts: ${it.message}")
            }
        }
    }

    private fun listenToChanges(lastUpdated: Timestamp) {
        collection.whereGreaterThan(Account::lastUpdated.name, lastUpdated)
            .orderBy(Account::lastUpdated.name, Query.Direction.DESCENDING)
            .addSnapshotListener { value, error ->
                if (error != null) {
                    Log.d("REPO", "listenToChanges: ${error.message}")
                    return@addSnapshotListener
                }
                value?.let { updated ->
                    val elements = updated.toObjects(Account::class.java).filter { it.id.isNotEmpty() }.toSet()
                    if (elements.isEmpty()) {
                        return@addSnapshotListener
                    }
                    _accounts.update { accountList ->
                        accountList.toMutableSet().apply {
                            removeAll(elements)
                            addAll(elements.filter { !it.deleted })
                            sortedByDescending { it.order }
                        }
                    }
                    listenToChanges(elements.first().lastUpdated ?: Timestamp.now())
                }
            }
    }

    suspend fun deleteAccount(accId: String) {
        collection.document(accId).update(Account::deleted.name, true).await()
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