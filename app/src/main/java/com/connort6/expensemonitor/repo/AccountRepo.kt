package com.connort6.expensemonitor.repo

import android.util.Log
import com.connort6.expensemonitor.mainCollection
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ServerTimestamp
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.Transaction
import jakarta.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
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

data class AccBalUpdate(
    var balance: Double = 0.0,
    @ServerTimestamp val lastUpdated: Timestamp? = null,
)

@Singleton
class AccountRepo private constructor() : MainRepository<Account>(
    Account::class.java,
    { it.lastUpdated },
    { list -> list.sortedByDescending { it.order } }
) {

    private val accountRef = mainCollection.document("accounts")
    override var collection = accountRef.collection("Accounts")

    private val _mainAcc = MutableStateFlow(Account())

    val accountFlow = _allData.asStateFlow()
    val mainAccount = _mainAcc.asStateFlow()

    init {
        accountRef.get().addOnSuccessListener {
            if (!it.exists()) {
                accountRef.set(Account(name = "All")).addOnSuccessListener { }
            } else {
                it.toObject(Account::class.java)?.let { acc ->
                    _mainAcc.value = acc
                }
            }
        }
        getAllAccounts()
//        CoroutineScope(Dispatchers.Default).launch {
//            accountFlow.collect {
//                _allSmsSenders.value =
//                    it.flatMap { acc ->
//                        acc.smsSenders
//                    }.distinct().sortedBy { it.address }
//            }
//        }
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


    private fun getAllAccounts() {
        Log.d("REPO", "getAllAccounts: ")
        loadAll()

        accountRef.get(Source.CACHE).addOnSuccessListener { value ->
            if (value == null || !value.exists()) {
                return@addOnSuccessListener
            }
            val account = value.toObject(Account::class.java) ?: return@addOnSuccessListener
            _mainAcc.value = account
            listenToMainAcc()
        }

    }

    private fun listenToMainAcc() {
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
            val mainAcc = getMainAcc(transaction)
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

    fun getMainAcc(transaction: Transaction): Account? =
        transaction.get(accountRef).toObject(Account::class.java)

    fun updateMainAcc(transaction: Transaction, account: Account) {
        val copy = account.copy(lastUpdated = null)
        transaction.set(accountRef, copy, SetOptions.merge())
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