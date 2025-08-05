package com.connort6.expensemonitor.ui.views

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.connort6.expensemonitor.repo.Account
import com.connort6.expensemonitor.repo.AccountRepo
import com.connort6.expensemonitor.repo.Category
import com.connort6.expensemonitor.repo.CategoryRepo
import com.connort6.expensemonitor.repo.SMSOperator
import com.connort6.expensemonitor.repo.Transaction
import com.connort6.expensemonitor.repo.TransactionRepo
import com.connort6.expensemonitor.repo.TransactionType
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalTime
import java.util.Calendar

interface IHomeScreenViewModel {
    val accounts: StateFlow<List<Account>>
    val categories: StateFlow<List<Category>>
    val accountTotal: StateFlow<Account>
    val selectedAccount: StateFlow<Account?>
    val selectedCategory: StateFlow<Category?>
    val selectedDate: StateFlow<Calendar>
    val selectedTime: StateFlow<LocalTime>
    val selectedTransactionType: StateFlow<TransactionType>
    val transactionAmount: StateFlow<BigDecimal>
    val smsOperators: StateFlow<List<SMSOperator>>
    val showCreateTransaction: StateFlow<Boolean>

    fun createTransaction()
    fun saveTransaction(transaction: Transaction)
    fun selectAccount(account: Account)
    fun selectCategory(category: Category)
    fun selectDate(calendar: Calendar)
    fun selectTransactionType(transactionType: TransactionType)
    fun selectTime(time: LocalTime)
    fun setTransactionAmount(amount: BigDecimal)
    fun showCreateTransaction(show: Boolean)
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

    override val selectedDate: StateFlow<Calendar>
        get() = _selectedDate.asStateFlow()

    override val selectedTime: StateFlow<LocalTime>
        get() = _selectedTime.asStateFlow()

    override val selectedTransactionType: StateFlow<TransactionType>
        get() = _transactionType.asStateFlow()

    override val transactionAmount: StateFlow<BigDecimal>
        get() = _transactionAmount.asStateFlow()

    override val smsOperators: StateFlow<List<SMSOperator>>
        get() = _smsOperators.asStateFlow()

    override val showCreateTransaction: StateFlow<Boolean>
        get() = _showCreateTransaction.asStateFlow()
    private val db = FirebaseFirestore.getInstance()

    private val accountRepo = AccountRepo.getInstance()
    private val _accountTotal = MutableStateFlow(Account())
    private val _accounts = MutableStateFlow<List<Account>>(emptyList())
    private val categoryRepo = CategoryRepo.getInstance()
    private val _categories = MutableStateFlow<List<Category>>(emptyList())

    private val transactionRepo = TransactionRepo.getInstance()
    private val _selectedAccount = MutableStateFlow<Account?>(null)
    private val _selectedCategory = MutableStateFlow<Category?>(null)
    private val _selectedDate = MutableStateFlow(Calendar.getInstance())
    private val _selectedTime = MutableStateFlow(LocalTime.now())
    private val _transactionType = MutableStateFlow(TransactionType.DEBIT)
    private val _transactionAmount = MutableStateFlow(BigDecimal.ZERO)
    private val _smsOperators = MutableStateFlow<List<SMSOperator>>(emptyList())

    private val _showCreateTransaction = MutableStateFlow(false)

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

    override fun createTransaction() {
        viewModelScope.launch {
            if (_selectedAccount.value == null || _selectedCategory.value == null) {
                return@launch
            }
            val transaction = Transaction(
                _selectedAccount.value!!.id,
                _selectedCategory.value!!.id,
                _transactionAmount.value.toDouble(),
                _transactionType.value
            )
            var amount = _transactionAmount.value
            if (_transactionType.value == TransactionType.DEBIT) {
                amount = amount.negate()
            }
            db.runTransaction { tr ->
                val updateAccountBalance = accountRepo.updateAccountBalance(
                    amount.toDouble(),
                    transaction.accountId,
                    tr
                )

                transactionRepo.saveTransactionTransactional(transaction, tr)
            }
            _selectedAccount.value = null
            _selectedCategory.value = null
            _selectedDate.value = Calendar.getInstance()
            _selectedTime.value = LocalTime.now()
            _transactionAmount.value = BigDecimal.ZERO
            _smsOperators.value = listOf()
        }
    }

    override fun selectAccount(account: Account) {
        _selectedAccount.value = account
    }

    override fun selectCategory(category: Category) {
        _selectedCategory.value = category
    }

    override fun selectDate(calendar: Calendar) {
        _selectedDate.value = calendar
    }

    override fun selectTime(time: LocalTime) {
        _selectedTime.value = time
    }

    override fun selectTransactionType(transactionType: TransactionType) {
        _transactionType.value = transactionType
    }

    override fun setTransactionAmount(amount: BigDecimal) {
        _transactionAmount.value = amount
    }

    override fun showCreateTransaction(show: Boolean) {
        _showCreateTransaction.value = show
    }
}


class MockHomeScreenViewModel : IHomeScreenViewModel {

    // Override properties from HomeScreenViewModel
    override val accountTotal: StateFlow<Account> =
        MutableStateFlow(
            Account(
                id = "total",
                name = "Total Balance",
                balance = 1500.75
            ) // Use your Account data class
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

    override val selectedAccount: StateFlow<Account?> =
        MutableStateFlow<Account?>(null).asStateFlow()

    override fun selectAccount(account: Account) {
        println("MockPreview: selectAccount called with $account")
    }

    override val selectedCategory: StateFlow<Category?> =
        MutableStateFlow<Category?>(null).asStateFlow()

    override fun selectCategory(category: Category) {
        println("MockPreview: selectCategory called with $category")
    }

    override val selectedDate: StateFlow<Calendar> =
        MutableStateFlow(Calendar.getInstance()).asStateFlow()

    override fun selectDate(calendar: Calendar) {
        println("MockPreview: selectDate called with $calendar")
    }

    override val selectedTime: StateFlow<LocalTime> =
        MutableStateFlow(LocalTime.now()).asStateFlow()

    override fun selectTime(time: LocalTime) {
        println("MockPreview: selectTime called with $time")
    }

    override val selectedTransactionType: StateFlow<TransactionType> =
        MutableStateFlow(TransactionType.DEBIT).asStateFlow()

    override fun selectTransactionType(transactionType: TransactionType) {
        println("MockPreview: selectTransactionType called with $transactionType")
    }

    override fun createTransaction() {
        println("MockPreview: createTransaction called")
    }

    override val transactionAmount: StateFlow<BigDecimal> =
        MutableStateFlow(BigDecimal.ZERO).asStateFlow()

    override fun setTransactionAmount(amount: BigDecimal) {
        println("MockPreview: setTransactionAmount called with $amount")
    }

    override val smsOperators: StateFlow<List<SMSOperator>> =
        MutableStateFlow(
            listOf(
                SMSOperator(address = "8822"),
                SMSOperator(address = "AAB")
            )
        ).asStateFlow()

    override fun showCreateTransaction(show: Boolean) {
    }

    override val showCreateTransaction: StateFlow<Boolean>
        get() = MutableStateFlow(false)

    // Add any other functions that your UI might call and need mock behavior for.
}