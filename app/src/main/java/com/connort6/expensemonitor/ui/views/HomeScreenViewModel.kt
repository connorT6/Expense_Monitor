package com.connort6.expensemonitor.ui.views

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.connort6.expensemonitor.repo.Account
import com.connort6.expensemonitor.repo.AccountRepo
import com.connort6.expensemonitor.repo.Category
import com.connort6.expensemonitor.repo.CategoryRepo
import com.connort6.expensemonitor.repo.SMSOperator
import com.connort6.expensemonitor.repo.SMSOperatorRepo
import com.connort6.expensemonitor.repo.SMSParser
import com.connort6.expensemonitor.repo.SMSParserRepo
import com.connort6.expensemonitor.repo.SmsMessage
import com.connort6.expensemonitor.repo.SmsRepo
import com.connort6.expensemonitor.repo.Transaction
import com.connort6.expensemonitor.repo.TransactionRepo
import com.connort6.expensemonitor.repo.TransactionType
import com.connort6.expensemonitor.util.SMSParserUtil
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalTime
import java.util.Calendar
import java.util.Date


interface IHomeScreenViewModel {
    val accounts: StateFlow<List<Account>>
    val categories: StateFlow<List<Category>>
    val selectedAccount: StateFlow<Account?>
    val selectedCategory: StateFlow<Category?>
    val selectedDate: StateFlow<Calendar>
    val selectedTime: Flow<LocalTime>
    val selectedSmsMessage: StateFlow<SmsMessage?>
    val selectedTransactionType: StateFlow<TransactionType>
    val transactionAmount: StateFlow<BigDecimal>
    val smsOperators: StateFlow<List<SMSOperator>>
    val showCreateTransaction: StateFlow<Boolean>
    val shouldModifyAccBal: StateFlow<Boolean>
    val errorCode: StateFlow<ErrorCodes>

    fun createTransaction()
    fun saveTransaction(transaction: Transaction)
    fun selectAccount(account: Account)
    fun selectCategory(category: Category)
    fun selectDate(calendar: Calendar)
    fun selectTransactionType(transactionType: TransactionType)
    fun selectTime(time: LocalTime)
    fun setTransactionAmount(amount: BigDecimal)
    fun showCreateTransaction(show: Boolean)
    fun selectSmsMessage(selectedSms: SmsMessage?)
    fun setAccBalModify(modify: Boolean)

    enum class ErrorCodes {
        NONE,
        MSG_NOT_PARSED
    }
}

class HomeScreenViewModel : ViewModel(), IHomeScreenViewModel {

    private val _accounts = MutableStateFlow<List<Account>>(emptyList())
    override val accounts = _accounts.asStateFlow()

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    override val categories = _categories.asStateFlow()


    private val _selectedAccount = MutableStateFlow<Account?>(null)
    override val selectedAccount = _selectedAccount.asStateFlow()

    private val _selectedCategory = MutableStateFlow<Category?>(null)
    override val selectedCategory = _selectedCategory.asStateFlow()

    private val _selectedDate = MutableStateFlow(Calendar.getInstance())
    override val selectedDate = _selectedDate.asStateFlow()
    override val selectedTime = selectedDate.map { calendar ->
        LocalTime.of(
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            calendar.get(Calendar.SECOND)
        )
    }

    private val _transactionType = MutableStateFlow(TransactionType.DEBIT)
    override val selectedTransactionType = _transactionType.asStateFlow()

    private val _transactionAmount = MutableStateFlow(BigDecimal.ZERO)
    override val transactionAmount = _transactionAmount.asStateFlow()

    private val _smsOperators = MutableStateFlow<List<SMSOperator>>(emptyList())
    override val smsOperators = _smsOperators.asStateFlow()

    private val _showCreateTransaction = MutableStateFlow(false)
    override val showCreateTransaction = _showCreateTransaction.asStateFlow()

    private val _selectedSmsMessage = MutableStateFlow<SmsMessage?>(null)
    override val selectedSmsMessage = _selectedSmsMessage.asStateFlow()

    private val _shouldModifyAccBal = MutableStateFlow(true)
    override val shouldModifyAccBal = _shouldModifyAccBal.asStateFlow()

    private val _errorCode = MutableStateFlow(IHomeScreenViewModel.ErrorCodes.NONE)
    override val errorCode = _errorCode.asStateFlow()

    private val db = FirebaseFirestore.getInstance()
    private val accountRepo = AccountRepo.getInstance()
    private val categoryRepo = CategoryRepo.getInstance()
    private val transactionRepo = TransactionRepo.getInstance()
    private val smsOperatorRepo = SMSOperatorRepo.getInstance()
    private val smsParserRepo = SMSParserRepo.getInstance()
    private val smsRepo = SmsRepo.getInstance()

