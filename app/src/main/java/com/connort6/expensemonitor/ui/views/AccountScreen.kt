package com.connort6.expensemonitor.ui.views

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
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
import androidx.compose.ui.Alignment
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
import com.connort6.expensemonitor.ui.theme.ExpenseMonitorTheme


data class AccountScreenData(var dialogShown: Boolean = false, var name: String? = null, var balance: String? = null, var image: String? = null)


@Composable
fun AccountScreen(
    navController: NavController = rememberNavController(),
    accountViewModel: AccountViewModel = viewModel(),
    iconPickerViewModel: IconPickerViewModel
) {
    val accountState by accountViewModel.accountsState.collectAsState()
    val screenData by accountViewModel.accountScreenData.collectAsState()
    val pickerResult by iconPickerViewModel.pickerResult.collectAsState()

    LaunchedEffect(pickerResult) {
        pickerResult.takeIf { it.selected }.let {
            it?.name?.let { iconName ->
                accountViewModel.setAddAccIcon(iconName)
            }
        }
        iconPickerViewModel.cleanResult()
    }

    AccountsView(
        accountState, {
            //TODO show add acc
            accountViewModel.showAddAcc(true)
//            accountViewModel.addAccount(account)
        }, { id ->
            accountViewModel.deleteAccount(id)
        }, navController
    )

    if (screenData.dialogShown) {
        AddOrEditAccount(
            {
                accountViewModel.addAccount()
                accountViewModel.showAddAcc(false)
            },
            {
                accountViewModel.showAddAcc(false)
            },
            { name ->
                accountViewModel.setAddAccName(name)
            },
            { balance ->
                accountViewModel.setAddAccBalance(balance)
            },
            {
                navController.navigate("iconPicker")
            }, screenData
        )
    }
}

@Composable
private fun AccountsView(
    accountState: AccountData, addAcc: () -> Unit, delAcc: (String) -> Unit, navController: NavController = rememberNavController()
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp), verticalAlignment = Alignment.CenterVertically
        ) {

            Text(
                "LKR ${"%.2f".format(accountState.mainAccount.balance)}", modifier = Modifier.weight(1f)
            )

            Button(
                onClick = {
                    addAcc.invoke()
                }) {
                Icon(Icons.Default.AddCircle, contentDescription = null)

            }
        }
        Column {
            for (acc in accountState.accounts) {
                ListItem(acc.name, acc.balance, acc.iconName, R.drawable.ic_bank, acc.id) { id ->
                    delAcc.invoke(id)
                }
            }
        }
    }

//
//    LaunchedEffect(accountState) {
//        showAddAcc = true
//    }
}

@Composable
private fun ListItem(name: String, balance: Double?, iconName: String, defaultIcon: Int, itemId: String, delete: (String) -> Unit) {
    var showDelDialog by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painterResource(getDrawableResIdFromR(iconName) ?: defaultIcon), null, modifier = Modifier.weight(0.25f)
        )
        Spacer(modifier = Modifier.width(8.dp))

        Column(
            modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center
        ) {
            Text(name)
            balance?.let {
                Text("LKR ${"%.2f".format(it)}")
            }
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
            delete.invoke(itemId)
            showDelDialog = false
        }, onCancel = {
            showDelDialog = false
        })
    }
}

@Composable
fun AddOrEditAccount(
    onAdd: () -> Unit,
    onCancel: () -> Unit,
    onNameEdit: ((name: String) -> Unit)? = null,
    onBalanceEdit: ((balance: String) -> Unit)? = null,
    onOpenIconPicker: (() -> Unit)? = null,
    screenData: AccountScreenData
) {
    val context = LocalContext.current



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
                val text = screenData.name ?: ""
                val balance = screenData.balance?.toString() ?: ""


                val resId = getDrawableResIdFromR(screenData.image ?: "")
                if (resId == null) {
                    Icon(
                        Icons.Default.Refresh, contentDescription = "", modifier = Modifier.clickable {
                            onOpenIconPicker?.invoke()
                        })
                } else {
                    Image(
                        painterResource(resId), contentDescription = "", modifier = Modifier.clickable {
                            onOpenIconPicker?.invoke()
                        })
                }


                OutlinedTextField(value = text, onValueChange = {
                    onNameEdit?.invoke(it)
                }, label = { Text("Name") })

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = balance,
                    onValueChange = { it ->
                        if (it.all { it.isDigit() }) {
                            onBalanceEdit?.invoke(it)
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
                            onAdd.invoke()
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
        Account(name = "Credit Card"),
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
fun itemPreview() {
    ListItem("name", null, "", R.drawable.ic_amazon, "test id", {})
}

@Preview
@Composable
private fun PreviewAdd() {
    val screenData = AccountScreenData(false, "Test name", "52454.15", "")
    AddOrEditAccount({}, {}, screenData = screenData)
}

@Preview
@Composable
private fun PreviewDialog() {
    ShowDeleteDialog({}, {})
}