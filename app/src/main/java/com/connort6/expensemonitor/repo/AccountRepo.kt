package com.connort6.expensemonitor.repo

import android.util.Log
import com.connort6.expensemonitor.mainCollection
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ServerTimestamp
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import jakarta.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.tasks.await

data class Account(
    var id: String = "",
    val name: String = "",
    var balance: Double = 0.0,
    var deleted: Boolean = false,
    @ServerTimestamp val lastUpdated: Timestamp? = null,
    val order: Int = 0,
    val iconName: String = ""
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Account

        if (id.isEmpty()) {
            return lastUpdated == other.lastUpdated
        }

        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}

data class AccBalUpdate(
    var balance: Double = 0.0,
    @ServerTimestamp val lastUpdated: Timestamp? = null,
)

@Singleton
class AccountRepo private constructor(
) {

    private val accountRef = mainCollection.document("accounts")
    private val collection = accountRef.collection("Accounts")

    private val _accounts = MutableStateFlow<Set<Account>>(mutableSetOf())
    private val _mainAcc = MutableStateFlow(Account())

    val accountFlow = _accounts.asStateFlow()
    val mainAccount = _mainAcc.asStateFlow()

    init {
        accountRef.get().addOnSuccessListener {
            if (!it.exists()) {
                accountRef.set(Account(name = "All")).addOnSuccessListener { }
            }
        }
        getAllAccounts()
    }

    suspend fun createAccount(account: Account): String {
        val existing = collection.whereEqualTo(Account::name.name, account.name).get(Source.CACHE).await()
        if (!existing.isEmpty) {
            return existing.toObjects(Account::class.java).first().id
        }

        Log.d("REPO", "createAccount: ")

        val db = FirebaseFirestore.getInstance()
        return db.runTransaction({ transaction ->

            val docRef = collection.document()
            val mainAcc = transaction.get(accountRef).toObject(Account::class.java)

            val mainBal = (mainAcc?.balance ?: 0.0) + account.balance



            transaction.set(docRef, account.copy(id = docRef.id))
            transaction.set(
                accountRef, AccBalUpdate(mainBal), SetOptions.merge()
            )

            docRef.id
        }).await()
    }

    suspend fun update(account: Account): String? {
        val updated = collection.document(account.id).set(account).await()
        return account.id
    }

    private fun getAllAccounts() {
        Log.d("REPO", "getAllAccounts: ")
        val snapshot =
            collection.whereEqualTo(Account::deleted.name, false).orderBy(Account::lastUpdated.name, Query.Direction.DESCENDING).get(Source.CACHE)


        snapshot.let { it ->
            it.addOnSuccessListener { querySnapshot ->
                val accountsOrderedUpTime = querySnapshot.toObjects(Account::class.java) // accounts orders by last updated time
                if (accountsOrderedUpTime.isNotEmpty()) {
                    listenToChanges(accountsOrderedUpTime.first().lastUpdated ?: Timestamp.now())
                } else {
                    listenToChanges(Timestamp.now())
                }
                val sortedByDescending = accountsOrderedUpTime.sortedByDescending { it.order }
                _accounts.value = sortedByDescending.toSet()
            }
            it.addOnFailureListener {
                Log.d("REPO", "getAllAccounts: ${it.message}")
            }
        }

        accountRef.get(Source.CACHE).addOnSuccessListener { value ->
            if (value == null || !value.exists()) {
                return@addOnSuccessListener
            }
            val account = value.toObject(Account::class.java) ?: return@addOnSuccessListener
            _mainAcc.value = account
        }
    }

    private fun listenToChanges(lastUpdated: Timestamp) {
        collection.whereGreaterThan(Account::lastUpdated.name, lastUpdated).orderBy(Account::lastUpdated.name, Query.Direction.DESCENDING)
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

        accountRef.addSnapshotListener { value, error ->
            if (error != null) {
                Log.d("REPO", "listenToChanges: ${error.message}")
                return@addSnapshotListener
            }

            if (value == null || !value.exists()) {
                return@addSnapshotListener
            }

            val account = value.toObject(Account::class.java) ?: return@addSnapshotListener
            _mainAcc.value = account

        }
    }

    suspend fun deleteAccount(accId: String) {

        val db = FirebaseFirestore.getInstance()
        db.runTransaction({ transaction ->

            val docRef = collection.document(accId)
            val deletingAcc = transaction.get(docRef).toObject(Account::class.java)
            val mainAcc = transaction.get(accountRef).toObject(Account::class.java)

            if (mainAcc == null || deletingAcc == null) {
                return@runTransaction
            }

            val mainBal = mainAcc.balance - deletingAcc.balance

            transaction.set(docRef, deletingAcc.copy(lastUpdated = null))
            transaction.set(
                accountRef, AccBalUpdate(mainBal), SetOptions.merge()
            )

            docRef.id
        }).await()
        Log.d("REPO", "deleteAccount: ")
    }

    suspend fun updateAccountBalance(addValue: Double, docId: String) {

    }

    companion object {
        @Volatile
        private var instance: AccountRepo? = null

        fun getInstance(): AccountRepo = instance ?: synchronized(this) {
            instance ?: AccountRepo().also { instance = it }
        }
    }

}