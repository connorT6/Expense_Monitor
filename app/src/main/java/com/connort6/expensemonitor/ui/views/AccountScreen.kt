package com.connort6.expensemonitor.ui.views

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.connort6.expensemonitor.R
import com.connort6.expensemonitor.repo.Account
import com.connort6.expensemonitor.repo.SMSParseKeys
import com.connort6.expensemonitor.repo.TransactionType
import com.connort6.expensemonitor.ui.theme.ExpenseMonitorTheme


data class AddOrEditPopupData(
    var dialogShown: Boolean = false,
    var name: String? = null,
    var balance: String? = null,
    var image: String? = null
)


@Composable
fun AccountScreen(
    navController: NavController,
    iconPickerViewModel: IconPickerViewModel
) {
    val accountViewModel: AccountViewModel = viewModel()
    val accountState by accountViewModel.accountsState.collectAsState()
    val addOrEditData by accountViewModel.addOrEditData.collectAsState()
    val pickerResult by iconPickerViewModel.pickerResult.collectAsState()
    var showAddSms by remember { mutableStateOf(false) }

    LaunchedEffect(pickerResult) {
        pickerResult.takeIf { it.selected }?.let {
            iconPickerViewModel.cleanResult()
            it
        }?.name?.let { iconName ->
            accountViewModel.setAddAccIcon(iconName)
        }
    }

    AccountsView(
        accountState, {
            //TODO show add acc
            accountViewModel.showAddAcc(true)
//            accountViewModel.addAccount(account)
        }, { id ->
            accountViewModel.deleteAccount(id)
        }, { acc ->
            accountViewModel.setSelectedAcc(acc)
            showAddSms = true
        }
    )

    if (addOrEditData.dialogShown) {
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
            }, addOrEditData
        )
    }

    if (showAddSms) {
        AddSMSOperators(accountViewModel, { showAddSms = false })
    }
}

@Composable
private fun AccountsView(
    accountState: AccountData,
    addAcc: () -> Unit,
    delAcc: (String) -> Unit,
    addSMS: (Account) -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp), verticalAlignment = Alignment.CenterVertically
        ) {

            Text(
                "LKR ${"%.2f".format(accountState.mainAccount.balance)}",
                modifier = Modifier.weight(1f)
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
                ListItem(acc.name, acc.balance, acc.iconName, R.drawable.ic_bank, acc.id, { id ->
                    delAcc.invoke(id)
                }, {
                    addSMS.invoke(acc)
                })
            }
        }
    }

//
//    LaunchedEffect(accountState) {
//        showAddAcc = true
//    }
}

@Composable
fun ListItem(
    name: String,
    balance: Double?,
    iconName: String,
    defaultIcon: Int,
    itemId: String,
    delete: (String) -> Unit,
    showSMSDialog: (() -> Unit)? = null
) {
    var showDelDialog by remember { mutableStateOf(false) }
//    var showSMSDialog by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painterResource(getDrawableResIdFromR(iconName) ?: defaultIcon),
            null,
            modifier = Modifier.weight(0.25f)
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

        if (showSMSDialog != null) {
            IconButton(
                onClick = {
                    showSMSDialog.invoke()
                }) {
                Icon(
                    Icons.Default.Mail, contentDescription = ""
                )
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
    screenData: AddOrEditPopupData
) {
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
                        Icons.Default.Refresh,
                        contentDescription = "",
                        modifier = Modifier.clickable {
                            onOpenIconPicker?.invoke()
                        })
                } else {
                    Image(
                        painterResource(resId),
                        contentDescription = "",
                        modifier = Modifier.clickable {
                            onOpenIconPicker?.invoke()
                        })
                }


                OutlinedTextField(value = text, onValueChange = {
                    onNameEdit?.invoke(it)
                }, label = { Text("Name") })

                Spacer(modifier = Modifier.height(16.dp))

                onBalanceEdit?.let {
                    OutlinedTextField(
                        value = balance,
                        onValueChange = { typedValue ->
                            if (typedValue.all { it.isDigit() }) {
                                onBalanceEdit.invoke(typedValue)
                            }
                        },
                        label = { Text("Balance") },
                        keyboardOptions = KeyboardOptions.Default.copy(
                            keyboardType = KeyboardType.Number
                        ),
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                }

                DialogBottomRow(
                    onAdd,
                    onCancel
                ) {
                    text.isEmpty() || (onBalanceEdit != null && balance.isEmpty())
                }
            }
        }
    }
}

