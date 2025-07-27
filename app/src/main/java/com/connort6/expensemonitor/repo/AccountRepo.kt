package com.connort6.expensemonitor.repo

import android.util.Log
import com.connort6.expensemonitor.mainCollection
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ServerTimestamp
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.Transaction
import jakarta.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class Account(
    var id: String = "",
    val name: String = "",
    var balance: Double = 0.0,
    var deleted: Boolean = false,
    @ServerTimestamp val lastUpdated: Timestamp? = null,
    val order: Int = 0,
    val iconName: String = "",
    val smsSenders: List<SMSOperator> = listOf(),
)

data class AccBalUpdate(
    var balance: Double = 0.0,
    @ServerTimestamp val lastUpdated: Timestamp? = null,
)

@Singleton
class AccountRepo private constructor(
) {

    private val accountRef = mainCollection.document("accounts")
    private val collection = accountRef.collection("Accounts")

    private val _accounts = MutableStateFlow<List<Account>>(listOf())
    private val _mainAcc = MutableStateFlow(Account())

    private val _allSmsSenders = MutableStateFlow<List<SMSOperator>>(listOf())
    val allSmsSendersFlow = _allSmsSenders.asStateFlow()

    val accountFlow = _accounts.asStateFlow()
    val mainAccount = _mainAcc.asStateFlow()

    init {
        accountRef.get().addOnSuccessListener {
            if (!it.exists()) {
                accountRef.set(Account(name = "All")).addOnSuccessListener { }
            }
        }
        getAllAccounts()
        CoroutineScope(Dispatchers.Default).launch {
            accountFlow.collect {
                _allSmsSenders.value =
                    it.flatMap { acc ->
                        acc.smsSenders.map {
                            it.copy(
                                accountId = acc.id
                            )
                        }
                    }.distinct().sortedBy { it.address }
            }
        }
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
            val mainAcc = transaction.get(accountRef).toObject(Account::class.java)

            val mainBal = (mainAcc?.balance ?: 0.0) + account.balance



            transaction.set(docRef, account.copy(id = docRef.id))
            transaction.set(
                accountRef, AccBalUpdate(mainBal), SetOptions.merge()
            )

            docRef.id
        }).await()
    }

    suspend fun update(account: Account): String {

        val updated = collection.document(account.id).set(account.copy(lastUpdated = null)).await()
        return account.id
    }

    private fun getAllAccounts() {
        Log.d("REPO", "getAllAccounts: ")
        val snapshot =
            collection.whereEqualTo(Account::deleted.name, false)
                .orderBy(Account::lastUpdated.name, Query.Direction.DESCENDING).get(Source.CACHE)


        snapshot.let { it ->
            it.addOnSuccessListener { querySnapshot ->
                val accountsOrderedUpTime =
                    querySnapshot.toObjects(Account::class.java) // accounts orders by last updated time
                if (accountsOrderedUpTime.isNotEmpty()) {
                    listenToChanges(accountsOrderedUpTime.first().lastUpdated ?: Timestamp.now())
                    val sortedByDescending = accountsOrderedUpTime.sortedByDescending { it.order }
                    _accounts.value = sortedByDescending
                } else {
                    checkDataAvailable()
                }
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

    private fun checkDataAvailable() {
        val snapshot =
            collection.whereEqualTo(Account::deleted.name, false)
                .orderBy(Account::lastUpdated.name, Query.Direction.DESCENDING).get(Source.SERVER)
        snapshot.let { it ->
            it.addOnSuccessListener { querySnapshot ->
                val accountsOrderedUpTime =
                    querySnapshot.toObjects(Account::class.java) // accounts orders by last updated time
                if (accountsOrderedUpTime.isNotEmpty()) {
                    listenToChanges(accountsOrderedUpTime.first().lastUpdated ?: Timestamp.now())
                    val sortedByDescending = accountsOrderedUpTime.sortedByDescending { it.order }
                    _accounts.value = sortedByDescending
                } else {
                    listenToChanges(Timestamp.now())
                }
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
                    val elements =
                        updated.toObjects(Account::class.java).filter { it.id.isNotEmpty() }.toSet()
                    if (elements.isEmpty()) {
                        return@addSnapshotListener
                    }
                    val updatedIds = elements.map { it.id }
                    _accounts.update { list ->
                        list.filterNot { updatedIds.contains(it.id) }
                            .plus(elements.filter { !it.deleted })
                            .sortedByDescending { it.order }
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

            transaction.set(docRef, deletingAcc.copy(deleted = true, lastUpdated = null))
            transaction.set(
                accountRef, AccBalUpdate(mainBal), SetOptions.merge()
            )

            docRef.id
        }).await()
        Log.d("REPO", "deleteAccount: ")
    }

    fun updateAccountBalance(
        addValue: Double,
        docId: String,
        transaction: Transaction? = null
    ): Account? {
        if (transaction != null) {
            return updateAccountBalance(docId, transaction, addValue)
        }
        val db = FirebaseFirestore.getInstance()
        db.runTransaction { tr ->
            return@runTransaction updateAccountBalance(docId, tr, addValue)
        }

        return null

    }

    private fun updateAccountBalance(
        docId: String,
        transaction: Transaction,
        addValue: Double
    ): Account? {
        try {
            val docRef = collection.document(docId)
            val account = transaction.get(docRef).toObject(Account::class.java)
            val mainAcc = transaction.get(accountRef).toObject(Account::class.java)
            if (account == null || mainAcc == null) {
                return null
            }
            val mainBal = mainAcc.balance + addValue
            transaction.set(
                docRef,
                account.copy(balance = account.balance + addValue, lastUpdated = null)
            )
            transaction.set(
                accountRef, AccBalUpdate(mainBal), SetOptions.merge()
            )
            return account
        } catch (e: Exception) {
            Log.e("Acc", "updateAccountBalance: ${e.message}", e)
        }
        return null
    }

    suspend fun getById(id: String): Account? {
        val snapshot = collection.document(id).get(Source.CACHE).await()
        return snapshot.toObject(Account::class.java)
    }

    companion object {
        @Volatile
        private var instance: AccountRepo? = null

        fun getInstance(): AccountRepo = instance ?: synchronized(this) {
            instance ?: AccountRepo().also { instance = it }
        }
    }

}