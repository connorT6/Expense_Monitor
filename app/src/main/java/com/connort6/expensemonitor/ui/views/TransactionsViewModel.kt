package com.connort6.expensemonitor.ui.views

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.connort6.expensemonitor.repo.AccountRepo
import com.connort6.expensemonitor.repo.CategoryRepo
import com.connort6.expensemonitor.repo.Transaction
import com.connort6.expensemonitor.repo.TransactionRepo
import com.connort6.expensemonitor.repo.TransactionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

data class TransactionDayDetails(val day: String)

interface ITransactionsViewModel {
    val transactions: StateFlow<List<Any>>
}

class TransactionsViewModel : ViewModel(), ITransactionsViewModel {

    private val _transactions = MutableStateFlow<List<Any>>(mutableListOf())
    override val transactions = _transactions.asStateFlow()

    private val transactionRepo = TransactionRepo.getInstance()
    private val dateFormat = SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault())
    private val accountRepo = AccountRepo.getInstance()
    private val categoryRepo = CategoryRepo.getInstance()

    init {
        viewModelScope.launch {
            transactionRepo.transactions.collect { transactions ->
                _transactions.value = transactions
                    .map { transaction ->
                        transaction.copy(
                            account = accountRepo.getById(transaction.accountId),
                            category = categoryRepo.getById(transaction.categoryId)
                        )
                    }
                    .groupBy {
                        dateFormat.format(it.createdTime.toDate())
                    }.flatMap { (day, transactions) ->
                        listOf(TransactionDayDetails(day)) + transactions
                    }
            }
        }

    }
}

class MockTransactionsViewModel : ITransactionsViewModel {
    private val _transactions = MutableStateFlow<List<Any>>(
        listOf(
            Transaction("1", "Groceries", 100.0, TransactionType.CREDIT),
            Transaction("1", "Groceries", 100.0, TransactionType.DEBIT),
            Transaction("1", "Groceries", 100.0, TransactionType.DEBIT),
            Transaction("1", "Groceries", 100.0, TransactionType.CREDIT)
        )
    )
    override val transactions: StateFlow<List<Any>> = _transactions.asStateFlow()

}