@Composable
fun DialogBottomRow(
    onAdd: () -> Unit,
    onCancel: () -> Unit,
    checkAllowed: () -> Boolean
) {
    var buttonsEnabled by remember { mutableStateOf(true) }
    val context = LocalContext.current
    Row {
        Button(
            onClick = {
                if (!checkAllowed.invoke()) {
                    Toast.makeText(context, "Empty value", Toast.LENGTH_LONG).show()
                }
                onAdd.invoke()
                buttonsEnabled = false
            },
            enabled = buttonsEnabled
        ) {
            Text("Save")
        }
        Spacer(modifier = Modifier.width(15.dp))
        Button(
            onClick = {
                onCancel.invoke()
                buttonsEnabled = false
            }, enabled = buttonsEnabled,
            colors = ButtonDefaults.buttonColors(containerColor = Color.Red)

        ) {
            Text("Cancel")
        }
    }
}

@Composable
fun AddSMSOperators(accountViewModel: AccountViewModel, onCancel: () -> Unit) {
    val account by accountViewModel.processingAccount.collectAsState()
    LaunchedEffect(account) {
        // Wait for 3 seconds
        kotlinx.coroutines.delay(3000)
        if (account == null) {
            onCancel()
        }
    }
    account?.let {
        AddSMSOperatorsView(it, { address, parseRule, transactionType ->
            accountViewModel.addSMSOperator(account!!.id, address, parseRule, transactionType)
            onCancel.invoke()
        }, onCancel)
    }
}

