package com.connort6.expensemonitor.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.connort6.expensemonitor.repo.Account
import com.connort6.expensemonitor.repo.AccountRepo
import com.connort6.expensemonitor.ui.views.AccountScreenData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AccountData(val accounts: List<Account> = emptyList(), val mainAccount: Account = Account())


class AccountViewModel : ViewModel() {
    private val _accountsState = MutableStateFlow(AccountData())
    val accountsState: StateFlow<AccountData> = _accountsState.asStateFlow()

    private val _accountScreenData = MutableStateFlow(AccountScreenData())
    val accountScreenData = _accountScreenData.asStateFlow()

    private var accounts: MutableList<Account> = mutableListOf()
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
            _accountScreenData.value.let { it ->
                if (it.name != null && it.balance != null) {
                    val account = Account(name = it.name!!, balance = it.balance!!.toDouble(), iconName = it.image ?: "")
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
        _accountScreenData.update { data ->
            data.copy(dialogShown = shouldShow)
        }
    }

    fun setAddAccName(name: String) {
        _accountScreenData.update { data ->
            data.copy(name = name)
        }
    }

    fun setAddAccBalance(balance: String) {
        _accountScreenData.update { data ->
            data.copy(balance = balance)
        }
    }

    fun setAddAccIcon(icon: String) {
        _accountScreenData.update { data ->
            data.copy(image = icon)
        }
    }

    fun saveAcc() {
        clearAccData()
    }

    fun clearAccData() {
        _accountScreenData.update {
            AccountScreenData()
        }
    }

}