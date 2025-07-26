package com.connort6.expensemonitor.ui.views

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.connort6.expensemonitor.repo.Account
import com.connort6.expensemonitor.repo.AccountRepo
import com.connort6.expensemonitor.repo.SMSOperator
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


class AccountViewModel : ViewModel() {
    private val _accountsState = MutableStateFlow(AccountData())
    val accountsState: StateFlow<AccountData> = _accountsState.asStateFlow()

    private val _addOrEditPopupData = MutableStateFlow(AddOrEditPopupData())
    val addOrEditData = _addOrEditPopupData.asStateFlow()

    private val _processingAccount = MutableStateFlow<Account?>(null)
    val processingAccount = _processingAccount.asStateFlow()

    private val accountRepo = AccountRepo.getInstance()

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

    fun addAccount() {
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

    fun deleteAccount(accId: String) {
        viewModelScope.launch {
            accountRepo.deleteAccount(accId)
        }
    }

    fun showAddAcc(shouldShow: Boolean) {
        _addOrEditPopupData.update { data ->
            data.copy(dialogShown = shouldShow)
        }
    }

    fun setAddAccName(name: String) {
        _addOrEditPopupData.update { data ->
            data.copy(name = name)
        }
    }

    fun setAddAccBalance(balance: String) {
        _addOrEditPopupData.update { data ->
            data.copy(balance = balance)
        }
    }

    fun setAddAccIcon(icon: String) {
        _addOrEditPopupData.update { data ->
            data.copy(image = icon)
        }
    }

    fun saveAcc() {
        clearAccData()
    }

    fun clearAccData() {
        _addOrEditPopupData.update {
            AddOrEditPopupData()
        }
    }

    fun setSelectedAcc(account: Account) {
        viewModelScope.launch {
            accountRepo.getById(account.id).let {
                _processingAccount.value = it
            }
        }
    }

    fun addSMSOperator(
        accountId: String,
        address: String,
        parseRule: String,
        transactionType: TransactionType
    ) {
        viewModelScope.launch {
            accountRepo.getById(accountId)?.let { account ->
                val smsSenders = account.smsSenders.toMutableList()
                var smsOperator = smsSenders.find { it.address == address } ?: SMSOperator(
                    address,
                    mutableListOf()
                )

                smsSenders.removeIf { it.address == address }
                smsOperator = smsOperator.copy(
                    parsers = smsOperator.parsers.toMutableList() + SMSParser(
                        parseRule,
                        transactionType
                    )
                )
                smsSenders.add(smsOperator)
                accountRepo.update(
                    account.copy(
                        smsSenders = smsSenders
                    )
                )

                _processingAccount.value = null
            }
        }
    }

}