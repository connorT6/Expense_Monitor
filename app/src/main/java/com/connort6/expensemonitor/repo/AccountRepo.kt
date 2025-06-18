package com.connort6.expensemonitor.repo

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FirebaseFirestore
import jakarta.inject.Singleton
import kotlinx.coroutines.tasks.await

data class Account(
    @set:DocumentId var id: String? = null,
    val name: String,
    var balance: Double
)

@Singleton
class AccountRepo private constructor(
) {
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val collection = firestore.collection("asd")

    suspend fun createAccount(account: Account): String {
        val document = collection.add(account).await()
        collection
            .document(document.id)
            .update("id", document.id).await()
        return document.id
    }

    companion object {
        @Volatile
        private var instance: AccountRepo? = null

        fun getInstance(): AccountRepo =
            instance ?: synchronized(this) {
                instance ?: AccountRepo().also { instance = it }
            }
    }
}