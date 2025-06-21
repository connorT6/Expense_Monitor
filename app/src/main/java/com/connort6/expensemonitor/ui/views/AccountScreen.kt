package com.connort6.expensemonitor.ui.views

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.connort6.expensemonitor.R
import com.connort6.expensemonitor.ui.AccountViewModel
import com.connort6.expensemonitor.ui.theme.ExpenseMonitorTheme

@Composable
fun AccountScreen(navController: NavController = rememberNavController(),
                  accountViewModel: AccountViewModel = viewModel(),
                  modifier: Modifier = Modifier.clipToBounds()) {
    val accountState by accountViewModel.accountsState.collectAsState()
    Column {
        Row {
            Button(onClick = {

            }) {
                Text("Add Account")
            }
        }
        Column {
            for (acc in accountState.accounts) {
                AccountItem(acc.name, R.drawable.ic_bank, acc.balance)
            }
        }
    }
}

@Composable
private fun AccountItem(name: String, imageId: Int, balance: Double) {
    Row {
        Image(painterResource(imageId), null)
        Column {
            Text(name)
            Text("LKR ${"%.2f".format(balance)}")
        }
    }
}

@Preview
@Composable
private fun PreviewAcc() {
    ExpenseMonitorTheme {
        AccountScreen()
    }

}