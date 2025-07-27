package com.connort6.expensemonitor.repo

import com.google.firebase.Timestamp
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update


interface BaseEntity {
    val id: String
    val lastUpdated: Timestamp?
    val deleted: Boolean
}

open class MainRepository<T : BaseEntity>(
    private val clazz: Class<T>,
    private val getLastUpdated: (T) -> Timestamp?,
    private val sorting: ((List<T>) -> List<T>)? = null
) {
    protected val _allData = MutableStateFlow(listOf<T>())

    fun loadAll(collection: CollectionReference) {
        collection.whereEqualTo("deleted", false)
            .orderBy("lastUpdated", Query.Direction.DESCENDING)
            .get(Source.CACHE)
            .addOnSuccessListener { snapshot ->
                val sortByUpTime = snapshot.toObjects(clazz)
                if (sortByUpTime.isEmpty()) {
                    checkDataAvailable(collection)
                    return@addOnSuccessListener
                }
                listenToChanges(
                    collection,
                    getLastUpdated.invoke(sortByUpTime.first()) ?: Timestamp.now()
                )
                _allData.update {
                    sorting?.invoke(sortByUpTime) ?: sortByUpTime
                }
            }
    }

    private fun checkDataAvailable(collection: CollectionReference) {
        collection.whereEqualTo("deleted", false)
            .orderBy("lastUpdated", Query.Direction.DESCENDING)
            .get(Source.SERVER)
            .let { it ->
                it.addOnSuccessListener { querySnapshot ->
                    val accountsOrderedUpTime =
                        querySnapshot.toObjects(clazz) // accounts orders by last updated time
                    if (accountsOrderedUpTime.isNotEmpty()) {
                        listenToChanges(
                            collection,
                            getLastUpdated.invoke(accountsOrderedUpTime.first()) ?: Timestamp.now()
                        )
                        val sortedByDescending =
                            sorting?.invoke(accountsOrderedUpTime) ?: accountsOrderedUpTime
                        _allData.value = sortedByDescending
                    } else {
                        listenToChanges(collection, Timestamp.now())
                    }
                }
            }
    }

    private fun listenToChanges(collection: CollectionReference, timestamp: Timestamp) {
        collection.whereGreaterThan("lastUpdated", timestamp)
            .orderBy("lastUpdated", Query.Direction.DESCENDING)
            .addSnapshotListener { value, error ->
                if (error != null) {
                    return@addSnapshotListener
                }
                if (value == null) {
                    return@addSnapshotListener
                }
                val sortByUpTime = value.toObjects(clazz)
                if (sortByUpTime.isEmpty()) {
                    return@addSnapshotListener
                }
                val updatedIds = sortByUpTime.map { it.id }

                _allData.update { operatorList ->
                    val all = operatorList.filter { it.id !in updatedIds }
                        .plus(sortByUpTime)
                    sorting?.invoke(all) ?: all
                }

                listenToChanges(
                    collection,
                    getLastUpdated.invoke(sortByUpTime.first()) ?: Timestamp.now()
                )
            }
    }

}