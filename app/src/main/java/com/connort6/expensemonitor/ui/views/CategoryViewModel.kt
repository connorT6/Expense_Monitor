package com.connort6.expensemonitor.ui.views

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.connort6.expensemonitor.repo.Category
import com.connort6.expensemonitor.repo.CategoryRepo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CategoryViewModel : ViewModel() {
    private val _categories = MutableStateFlow(emptySet<Category>())
    val categoriesState = _categories.asStateFlow()

    private val _addOrEditPopupData = MutableStateFlow(AddOrEditPopupData())
    val addOrEditData = _addOrEditPopupData.asStateFlow()

    private val categoryRepo = CategoryRepo.getInstance()

    init {
        viewModelScope.launch {
            categoryRepo.categories.collect { categories ->
                _categories.update { categories.toSet()}
            }
        }
    }


    fun showAddCat(shouldShow: Boolean) {
        _addOrEditPopupData.update { data ->
            data.copy(dialogShown = shouldShow)
        }
    }

    fun setAddCatName(name: String) {
        _addOrEditPopupData.update { data ->
            data.copy(name = name)
        }
    }

    fun setAddCatIcon(icon: String) {
        _addOrEditPopupData.update { data ->
            data.copy(image = icon)
        }
    }

    fun addCategory() {
        viewModelScope.launch {
            _addOrEditPopupData.value.let { it ->
                if (it.name != null) {
                    val category = Category(name = it.name!!, iconName = it.image ?: "")
                    categoryRepo.createCategory(category)
                    clearAccData()
                }
            }
        }
    }

    fun deleteCategory(id: String) {
        viewModelScope.launch {
            categoryRepo.deleteById(id)
        }
    }

    fun clearAccData() {
        _addOrEditPopupData.update {
            AddOrEditPopupData()
        }
    }
}