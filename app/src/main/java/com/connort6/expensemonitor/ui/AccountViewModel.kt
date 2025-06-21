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

class AccountViewModel : ViewModel() {
    private val _accountsState = MutableStateFlow(AccountData())
    val accountsState: StateFlow<AccountData> = _accountsState.asStateFlow()

    private var accounts: MutableList<Account> = mutableListOf()
    private val accountRepo = AccountRepo.getInstance()

    init {
        viewModelScope.launch {
            val allAccounts = accountRepo.getAllAccounts()
            accounts.clear()
            accounts.addAll(allAccounts)
            _accountsState.update { accountData ->
                accountData.copy(accounts = accounts)
            }
        }
    }

    fun deleteAccount(accId: String) {
        viewModelScope.launch {
            accountRepo.deleteAccount(accId)
            val allAccounts = accountRepo.getAllAccounts()
            accounts.clear()
            accounts.addAll(allAccounts)
            _accountsState.update { accountData ->
                accountData.copy(accounts = accounts)
            }
        }
    }

}