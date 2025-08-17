package com.connort6.expensemonitor.ui.views

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.connort6.expensemonitor.repo.AccountRepo
import com.connort6.expensemonitor.repo.CategoryRepo
import com.connort6.expensemonitor.repo.Transaction
import com.connort6.expensemonitor.repo.TransactionRepo
import com.connort6.expensemonitor.repo.TransactionType
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class TransactionDayDetails(val day: String)

interface ITransactionsViewModel {
    val transactions: StateFlow<List<Any>>
    val selected : StateFlow<Transaction?>

    fun selectTransaction(transaction: Transaction)
    fun clearSelection()
}

class TransactionsViewModel : ViewModel(), ITransactionsViewModel {

    private val _transactions = MutableStateFlow<List<Any>>(mutableListOf())
    override val transactions = _transactions.asStateFlow()

    private val _selected = MutableStateFlow<Transaction?>(null)
    override val selected = _selected.asStateFlow()

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
                            account = accountRepo.findById(transaction.accountId),
                            category = categoryRepo.findById(transaction.categoryId)
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

    override fun selectTransaction(transaction: Transaction) {
        _selected.value = transaction
    }

    override fun clearSelection() {
        _selected.value = null
    }
}

class MockTransactionsViewModel : ITransactionsViewModel {

    val list = listOf(
        Transaction(
            accountId = "account_1",
            categoryId = "category_food",
            amount = 50.0,
            transactionType = TransactionType.DEBIT,
            createdTime = Timestamp.now()
        ),
        Transaction(
            accountId = "account_2",
            categoryId = "category_transport",
            amount = 25.50,
            transactionType = TransactionType.DEBIT,
            createdTime = Timestamp.now()
        ),
        Transaction(
            accountId = "account_1",
            categoryId = "category_salary",
            amount = 2000.0,
            transactionType = TransactionType.CREDIT,
            createdTime = Timestamp.now()
        ),
        Transaction(
            accountId = "account_3",
            categoryId = "category_entertainment",
            amount = 75.20,
            transactionType = TransactionType.DEBIT,
            createdTime = Timestamp(
                Date(
                    System.currentTimeMillis() - 24 * 2 * 60 * 60 * 1000
                )
            )
        )
    )
    private val _transactions = MutableStateFlow<List<Any>>(
        list
    )
    override val transactions: StateFlow<List<Any>> = _transactions.asStateFlow()


    val value = Transaction(
        "1",
        "Groceries",
        100.0,
        TransactionType.CREDIT,
    )
    private val _selected = MutableStateFlow<Transaction?>(
        value
    )
    override val selected: StateFlow<Transaction?> = _selected.asStateFlow()

    override fun selectTransaction(transaction: Transaction) {
        _selected.value = transaction
    }

    override fun clearSelection() {
        _selected.value = null
    }

}