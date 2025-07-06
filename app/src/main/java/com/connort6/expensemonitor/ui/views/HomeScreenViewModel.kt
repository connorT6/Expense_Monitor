package com.connort6.expensemonitor.ui.views

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.connort6.expensemonitor.repo.Account
import com.connort6.expensemonitor.repo.AccountRepo
import com.connort6.expensemonitor.repo.Category
import com.connort6.expensemonitor.repo.CategoryRepo
import com.connort6.expensemonitor.repo.TransactionRepo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeScreenViewModel : ViewModel() {

    private val accountRepo = AccountRepo.getInstance()
    private val _accountTotal = MutableStateFlow(Account())
    private val _accounts = MutableStateFlow<List<Account>>(emptyList())
    val accounts = _accounts.asStateFlow()

    private val categoryRepo = CategoryRepo.getInstance()
    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories = _categories.asStateFlow()

    val accountTotal = _accountTotal.asStateFlow()
    private val transactionRepo = TransactionRepo.getInstance()

    init {
        viewModelScope.launch {
            accountRepo.mainAccount.collect {
                _accountTotal.value = it
            }
        }
        viewModelScope.launch {
            accountRepo.accountFlow.collect {
                _accounts.value = it.toList()
            }
        }
        viewModelScope.launch {
            categoryRepo.categoryFlow.collect {
                _categories.value = it.toList()
            }
        }
    }
}