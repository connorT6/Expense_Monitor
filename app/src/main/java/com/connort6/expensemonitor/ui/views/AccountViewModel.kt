package com.connort6.expensemonitor.ui.views

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.connort6.expensemonitor.repo.Account
import com.connort6.expensemonitor.repo.AccountRepo
import com.connort6.expensemonitor.repo.SMSOperator
import com.connort6.expensemonitor.repo.SMSOperatorRepo
import com.connort6.expensemonitor.repo.SMSParser
import com.connort6.expensemonitor.repo.TransactionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AccountData(
    val accounts: List<Account> = emptyList(),
    val mainAccount: Account = Account()
)

interface IAccountViewModel {
    val accountsState: StateFlow<AccountData>
    val addOrEditData: StateFlow<AddOrEditPopupData>
    val processingAccount: StateFlow<Account?>
    fun addAccount()
    fun deleteAccount(accId: String)
    fun showAddAcc(shouldShow: Boolean)
    fun setAddAccName(name: String)
    fun setAddAccBalance(balance: String)
    fun setAddAccIcon(icon: String)
    fun saveAcc()
    fun clearAccData()
    fun setSelectedAcc(account: Account)
    fun addSMSOperator(
        accountId: String,
        address: String,
        parseRule: String,
        transactionType: TransactionType
    )
}

class AccountViewModel : ViewModel(), IAccountViewModel {
    private val _accountsState = MutableStateFlow(AccountData())
    override val accountsState: StateFlow<AccountData> = _accountsState.asStateFlow()

    private val _addOrEditPopupData = MutableStateFlow(AddOrEditPopupData())
    override val addOrEditData = _addOrEditPopupData.asStateFlow()

    private val _processingAccount = MutableStateFlow<Account?>(null)
    override val processingAccount = _processingAccount.asStateFlow()

    private val accountRepo = AccountRepo.getInstance()
    private val smsOperatorRepo = SMSOperatorRepo.getInstance()

    init {
        viewModelScope.launch {
            accountRepo.accountFlow.collect { accounts ->
                _accountsState.update { state ->
                    state.copy(accounts = accounts.toList())
                }
            }
        }

        viewModelScope.launch {
            accountRepo.mainAccount.collect { mainAcc ->
                _accountsState.update { state ->
                    state.copy(mainAccount = mainAcc)
                }
            }
        }
    }

    override fun addAccount() {
        viewModelScope.launch {
            _addOrEditPopupData.value.let { it ->
                if (it.name != null && it.balance != null) {
                    val account = Account(
                        name = it.name!!,
                        balance = it.balance!!.toDouble(),
                        iconName = it.image ?: ""
                    )
                    accountRepo.createAccount(account)
                    clearAccData()
                }
            }
        }
    }

    override fun deleteAccount(accId: String) {
        viewModelScope.launch {
            accountRepo.deleteAccount(accId)
        }
    }

    override fun showAddAcc(shouldShow: Boolean) {
        _addOrEditPopupData.update { data ->
            data.copy(dialogShown = shouldShow)
        }
    }

    override fun setAddAccName(name: String) {
        _addOrEditPopupData.update { data ->
            data.copy(name = name)
        }
    }

    override fun setAddAccBalance(balance: String) {
        _addOrEditPopupData.update { data ->
            data.copy(balance = balance)
        }
    }

    override fun setAddAccIcon(icon: String) {
        _addOrEditPopupData.update { data ->
            data.copy(image = icon)
        }
    }

    override fun saveAcc() {
        clearAccData()
    }

    override fun clearAccData() {
        _addOrEditPopupData.update {
            AddOrEditPopupData()
        }
    }

    override fun setSelectedAcc(account: Account) {
        viewModelScope.launch {
            accountRepo.getById(account.id).let {
                _processingAccount.value = it
            }
        }
    }

    override fun addSMSOperator(
        accountId: String,
        address: String,
        parseRule: String,
        transactionType: TransactionType
    ) {
        viewModelScope.launch {

            val smsOperator =
                smsOperatorRepo.findByAddress(address) ?: SMSOperator(address = address)

            smsOperator.let { operator ->
                val copy = operator.copy(
                    parsers = operator.parsers + SMSParser(
                        accountId,
                        parseRule,
                        transactionType
                    )
                )
                smsOperatorRepo.saveOrUpdate(copy)
            }
        }
    }

}

// MockAccountViewModel.kt
// (Place in src/test/java or src/debug/java)
// Assuming AddOrEditPopupData is defined in your project
// If not, you'll need to define a mock version or import it.
// For simplicity, I'll assume it's available.
// data class AddOrEditPopupData(
//     val dialogShown: Boolean = false,
//     val name: String? = null,
//     val balance: String? = null,
//     val image: String? = null
// )
class MockAccountViewModel : IAccountViewModel {

    private val _accountsState = MutableStateFlow(AccountData())
    override val accountsState: StateFlow<AccountData> = _accountsState.asStateFlow()

    private val _addOrEditPopupData = MutableStateFlow(AddOrEditPopupData())
    override val addOrEditData: StateFlow<AddOrEditPopupData> = _addOrEditPopupData.asStateFlow()

    private val _processingAccount = MutableStateFlow<Account?>(null)
    override val processingAccount: StateFlow<Account?> = _processingAccount.asStateFlow()

