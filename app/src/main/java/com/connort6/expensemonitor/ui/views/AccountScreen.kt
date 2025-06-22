package com.connort6.expensemonitor.ui.views

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
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
    navController: NavController = rememberNavController(), accountViewModel: AccountViewModel = viewModel(), modifier: Modifier = Modifier
) {
    val accountState by accountViewModel.accountsState.collectAsState()
    AccountsView(
        accountState, { account ->
            accountViewModel.addAccount(account)
        }, { id ->
            accountViewModel.deleteAccount(id)
        },
        navController
    )
}

@Composable
private fun AccountsView(
    accountState: AccountData,
    addAcc: (Account) -> Unit,
    delAcc: (String) -> Unit,
    navController: NavController = rememberNavController()
) {
    var showAddAcc by remember { mutableStateOf(false) }
    Column {
        Row {
            Button(
                onClick = {
                    showAddAcc = true
                }
            ) {
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
    if (showAddAcc) {
        AddOrEditAccount(
            { name, balance, iconName ->
            val account = Account(name = name, balance = balance, iconName = iconName)
            addAcc.invoke(account)
            showAddAcc = false
        }, {
            showAddAcc = false
        },
            navController
        )
    }
//
//    LaunchedEffect(accountState) {
//        showAddAcc = true
//    }
}

@Composable
private fun AccountItem(acc: Account, delete: (String) -> Unit) {
    var showDelDialog by remember { mutableStateOf(false) }

    Row {
        Image(painterResource(getDrawableResIdFromR(acc.iconName) ?: R.drawable.ic_bank), null)
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
            }) {
            Icon(
                Icons.Default.Delete, contentDescription = "", tint = Color.Red
            )
        }
    }

    if (showDelDialog) {
        ShowDeleteDialog(onConfirm = {
            delete.invoke(acc.id)
            showDelDialog = false
        }, onCancel = {
            showDelDialog = false
        })
    }
}

@Composable
fun AddOrEditAccount(
    onAdd: (name: String, balance: Double, iconName: String) -> Unit,
    onCancel: () -> Unit,
    navController: NavController = rememberNavController()
) {
    val context = LocalContext.current
    var showIconPicker by remember { mutableStateOf(false) }
    var selectedIcon by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        navController.currentBackStackEntry?.savedStateHandle?.set(
            RESULT_KEY,
            null
        )
    }

    // Observe for results from SettingsScreen
    val resultFlow = navController.previousBackStackEntry
        ?.savedStateHandle
        ?.getStateFlow<PickerResult?>(RESULT_KEY, null)

    val result by resultFlow?.collectAsState() ?: remember { mutableStateOf(null) }

    LaunchedEffect(result) {
        result?.name?.let { name ->
            selectedIcon = name
        }
        navController.previousBackStackEntry?.savedStateHandle?.set(
            RESULT_KEY,
            null
        )
    }

    Dialog(
        onDismissRequest = { onCancel.invoke() }, properties = DialogProperties(
            dismissOnBackPress = false, dismissOnClickOutside = false
        )
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium, tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                var text by remember { mutableStateOf("") }
                var balance by remember { mutableStateOf("") }
                navController.currentBackStackEntry?.savedStateHandle?.set(
                    PARAM_KEY,
                    Regex(".+")
                )

                val resId = getDrawableResIdFromR(selectedIcon)
                if (resId == null) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "",
                        modifier = Modifier.clickable {
                            navController.navigate("iconPicker")
                        }
                    )
                } else {
                    Icon(
                        painterResource(resId),
                        contentDescription = "",
                        modifier = Modifier.clickable {
                            navController.navigate("iconPicker")
                        }
                    )
                }

                OutlinedTextField(value = text, onValueChange = {
                    text = it
                }, label = { Text("Name") })

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = balance,
                    onValueChange = { it ->
                        if (it.all { it.isDigit() }) {
                            balance = it
                        }
                    },
                    label = { Text("Balance") },
                    keyboardOptions = KeyboardOptions.Default.copy(
                        keyboardType = KeyboardType.Number
                    ),
                )

                Spacer(modifier = Modifier.height(16.dp))

                var buttonsEnabled by remember { mutableStateOf(true) }
                Row {
                    Button(
                        onClick = {
                            if (text.isEmpty() || balance.isEmpty()) {
                                Toast.makeText(context, "Empty value", Toast.LENGTH_LONG).show()
                            }
                            onAdd.invoke(text, balance.toDouble(), "")
                            buttonsEnabled = false
                        }, enabled = buttonsEnabled, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) {
                        Text("Save")
                    }
                    Spacer(modifier = Modifier.width(15.dp))
                    Button(
                        onClick = {
                            onCancel.invoke()
                            buttonsEnabled = false
                        }, enabled = buttonsEnabled
                    ) {
                        Text("Cancel")
                    }
                }
            }
        }
    }
}

@Composable
fun ShowDeleteDialog(onConfirm: () -> Unit, onCancel: () -> Unit) {
    var enabled by remember { mutableStateOf(true) }

    Dialog(
        onDismissRequest = { /* Do nothing to prevent dismissal */ }, properties = DialogProperties(
            dismissOnBackPress = false, dismissOnClickOutside = false
        )
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium, tonalElevation = 8.dp
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
                            enabled = false
                        }, enabled = enabled, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) {
                        Text("Yes")
                    }
                    Spacer(modifier = Modifier.width(15.dp))
                    Button(
                        onClick = {
                            onCancel.invoke()
                            enabled = false
                        }, enabled = enabled
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
        AccountsView(fakeState, {}, {})
    }

}

fun getDrawableResIdFromR(name: String): Int? {
    if (name.isEmpty()) {
        return null
    }
    return try {
        val field = R.drawable::class.java.getDeclaredField(name)
        field.getInt(null)
    } catch (e: Exception) {
        null
    }
}

@Preview
@Composable
private fun PreviewAdd() {
    AddOrEditAccount({ _, _, _ -> }, {})
}

@Preview
@Composable
private fun PreviewDialog() {
    ShowDeleteDialog({}, {})
}