    init {
        viewModelScope.launch {
            accountRepo.accountFlow.collect {
                _accounts.value = it.toList()
            }
        }
        viewModelScope.launch {
            categoryRepo.categories.collect {
                _categories.value = it
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

            var savedSms: SmsMessage? = null

            val value = _selectedSmsMessage.value
            if (value != null) {
                savedSms = smsRepo.saveOrUpdate(value)
            }

            val transaction = Transaction(
                _selectedAccount.value!!.id,
                _selectedCategory.value!!.id,
                _transactionAmount.value.toDouble(),
                _transactionType.value,
                createdTime = Timestamp(_selectedDate.value.time),
                smsId = savedSms?.id
                //TODO update sms
            )

            var amount = _transactionAmount.value
            if (_transactionType.value == TransactionType.DEBIT) {
                amount = amount.negate()
            }
            try {
                db.runTransaction { tr ->
                    val accountById = accountRepo.findByIdTr(tr, transaction.accountId)

                    if (accountById == null) {
                        throw FirebaseFirestoreException(
                            "Account  not found",
                            FirebaseFirestoreException.Code.ABORTED // Or FAILED_PRECONDITION
                        )
                    }

                    if (_shouldModifyAccBal.value) {
                        accountRepo.saveOrUpdateTr(
                            tr,
                            accountById.copy(balance = accountById.balance + amount.toDouble())
                        )
                    }

                    transactionRepo.saveOrUpdateTr(tr, transaction)
                }.addOnSuccessListener {
                    _selectedAccount.value = null
                    _selectedCategory.value = null
                    _selectedDate.value = Calendar.getInstance()
                    _transactionAmount.value = BigDecimal.ZERO
                    _smsOperators.value = listOf()
                    _errorCode.value = IHomeScreenViewModel.ErrorCodes.NONE
                    _selectedSmsMessage.value = null
                    _shouldModifyAccBal.value = true
                }
            } catch (e: Exception) {
                Log.e(HomeScreenViewModel::class.java.name, "createTransaction failed: ", e)
            }
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
        _selectedDate.value = _selectedDate.value.let { calendar ->
            calendar.set(Calendar.HOUR_OF_DAY, time.hour)
            calendar.set(Calendar.MINUTE, time.minute)
            calendar.set(Calendar.SECOND, time.second)
            calendar
        }
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

    override fun selectSmsMessage(selectedSms: SmsMessage?) {
        _selectedSmsMessage.value = selectedSms
        if (selectedSms == null) {
            return
        }

        viewModelScope.launch {
            val selectedAccount = _selectedAccount.value
            val smsOperator = smsOperatorRepo.findByAddress(selectedSms.address)
            //TODO handle if not found
            var smsParsers: List<SMSParser> = listOf()
            if (smsOperator != null) {
                if (selectedAccount != null) {
                    smsParsers = smsParserRepo.findAllByOperatorIdAndAccountId(
                        smsOperator.id,
                        selectedAccount.id
                    )
                } else {
                    smsParsers = smsParserRepo.findBySmsOperatorId(smsOperator.id)
                }
            }
            if (smsParsers.isEmpty()) {
                _errorCode.value = IHomeScreenViewModel.ErrorCodes.MSG_NOT_PARSED
                return@launch
            }

            for (parser in smsParsers) {
                val parsedData = SMSParserUtil.parseMessage(selectedSms, parser)
                if (parsedData != null) {
                    if (selectedAccount == null) {
                        _selectedAccount.value = accountRepo.findById(parser.accountId)
                    }
                    val instance = Calendar.getInstance()
                    val date = Date(selectedSms.date)
                    instance.time = date
                    _selectedDate.value = instance
                    _transactionType.value = parser.transactionType
                    _transactionAmount.value = parsedData.amount
                    _errorCode.value = IHomeScreenViewModel.ErrorCodes.NONE
                    return@launch
                }
            }

            _errorCode.value = IHomeScreenViewModel.ErrorCodes.MSG_NOT_PARSED

        }
    }

    override fun setAccBalModify(modify: Boolean) {
        _shouldModifyAccBal.value = modify
    }
}


class MockHomeScreenViewModel : IHomeScreenViewModel {

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
    override val selectedSmsMessage: StateFlow<SmsMessage?>
        get() = MutableStateFlow(
            SmsMessage(
                "asdfa", "8822",
                "Test body Test body Test body Test body Test body Test body Test body Test body Test body Test body Test body Test body Test body",
                1234567890, 1
            )
        ).asStateFlow()

    override val shouldModifyAccBal: StateFlow<Boolean>
        get() = MutableStateFlow(false)
    override val errorCode: StateFlow<IHomeScreenViewModel.ErrorCodes>
        get() = MutableStateFlow(IHomeScreenViewModel.ErrorCodes.NONE).asStateFlow()

    override fun selectSmsMessage(selectedSms: SmsMessage?) {

    }

    override fun setAccBalModify(modify: Boolean) {

    }

    // Add any other functions that your UI might call and need mock behavior for.
}