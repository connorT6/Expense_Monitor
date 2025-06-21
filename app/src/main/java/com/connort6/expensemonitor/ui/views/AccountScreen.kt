package com.connort6.expensemonitor.ui.views

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.connort6.expensemonitor.R
import com.connort6.expensemonitor.repo.Account
import com.connort6.expensemonitor.ui.AccountData
import com.connort6.expensemonitor.ui.AccountViewModel
import com.connort6.expensemonitor.ui.theme.ExpenseMonitorTheme

@Composable
fun AccountScreen(
    navController: NavController = rememberNavController(),
    accountViewModel: AccountViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val accountState by accountViewModel.accountsState.collectAsState()
    AccountsView(accountState, {

    })
}

@Composable
private fun AccountsView(accountState: AccountData, delAcc: (String) -> Unit) {
    Column {
        Row {
            Button(onClick = {

            }) {
                Text("Add Account")
            }
        }
        Column {
            for (acc in accountState.accounts) {
                AccountItem(acc, { id ->
                    delAcc.invoke(id)
                })
            }
        }
    }


}

@Composable
private fun AccountItem(acc: Account, delete: (String) -> Unit) {
    var showDelDialog by remember { mutableStateOf(false) }

    Row {
        Image(painterResource(R.drawable.ic_bank), null)
        Spacer(modifier = Modifier.width(8.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(acc.name)
            Text("LKR ${"%.2f".format(acc.balance)}")
        }

        IconButton(
            onClick = {
                showDelDialog = true
            }
        ) {
            Icon(
                Icons.Default.Delete, contentDescription = "",
                tint = Color.Red
            )
        }
    }

    if (showDelDialog) {
        ShowDeleteDialog(
            onConfirm = {
                delete.invoke(acc.id)
            },
            onCancel = {
                showDelDialog = false
            }
        )
    }
}

@Composable
fun ShowDeleteDialog(onConfirm: () -> Unit, onCancel: () -> Unit) {
    var enabled by remember { mutableStateOf(true) }

    Dialog(
        onDismissRequest = { /* Do nothing to prevent dismissal */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text("Do you want to delete this account?")
                Spacer(modifier = Modifier.height(16.dp))
                Row {
                    Button(
                        onClick = {
                            onConfirm.invoke()

                        },
                        enabled = enabled,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) {
                        Text("Yes")
                    }
                    Spacer(modifier = Modifier.width(15.dp))
                    Button(
                        onClick = { onCancel.invoke() },
                        enabled = enabled
                    ) {
                        Text("No")
                    }
                }
            }
        }
    }
}


@Preview
@Composable
private fun PreviewAcc() {

    // Fake ViewModel data
    val fakeAccounts = listOf(
        Account(name = "Savings", balance = 1200.0),
        Account(name = "Credit Card", balance = -300.0),
    )

    // Simulate ViewModel state
    val fakeState = AccountData(accounts = fakeAccounts)

    ExpenseMonitorTheme {
        AccountsView(fakeState, {})
    }

}

@Preview
@Composable
private fun PreviewDialog() {
    ShowDeleteDialog({}, {})
}