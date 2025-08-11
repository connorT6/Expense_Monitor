package com.connort6.expensemonitor.repo

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.Transaction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.tasks.await


interface BaseEntity {
    var id: String
    var lastUpdated: Timestamp?
    val deleted: Boolean
}

open class MainRepository<T : BaseEntity>(
    private val clazz: Class<T>,
    private val getLastUpdated: (T) -> Timestamp?,
    private val sorting: ((List<T>) -> List<T>)? = null
) {
    protected val _allData = MutableStateFlow(listOf<T>())
    protected open lateinit var collection: CollectionReference

    open fun loadAll() {
        collection.whereEqualTo(BaseEntity::deleted.name, false)
            .orderBy(BaseEntity::lastUpdated.name, Query.Direction.DESCENDING)
            .get(Source.CACHE)
            .addOnSuccessListener { snapshot ->
                val sortByUpTime = snapshot.toObjects(clazz)
                if (sortByUpTime.isEmpty()) {
                    checkDataAvailable()
                    return@addOnSuccessListener
                }
                listenToChanges(
                    getLastUpdated.invoke(sortByUpTime.first()) ?: Timestamp.now()
                )
                _allData.update {
                    sorting?.invoke(sortByUpTime) ?: sortByUpTime
                }
            }
    }

    private fun checkDataAvailable() {
        collection.whereEqualTo(BaseEntity::deleted.name, false)
            .orderBy(BaseEntity::lastUpdated.name, Query.Direction.DESCENDING)
            .get(Source.SERVER)
            .let {
                it.addOnSuccessListener { querySnapshot ->
                    val accountsOrderedUpTime =
                        querySnapshot.toObjects(clazz) // accounts orders by last updated time
                    if (accountsOrderedUpTime.isNotEmpty()) {
                        listenToChanges(
                            getLastUpdated.invoke(accountsOrderedUpTime.first()) ?: Timestamp.now()
                        )
                        val sortedByDescending =
                            sorting?.invoke(accountsOrderedUpTime) ?: accountsOrderedUpTime
                        _allData.value = sortedByDescending
                    } else {
                        listenToChanges(Timestamp.now())
                    }
                }.addOnFailureListener { e ->
                    Log.e("MainRepository", "checkDataAvailable: ", e)
                }
            }
    }

    private fun listenToChanges(timestamp: Timestamp) {
        collection.whereGreaterThan(BaseEntity::lastUpdated.name, timestamp)
            .orderBy(BaseEntity::lastUpdated.name, Query.Direction.DESCENDING)
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
                    getLastUpdated.invoke(sortByUpTime.first()) ?: Timestamp.now()
                )
            }
    }

    suspend fun findAllByQuery(query: (CollectionReference) -> Query): List<T> {
        val snapshot = query.invoke(collection).get(Source.CACHE).await()
        return snapshot.toObjects(clazz)
    }

    suspend fun findByQuery(query: (CollectionReference) -> Query): T? {
        val snapshot = query.invoke(collection).limit(1).get(Source.CACHE).await()
        return snapshot.documents.firstOrNull()?.toObject(clazz)
    }

    fun findByIdTr(transaction: Transaction, id: String): T? {
        val reference = collection.document(id)
        return transaction.get(reference).toObject(clazz)
    }

    fun saveOrUpdateTr(transaction: Transaction, entity: T) {
        val document =
            if (entity.id.isNotEmpty())
                collection.document(entity.id)
            else
                collection.document()
        entity.lastUpdated = null
        transaction.set(document, entity, SetOptions.merge())
    }

    open suspend fun findById(id: String): T? {
        val snapshot = collection.document(id).get(Source.CACHE).await()
        return snapshot.toObject(clazz)
    }

    suspend fun saveOrUpdate(entity: T): T {
        entity.lastUpdated = null
        val document =
            if (entity.id.isNotEmpty())
                collection.document(entity.id)
            else
                collection.document()
        document.set(entity).await()
        entity.id = document.id
        return entity
    }

}