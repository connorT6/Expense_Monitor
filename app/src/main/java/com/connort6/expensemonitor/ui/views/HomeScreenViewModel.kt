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

interface IHomeScreenViewModel {
    val accounts: StateFlow<List<Account>>
    val categories: StateFlow<List<Category>>
    val accountTotal: StateFlow<Account>
    val selectedAccount: StateFlow<Account?>
    val selectedCategory: StateFlow<Category?>
    fun saveTransaction(transaction: Transaction)
    fun selectAccount(account: Account)
    fun selectCategory(category: Category)
}

class HomeScreenViewModel : ViewModel(), IHomeScreenViewModel {

    // Interface implementations
    override val accounts: StateFlow<List<Account>>
        get() = _accounts.asStateFlow()
    override val categories: StateFlow<List<Category>>
        get() = _categories.asStateFlow()
    override val accountTotal: StateFlow<Account>
        get() = _accountTotal.asStateFlow()

    override val selectedAccount: StateFlow<Account?>
        get() = _selectedAccount.asStateFlow()

    override val selectedCategory: StateFlow<Category?>
        get() = _selectedCategory.asStateFlow()

    private val db = FirebaseFirestore.getInstance()

    private val accountRepo = AccountRepo.getInstance()
    private val _accountTotal = MutableStateFlow(Account())
    private val _accounts = MutableStateFlow<List<Account>>(emptyList())
    private val categoryRepo = CategoryRepo.getInstance()
    private val _categories = MutableStateFlow<List<Category>>(emptyList())

    private val transactionRepo = TransactionRepo.getInstance()
    private val _selectedAccount = MutableStateFlow<Account?>(null)
    private val _selectedCategory = MutableStateFlow<Category?>(null)

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

    override fun saveTransaction(transaction: Transaction) {
        if (transaction.account == null) {
            return
        }
        val transactionCopy = transaction.copy(
            accountId = transaction.account!!.id,
        )
//        viewModelScope.launch {
//            db.runTransaction ({ tr ->
//                accountRepo.updateAccount(transactionCopy.account!!)
//            })
//            transactionRepo.saveTransaction(transaction)
//        }
    }

    override fun selectAccount(account: Account) {
        _selectedAccount.value = account
    }

    override fun selectCategory(category: Category) {
        _selectedCategory.value = category
    }
}



class MockHomeScreenViewModel : IHomeScreenViewModel {

    // Override properties from HomeScreenViewModel
    override val accountTotal: StateFlow<Account> =
        MutableStateFlow(
            Account(id = "total", name = "Total Balance", balance = 1500.75) // Use your Account data class
        )
            .asStateFlow()

    override val accounts: StateFlow<List<Account>> = MutableStateFlow<List<Account>>(
        listOf(
            Account(id = "1", name = "Savings", balance = 1000.0),
            Account(id = "2", name = "Checking", balance = 500.75),
            Account(id = "3", name = "Investment", balance = 12050.20)
        )
    )
    .asStateFlow()
    override val categories: StateFlow<List<Category>> = MutableStateFlow<List<Category>>(
        listOf(
            Category(id = "cat1", name = "Groceries"),
            Category(id = "cat2", name = "Salary"),
            Category(id = "cat3", name = "Entertainment"),
            Category(id = "cat4", name = "Utilities"),
            Category(id = "cat5", name = "Rent/Mortgage"),
            Category(id = "cat6", name = "Transportation")
        )
    )
    .asStateFlow()
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

    override val selectedAccount: StateFlow<Account?> = MutableStateFlow<Account?>(null).asStateFlow()
    override fun selectAccount(account: Account) {
        println("MockPreview: selectAccount called with $account")
    }

    override val selectedCategory: StateFlow<Category?> = MutableStateFlow<Category?>(null).asStateFlow()
    override fun selectCategory(category: Category) {
        println("MockPreview: selectCategory called with $category")
    }

    // Add any other functions that your UI might call and need mock behavior for.
}