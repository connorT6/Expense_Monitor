package com.connort6.expensemonitor.repo

import android.util.Log
import com.connort6.expensemonitor.mainCollection
import com.google.firebase.Timestamp
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ServerTimestamp
import com.google.firebase.firestore.Source
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.tasks.await


data class Category(
    var id: String = "",
    val name: String = "",
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

class CategoryRepo private constructor() {

    private val categoryDocRef = mainCollection.document("category")
    private val collection = categoryDocRef.collection("Categories")

    private val _categories = MutableStateFlow<Set<Category>>(mutableSetOf())
//    private val _mainAcc = MutableStateFlow(Account())

    val accountFlow = _categories.asStateFlow()
//    val mainAccount = _mainAcc.asStateFlow()

    companion object {
        @Volatile
        private var instance: CategoryRepo? = null

        fun getInstance(): CategoryRepo = instance ?: synchronized(this) {
            instance ?: CategoryRepo().also { instance = it }
        }
    }

    suspend fun createCategory(category: Category): String {
        val existing = collection.whereEqualTo(Category::name.name, category.name).get(Source.CACHE).await()
        if (!existing.isEmpty) {
            return existing.toObjects(Account::class.java).first().id
        }

        val docRef = collection.document()

        docRef.set(category.copy(id = docRef.id)).await()

        return docRef.id
    }

    suspend fun updateCategory(category: Category) { // TOTO set last update time
        collection.document(category.id).set(category).await()
    }

    private fun getAllCategories() {
        val snapshot =
            collection.whereEqualTo(Category::deleted.name, false).orderBy(Category::lastUpdated.name, Query.Direction.DESCENDING).get(Source.CACHE)


        snapshot.let { it ->
            it.addOnSuccessListener { querySnapshot ->
                val accountsOrderedUpTime = querySnapshot.toObjects(Category::class.java) // accounts orders by last updated time
                if (accountsOrderedUpTime.isNotEmpty()) {
                    listenToChanges(accountsOrderedUpTime.first().lastUpdated ?: Timestamp.now())
                } else {
                    listenToChanges(Timestamp.now())
                }
                val sortedByDescending = accountsOrderedUpTime.sortedByDescending { it.order }
                _categories.value = sortedByDescending.toSet()
            }
            it.addOnFailureListener {
                Log.d("REPO", "getAllAccounts: ${it.message}")
            }
        }
    }

    private fun listenToChanges(lastUpdated: Timestamp) {
        collection.whereGreaterThan(Category::lastUpdated.name, lastUpdated).orderBy(Category::lastUpdated.name, Query.Direction.DESCENDING)
            .addSnapshotListener { value, error ->
                if (error != null) {
                    Log.d("REPO", "listenToChanges: ${error.message}")
                    return@addSnapshotListener
                }
                value?.let { updated ->
                    val elements = updated.toObjects(Category::class.java).filter { it.id.isNotEmpty() }.toSet()
                    if (elements.isEmpty()) {
                        return@addSnapshotListener
                    }
                    _categories.update { categorySet ->
                        categorySet.toMutableSet().apply {
                            removeAll(elements)
                            addAll(elements.filter { !it.deleted })
                            sortedByDescending { it.order }
                        }
                    }
                    listenToChanges(elements.first().lastUpdated ?: Timestamp.now())
                }
            }
    }
}