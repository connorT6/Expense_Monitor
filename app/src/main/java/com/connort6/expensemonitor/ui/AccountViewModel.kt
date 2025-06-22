package com.connort6.expensemonitor.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.connort6.expensemonitor.repo.Account
import com.connort6.expensemonitor.repo.AccountRepo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AccountData(val accounts: List<Account> = emptyList())

enum class DEST {
    ICON_PICKER
}

class AccountViewModel : ViewModel() {
    private val _accountsState = MutableStateFlow(AccountData())
    val accountsState: StateFlow<AccountData> = _accountsState.asStateFlow()

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
    }

    fun addAccount(account: Account) {
        viewModelScope.launch {
            accountRepo.createAccount(account)
        }
    }

    fun deleteAccount(accId: String) {
        viewModelScope.launch {
            accountRepo.deleteAccount(accId)
        }
    }

}