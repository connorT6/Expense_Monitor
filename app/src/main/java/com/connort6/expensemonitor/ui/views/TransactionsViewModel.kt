package com.connort6.expensemonitor.ui.views

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.connort6.expensemonitor.repo.Transaction
import com.connort6.expensemonitor.repo.TransactionRepo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TransactionsViewModel : ViewModel() {

    private val _transactions = MutableStateFlow<List<Transaction>>(mutableListOf())
    val transactions = _transactions.asStateFlow()


    private val transactionRepo = TransactionRepo.getInstance()


    init {
        viewModelScope.launch {
            transactionRepo.transactions.collect {
                _transactions.value = it
            }
        }

    }
}