    private val sampleAccounts = mutableListOf<Account>()

    init {
        // Sample Data
        val account1 = Account(
            id = "id_cash",
            name = "Cash",
            balance = 150.75,
            iconName = "account_balance_wallet",
//            smsSenders = emptyList()
        )
        val account2 = Account(
            id = "id_bank_savings",
            name = "Bank Savings",
            balance = 5270.20,
            iconName = "savings",
//            smsSenders = listOf(
//                SMSOperator(
//                    address = "MyBankAlerts",
//                    parsers = mutableListOf(
//                        SMSParser(
//                            "id_bank_savings",
//                            "Balance is \\$(\\d+\\.\\d+)",
//                            TransactionType.CREDIT
//                        )
//                    )
//                )
//            )
        )
        val account3 = Account(
            id = "id_credit_card",
            name = "Credit Card",
            balance = -340.50,
            iconName = "credit_card",
//            smsSenders = emptyList()
        )

        sampleAccounts.addAll(listOf(account1, account2, account3))

        _accountsState.value = AccountData(
            accounts = sampleAccounts.toList(),
            mainAccount = account1 // Let's say Cash is the main account initially
        )
    }

    override fun addAccount() {
        _addOrEditPopupData.value.let { data ->
            if (data.name != null && data.balance != null) {
                val newAccount = Account(
                    id = "new_id_${System.currentTimeMillis()}", // Simple unique ID for mock
                    name = data.name!!,
                    balance = data.balance!!.toDoubleOrNull() ?: 0.0,
                    iconName = data.image ?: "default_icon"
                )
                sampleAccounts.add(newAccount)
                _accountsState.update {
                    it.copy(accounts = sampleAccounts.toList())
                }
                clearAccData() // Clear popup data after adding
            }
        }
        println("MockAccountViewModel: addAccount called with data: ${_addOrEditPopupData.value}")
    }

    override fun deleteAccount(accId: String) {
        sampleAccounts.removeIf { it.id == accId }
        _accountsState.update {
            val newMain = if (it.mainAccount.id == accId) sampleAccounts.firstOrNull()
                ?: Account() else it.mainAccount
            it.copy(
                accounts = sampleAccounts.toList(),
                mainAccount = newMain
            )
        }
        println("MockAccountViewModel: deleteAccount called for ID: $accId")
    }

    override fun showAddAcc(shouldShow: Boolean) {
        _addOrEditPopupData.update { it.copy(dialogShown = shouldShow) }
        println("MockAccountViewModel: showAddAcc called with: $shouldShow")
    }

    override fun setAddAccName(name: String) {
        _addOrEditPopupData.update { it.copy(name = name) }
    }

    override fun setAddAccBalance(balance: String) {
        _addOrEditPopupData.update { it.copy(balance = balance) }
    }

    override fun setAddAccIcon(icon: String) {
        _addOrEditPopupData.update { it.copy(image = icon) }
    }

    override fun saveAcc() {
        // In your real VM, this calls clearAccData.
        // If it's meant to save the currently edited account (if editing was implemented),
        // the logic would be different. Based on current code, it just clears.
        clearAccData()
        println("MockAccountViewModel: saveAcc called (cleared popup data)")
    }

    override fun clearAccData() {
        _addOrEditPopupData.value = AddOrEditPopupData() // Reset to defaults
        println("MockAccountViewModel: clearAccData called")
    }

    override fun setSelectedAcc(account: Account) {
        _processingAccount.value = sampleAccounts.find { it.id == account.id }
        println("MockAccountViewModel: setSelectedAcc called with account: ${account.name}")
    }

    override fun addSMSOperator(
        accountId: String,
        address: String,
        parseRule: String,
        transactionType: TransactionType
    ) {
//        val accountIndex = sampleAccounts.indexOfFirst { it.id == accountId }
//        if (accountIndex != -1) {
//            val currentAccount = sampleAccounts[accountIndex]
//            val existingOperators = currentAccount.smsSenders.toMutableList()
//            var operator = existingOperators.find { it.address == address }
//
//            if (operator == null) {
//                operator =
//                    SMSOperator(
//                        address = address,
//                        parsers = mutableListOf(
//                            SMSParser(
//                                currentAccount.id,
//                                parseRule,
//                                transactionType
//                            )
//                        )
//                    )
//                existingOperators.add(operator)
//            } else {
//                val operatorIndex = existingOperators.indexOf(operator)
//                val newParsers = operator.parsers.toMutableList().apply {
//                    add(SMSParser(currentAccount.id, parseRule, transactionType))
//                }
//                operator = operator.copy(parsers = newParsers)
//                existingOperators[operatorIndex] = operator
//            }
//
//            val updatedAccount = currentAccount.copy(smsSenders = existingOperators)
//            sampleAccounts[accountIndex] = updatedAccount
//            _accountsState.update { it.copy(accounts = sampleAccounts.toList()) }
//            if (_processingAccount.value?.id == accountId) {
//                _processingAccount.value =
//                    updatedAccount // Update processing account if it's the one being modified
//            }
//        }
//        _processingAccount.value = null // As per original ViewModel
        println("MockAccountViewModel: addSMSOperator called for accId: $accountId, address: $address")
    }
}