package com.connort6.expensemonitor.repo

import com.connort6.expensemonitor.model.entity.Account
import com.google.firebase.firestore.FirebaseFirestore
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.tasks.await

@Singleton
class AccountRepo @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    suspend fun createAccount(account: Account): String {
        val document = firestore.collection("asd").add(account).await()
        firestore.collection("asd")
            .document(document.id)
            .update("id", document.id)
            .await()
        return document.id
    }
}