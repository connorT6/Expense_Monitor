package com.connort6.expensemonitor.ui.views

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.connort6.expensemonitor.repo.Account
import com.connort6.expensemonitor.repo.AccountRepo
import com.connort6.expensemonitor.repo.Category
import com.connort6.expensemonitor.repo.CategoryRepo
import com.connort6.expensemonitor.repo.Transaction
import com.connort6.expensemonitor.repo.TransactionRepo
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

open class HomeScreenViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    private val accountRepo = AccountRepo.getInstance()
    private val _accountTotal = MutableStateFlow(Account())
    private val _accounts = MutableStateFlow<List<Account>>(emptyList())
    open val accounts = _accounts.asStateFlow()

    private val categoryRepo = CategoryRepo.getInstance()
    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    open val categories = _categories.asStateFlow()

    open val accountTotal = _accountTotal.asStateFlow()
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

    open fun saveTransaction(transaction: Transaction) {
        if (transaction.account == null) {
            return
        }
        val transactionCopy = transaction.copy(
            accountId = transaction.account!!.id,
        )
        viewModelScope.launch {
            db.runTransaction ({ tr ->
                accountRepo.updateAccount(transactionCopy.account!!)
            })
            transactionRepo.saveTransaction(transaction)
        }
    }
}

class MockHomeScreenViewModel : HomeScreenViewModel() {

    // Override properties from HomeScreenViewModel
    private val _mockAccountTotal = MutableStateFlow(
        Account(id = "total", name = "Total Balance", balance = 1500.75) // Use your Account data class
    )
    override val accountTotal: StateFlow<Account> = _mockAccountTotal

    private val _mockAccounts = MutableStateFlow<List<Account>>(
        listOf(
            Account(id = "1", name = "Savings", balance = 1000.0),
            Account(id = "2", name = "Checking", balance = 500.75)
        )
    )
    override val accounts: StateFlow<List<Account>> = _mockAccounts

    private val _mockCategories = MutableStateFlow<List<Category>>(
        listOf(
            Category(id = "cat1", name = "Groceries"),
            Category(id = "cat2", name = "Salary"),
            Category(id = "cat3", name = "Entertainment")
        )
    )
    override val categories: StateFlow<List<Category>> = _mockCategories

    // The init block of the parent HomeScreenViewModel will run unless you
    // prevent it or ensure its dependencies are mocked if it tries to access them.
    // Since we're overriding the flows directly with mock data,
    // the parent's init block collecting from real repos won't affect these overridden flows.

    // Override functions as needed
    override fun saveTransaction(transaction: Transaction) {
        // Provide a mock implementation for previews
        // For example, you could log it or update one of the mock flows if needed
        println("MockPreview: saveTransaction called with $transaction")
        // Optionally, you could add the transaction to a mock list of transactions
        // or update the _mockAccountTotal if the transaction affects it.
        // For instance, if it's an expense:
        // _mockAccountTotal.value = _mockAccountTotal.value.copy(
        //     balance = _mockAccountTotal.value.balance - transaction.amount
        // )
    }

    // Add any other functions that your UI might call and need mock behavior for.
}