@Composable
fun AddSMSOperatorsView(
    account: Account,
    onAdd: (address: String, parseRule: String, transactionType: TransactionType) -> Unit,
    onCancel: () -> Unit
) {

    // map will have all enums and if it has been used in the parser text or not
    val enumKeys: Map<SMSParseKeys, Boolean> = SMSParseKeys.entries.associateBy({ it }, { false })

    var addressText by remember { mutableStateOf("") }
    var parseRule by remember { mutableStateOf(TextFieldValue("")) }
    val scrollState = rememberScrollState()
    var keyState by remember { mutableStateOf(enumKeys) }
    var transactionType by remember { mutableStateOf(TransactionType.CREDIT) }

    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 8.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Add SMS Operator for ${account.name}",
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = addressText,
                    onValueChange = { addressText = it },
                    label = { Text("Address") }
                )
                Spacer(modifier = Modifier.height(16.dp))

                DropdownTextField(TransactionType.entries, {
                    transactionType = it
                }, { type -> type.name })

                OutlinedTextField(
                    value = parseRule,
                    onValueChange = { newValue ->
                        val oldText = parseRule.text
                        val newText = newValue.text
                        if (newText.length > oldText.length) {
                            parseRule = newValue
                            return@OutlinedTextField
                        }

                        val start = parseRule.selection.start
                        val end = parseRule.selection.end
                        if (start != end) {
                            parseRule = newValue
                            return@OutlinedTextField //TODO handle selection
                        }
                        val textBefore = parseRule.text.substring(0, start)
                        val textAfter = parseRule.text.substring(start)
                        val lastWordRegex = Regex("[A-Z]+$") // getting last word
                        val lastWord =
                            lastWordRegex.find(textBefore)?.value
                        if (lastWord == null) {
                            parseRule = newValue
                            return@OutlinedTextField
                        }

                        val matchingKey =
                            keyState.entries.find { it.value && it.key.name == lastWord }?.key
                        if (matchingKey == null) {
                            parseRule = newValue
                            return@OutlinedTextField
                        }

                        val replace = textBefore.replace(lastWordRegex, "")
                        val finalText = replace + textAfter
                        parseRule = TextFieldValue(finalText, TextRange(replace.length))
                        keyState = keyState + (matchingKey to false)
                    },
                    label = { Text("Parsing Rule") },
                    modifier = Modifier
                        .verticalScroll(scrollState),
                    maxLines = 100,
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {

                        }
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))

                if (keyState.filter { !it.value }.isNotEmpty()) {
                    Row {
                        keyState.filter { !it.value }.forEach {
                            Card({
                                val text = parseRule.text + it.key.name
                                parseRule =
                                    parseRule.copy(text = text, selection = TextRange(text.length))
                                keyState = keyState + (it.key to true)
                            }) {
                                Text(it.key.name)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // TODO: Add UI for SMS Rules (keywords, amount extraction logic)
                DialogBottomRow(onAdd = {
                    onAdd.invoke(
                        addressText,
                        parseRule.text,
                        transactionType
                    )
                }, onCancel = onCancel) {
                    addressText.isNotEmpty()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> DropdownTextField(
    options: List<T>,
    onOptionSelected: (T) -> Unit,
    labelProcessor: (T) -> String,
    modifier: Modifier = Modifier
) {


    var expanded by remember { mutableStateOf(false) }
    var selectedOption by remember { mutableStateOf(if (options.isEmpty()) null else options.first()) }
    var textFieldSize by remember { mutableStateOf(Size.Zero) }

    val icon = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown

    val enabled = options.isNotEmpty()

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (enabled) expanded = it },
        modifier = Modifier.focusable(enabled)
    ) {
        OutlinedTextField(
            value = selectedOption?.let { labelProcessor.invoke(it) } ?: "",
            onValueChange = { }, // Empty to prevent typing
            modifier = modifier
                .onGloballyPositioned { coordinates ->
                    textFieldSize = coordinates.size.toSize()
                }
                .menuAnchor(MenuAnchorType.PrimaryNotEditable), // Use default menuAnchor
            readOnly = true, // Prevents typing
            label = { Text("Select Option") },
            enabled = enabled,
            trailingIcon = {
                Icon(
                    icon,
                    contentDescription = "Dropdown Icon"
                )
            }
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.width(with(LocalDensity.current) { textFieldSize.width.toDp() })
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(labelProcessor.invoke(option)) },
                    onClick = {
                        selectedOption = option
                        onOptionSelected.invoke(option)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
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
                        },
                        enabled = enabled
                    ) {
                        Text("Yes")
                    }
                    Spacer(modifier = Modifier.width(15.dp))
                    Button(
                        onClick = {
                            onCancel.invoke()
                            enabled = false
                        }, enabled = enabled,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
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
        AccountsView(fakeState, {}, {}, {})
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
fun ItemPreview() {
    ExpenseMonitorTheme {
        ListItem("name", null, "", R.drawable.ic_amazon, "test id", {})
    }
}

@Preview
@Composable
private fun PreviewAdd() {
    val screenData = AddOrEditPopupData(false, "Test name", "52454.15", "")
    AddOrEditAccount({}, {}, screenData = screenData, onBalanceEdit = {})
}

@Preview
@Composable
private fun PreviewDialog() {
    ShowDeleteDialog({}, {})
}

@Preview
@Composable
fun AddSMSOperatorsPreview() {
    ExpenseMonitorTheme {

        AddSMSOperatorsView(
            account = Account(
                id = "1",
                name = "Test Account",
                balance = 100.0,
                iconName = "ic_bank"
            ),
            onAdd = { _, _, _ -> },
            onCancel = {}
        )
    }
}
