package com.connort6.expensemonitor.repo

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp
import kotlinx.coroutines.flow.asStateFlow


data class Category(
    override var id: String = "",
    val name: String = "",
    override var deleted: Boolean = false,
    @ServerTimestamp override var lastUpdated: Timestamp? = null,
    val order: Int = 0,
    val iconName: String = ""
) : BaseEntity

class CategoryRepo private constructor() : MainRepository<Category>(
    Category::class.java,
    { it.lastUpdated ?: Timestamp.now() },
    "category",
    "Categories"
) {

    val categories = _allData.asStateFlow()

    companion object {
        @Volatile
        private var instance: CategoryRepo? = null

        fun getInstance(): CategoryRepo = instance ?: synchronized(this) {
            instance ?: CategoryRepo().also { instance = it }
        }
    }

    suspend fun createCategory(category: Category): String {
        val existing = findByName(category.name)
        if (existing != null) {
            return existing.id
        }
        return saveOrUpdate(category).id
    }

    suspend fun findByName(name: String): Category? {
        return findByQuery { query ->
            query.whereEqualTo(Category::name.name, name)
        }

    }

}