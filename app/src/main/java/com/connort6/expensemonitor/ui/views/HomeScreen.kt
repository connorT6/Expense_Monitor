package com.connort6.expensemonitor.ui.views

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerState
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.connort6.expensemonitor.R
import com.connort6.expensemonitor.repo.Account
import com.connort6.expensemonitor.repo.Category
import com.connort6.expensemonitor.repo.SmsMessage
import com.connort6.expensemonitor.repo.Transaction
import com.connort6.expensemonitor.repo.TransactionType
import com.connort6.expensemonitor.ui.theme.ExpenseMonitorTheme
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale

data class AutoCompleteObj(val id: String, val name: String, val obj: Any? = null)

@Composable
fun HomeScreen(
    navController: NavController,
    homeScreenViewModel: IHomeScreenViewModel,
    smsViewModel: ISmsViewModel
) {

    val accountTotal by homeScreenViewModel.accountTotal.collectAsState()

    val showCreateTransaction by homeScreenViewModel.showCreateTransaction.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "LKR ${"%.2f".format(accountTotal.balance)}",
                    fontSize = 20.sp,
                    modifier = Modifier.weight(1f)
                )
                Icon(Icons.Default.Edit, contentDescription = "", modifier = Modifier.clickable {
                    navController.navigate("accountPage")
                })
            }
            Spacer(Modifier.height(8.dp))
            Button({
                navController.navigate("categoryScreen")
            }) {
                Text("Categories")
            }
            Spacer(Modifier.height(8.dp))
            Button({
                navController.navigate("transactionScreen")
            }) {
                Text("Transactions")
            }
            Spacer(Modifier.height(8.dp))
            Button({
                navController.navigate("smsReader")
            }) {
                Text("SMS")
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp), horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier
                    .fillMaxHeight()
                    .clickable {
                        homeScreenViewModel.showCreateTransaction(true)
                        homeScreenViewModel.selectTransactionType(TransactionType.CREDIT)
                    }) {
                Image(
                    painter = painterResource(id = R.drawable.ic_plus_2),
                    contentDescription = "Income",

                    )
                Text("Income")
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier
                    .fillMaxHeight()
                    .clickable {
                        homeScreenViewModel.showCreateTransaction(true)
                        homeScreenViewModel.selectTransactionType(TransactionType.DEBIT)
                    }) {
                Image(
                    painter = painterResource(id = R.drawable.ic_minus_2),
                    contentDescription = "Expense"

                )
                Text("Expense")
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxHeight()
                    .clickable {
                        homeScreenViewModel.showCreateTransaction(true)
                        homeScreenViewModel.selectTransactionType(TransactionType.CREDIT)
                    }) {
                Image(
                    painter = painterResource(id = R.drawable.ic_transfer),
                    contentDescription = "Swap",

                    )
                Text("Swap")
            }

        }
    }

    if (showCreateTransaction) {
        ShowTransactionView(
            homeScreenViewModel,
            onDismiss = {
                homeScreenViewModel.showCreateTransaction(false)
                smsViewModel.selectSmsMessage(null)
            },
            navigateToSms = { navController.navigate("smsReader") },
            smsViewModel = smsViewModel
        )
    }
}

@Composable
fun ShowTransactionView(
    homeScreenViewModel: IHomeScreenViewModel,
    onDismiss: () -> Unit,
    transactionToEdit: Transaction? = null,
    navigateToSms: () -> Unit = {},
    smsViewModel: ISmsViewModel
) {

    //TODO close sms reader when selected
    CreateTransactionView(
        homeScreenViewModel,
        onDismiss,
        transactionToEdit,
        { accountId ->
            if (accountId?.isNotEmpty() ?: false) {
                smsViewModel.filterSmsByAccountId(accountId)
            }
            smsViewModel.setOpenType(OpenType.SELECTION)
            navigateToSms.invoke()
        },
        smsViewModel.selectedSmsMessage.collectAsState().value
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateTransactionView(
    homeScreenViewModel: IHomeScreenViewModel,
    onDismiss: () -> Unit,
    transactionToEdit: Transaction? = null,
    openSMSView: (accountId: String?) -> Unit,
    selectedMessage: SmsMessage? = null
) {
    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val timeFormatter = DateTimeFormatter.ofPattern("hh:mm a")

    val accounts by homeScreenViewModel.accounts.collectAsState()
    val categories by homeScreenViewModel.categories.collectAsState()
    val selectedAccount by homeScreenViewModel.selectedAccount.collectAsState()
    val selectedCategory by homeScreenViewModel.selectedCategory.collectAsState()
    val transactionAmount by homeScreenViewModel.transactionAmount.collectAsState()
    val selectedDateState by homeScreenViewModel.selectedDate.collectAsState()
    val selectedTimeState by homeScreenViewModel.selectedTime.collectAsState()
    val smsOperators by homeScreenViewModel.smsOperators.collectAsState()

    val amountFocusRequester by remember { mutableStateOf(FocusRequester()) }
    var amountFocused by remember { mutableStateOf(false) }

    var amountFieldValue by remember { mutableStateOf(TextFieldValue()) }

    LaunchedEffect(amountFocused) {
        if (amountFocused) {
            amountFieldValue =
                amountFieldValue.copy(selection = TextRange(amountFieldValue.text.length, 0))
        }
    }

    LaunchedEffect(transactionAmount) {
        amountFieldValue = amountFieldValue.copy(
            transactionAmount.setScale(
                2,
                RoundingMode.HALF_UP
            ).toPlainString()
        )
    }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }


    var selectedDate by remember { mutableStateOf(formatter.format(selectedDateState.time)) }

    LaunchedEffect(selectedDateState) {
        selectedDate = formatter.format(selectedDateState.time)
    }


    var selectedTime by remember { mutableStateOf(timeFormatter.format(LocalTime.now())) }

    LaunchedEffect(selectedTimeState) {
        selectedTime = timeFormatter.format(selectedTimeState)
    }

    val interactionSource = remember { MutableInteractionSource() }

    if (showDatePicker) {
        DatePickerPopUp(
            homeScreenViewModel, selectedDateState
        ) {
            showDatePicker = false
            // Date selection handled in DatePick composable
        }
    }

    if (showTimePicker) {
        TimePickerDialog(homeScreenViewModel, selectedTimeState) {
            showTimePicker = false
        }
    }
    Dialog(
        onDismiss,
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium, tonalElevation = 8.dp
        ) {

            Column(
                modifier = Modifier.padding(16.dp)
            ) {

                DropDownOutlineTextField(
                    "Account",
                    accounts.map {
                        AutoCompleteObj(it.id, it.name, it)
                    },
                    {
                        homeScreenViewModel.selectAccount(it.obj as Account)
                    },
                    defaultText = selectedAccount?.name
                )

                Spacer(Modifier.height(12.dp))

                DropDownOutlineTextField(
                    "Category",
                    categories.map {
                        AutoCompleteObj(it.id, it.name, it)
                    },
                    {
                        homeScreenViewModel.selectCategory(it.obj as Category)
                    },
                    defaultText = selectedCategory?.name
                )
                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = selectedDate,
                    onValueChange = { },
                    label = { Text("Date") },
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = {
                            showDatePicker = !showDatePicker
                        }) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = "Select date"
                            )
                        }
                    }
                )

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = selectedTime,
                    onValueChange = { },
                    label = { Text("Time") },
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { showTimePicker = !showTimePicker }) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = "Select time"
                            )
                        }
                    }
                )

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = amountFieldValue,
                    onValueChange = { fieldValue ->
                        val typedValue = fieldValue.text
                        // Allow up to 2 decimal places
                        val regex = Regex("^\\d+\\.?\\d{0,2}$")
                        if (typedValue.isEmpty() || regex.matches(typedValue)) {
                            amountFieldValue = fieldValue
                            homeScreenViewModel.setTransactionAmount(
                                if (typedValue.isEmpty()) BigDecimal.ZERO
                                else BigDecimal(
                                    typedValue
                                )
                            )
                        }
                    },
                    label = {
                        Text("Amount")
                    },
                    modifier = Modifier
                        .focusRequester(amountFocusRequester)
                        .onFocusChanged {
                            amountFocused = it.isFocused
                            if (it.isFocused) { // if all are 0 select highlight all text
                                amountFocusRequester.freeFocus()
                            } else {
                                val amountText =
                                    transactionAmount.setScale(2, RoundingMode.HALF_UP)
                                        .toPlainString()
                                amountFieldValue =
                                    TextFieldValue(amountText)
                            }
                        },
                    keyboardOptions = KeyboardOptions.Default.copy(
                        keyboardType = KeyboardType.Number
                    )
                )

                if (selectedMessage != null) {
                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = selectedMessage.body,
                        onValueChange = { fieldValue ->
                        },
                        label = {
                            Text("Message")
                        },
                        readOnly = true,
                        maxLines = 5,
                        trailingIcon = {
                            IconButton(onClick = {
                                selectedAccount?.id.let {
                                    openSMSView.invoke(it)
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Select date"
                                )
                            }
                        }
                    )


                } else {
                    Spacer(Modifier.height(18.dp))

                    Icon(
                        Icons.Default.Mail, contentDescription = "",
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .scale(2f)
                            .clickable(
                                onClick = {
                                    selectedAccount?.id.let {
                                        openSMSView.invoke(it)
                                    }
                                },
                                role = Role.Button,
                                interactionSource = interactionSource,
                                indication = ripple( // Use the new ripple from Material 3
                                    bounded = false,
                                    radius = 20.dp,
                                    color = Color.Unspecified // Optional: Or MaterialTheme.colorScheme.primary for example
                                )// Adjust ripple radius if needed
                            )
                    )
                }

                Spacer(Modifier.height(12.dp))

                DialogBottomRow({
                    homeScreenViewModel.createTransaction()
                    onDismiss.invoke()
                }, onDismiss, { true })
            }
        }
    }

    if (transactionToEdit != null) {
        homeScreenViewModel.selectAccount(transactionToEdit.account!!)
        homeScreenViewModel.selectCategory(transactionToEdit.category!!)
        val calendar = Calendar.getInstance()
        calendar.time = transactionToEdit.createdTime.toDate()
        homeScreenViewModel.selectDate(calendar)
        homeScreenViewModel.selectTime(
            transactionToEdit.createdTime.toDate().toInstant()
                .atZone(java.time.ZoneId.systemDefault()).toLocalTime()
        )
        homeScreenViewModel.setTransactionAmount(BigDecimal(transactionToEdit.amount))
    }

}

@Composable
private fun DropDownOutlineTextField(
    label: String,
    suggestions: List<AutoCompleteObj>,
    onItemSelect: (AutoCompleteObj) -> Unit,
    focusRequester: FocusRequester? = null,
    defaultText: String? = null
) {

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // Handle back press
    BackHandler {
        focusManager.clearFocus(true)
        keyboardController?.hide()
    }

    var text by remember { mutableStateOf(defaultText ?: "") }
    var expanded by remember { mutableStateOf(false) }
    val fieldValue = TextFieldValue(text = text, selection = TextRange(text.length))

    LaunchedEffect(defaultText) {
        text = defaultText ?: ""
    }

    val filteredSuggestions: List<AutoCompleteObj>
    if (text.isEmpty()) {
        filteredSuggestions = suggestions
    } else {
        filteredSuggestions = suggestions.filter {
            it.name.contains(text, ignoreCase = true) && text.isNotBlank()
        }
    }

    var modifier = Modifier
        .onFocusChanged { state ->
            expanded = state.isFocused
        }
    if (focusRequester != null) {
        modifier = modifier.focusRequester(focusRequester)
    }
    Column {
        OutlinedTextField(
            label = { Text(label) },
            singleLine = true,
            value = fieldValue,
            onValueChange = { newText ->
                text = newText.text
                if (!expanded) {
                    expanded = true
                }
            },
//            placeholder = { Text("Select category") },
            modifier = modifier,
            trailingIcon = {
                if (text.isNotEmpty()) {
                    IconButton(onClick = {
                        text = ""
                        expanded = false
                    }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            }
        )

        // Dropdown suggestions
        if (expanded && filteredSuggestions.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 200.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                DropDownList(
                    filteredSuggestions,
                    text,
                    { expanded = false },
                    {
                        text = it.name
                        onItemSelect.invoke(it)
                    })
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerPopUp(
    homeScreenViewModel: IHomeScreenViewModel,
    selectedDate: Calendar,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState()

    DatePickerDialog(
        onDismissRequest = {
            // Dismiss the dialog when the user clicks outside the dialog or on the back
            // button. If you want to disable that functionality, simply use an empty
            // onDismissRequest.
            onDismiss.invoke()
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (datePickerState.selectedDateMillis == null) {
                        return@TextButton
                    }
                    selectedDate.timeInMillis = datePickerState.selectedDateMillis!!
                    homeScreenViewModel.selectDate(selectedDate)
                    onDismiss.invoke()
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = { onDismiss.invoke() }) { Text("Cancel") }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    homeScreenViewModel: IHomeScreenViewModel,
    selectedTime: LocalTime,
    onDismiss: () -> Unit
) {
    val timePickerState: TimePickerState = rememberTimePickerState(
        initialHour = selectedTime.hour,
        initialMinute = selectedTime.minute,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        dismissButton = {
            TextButton(onClick = {
                onDismiss.invoke()
            }) {
                Text("Dismiss")
            }
        },
        confirmButton = {
            TextButton(onClick = {
                homeScreenViewModel.selectTime(
                    LocalTime.of(
                        timePickerState.hour,
                        timePickerState.minute
                    )
                )
                onDismiss.invoke()
            }) {
                Text("OK")
            }
        },
        text = {
            TimePicker(
                state = timePickerState,
            )
        }
    )
}

@Composable
private fun DropDownList(
    filteredSuggestions: List<AutoCompleteObj>,
    text: String,
    onDismiss: () -> Unit,
    onItemSelect: (AutoCompleteObj) -> Unit
) {
    var text1 = text
    LazyColumn {
        items(filteredSuggestions.size) { index ->
            val currentObj = filteredSuggestions[index]
            val suggestion = currentObj.name
            DropdownMenuItem(
                text = {
                    Text(
                        text = buildAnnotatedString {
                            val startIndex = suggestion.indexOf(
                                text1,
                                ignoreCase = true
                            )
                            if (startIndex >= 0) {
                                append(
                                    suggestion.substring(
                                        0,
                                        startIndex
                                    )
                                )
                                withStyle(
                                    style = SpanStyle(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    append(
                                        suggestion.substring(
                                            startIndex,
                                            startIndex + text1.length
                                        )
                                    )
                                }
                                append(suggestion.substring(startIndex + text1.length))
                            } else {
                                append(suggestion)
                            }
                        }
                    )
                },
                onClick = {
                    text1 = suggestion
                    onItemSelect.invoke(currentObj)
                    onDismiss.invoke()
//                                                onSuggestionSelected(suggestion)
                }
            )
        }
    }
}

//-------------------------------------------------- Previews -------------------------------------------------------

@Composable
@Preview
private fun HomePreview() {
    ExpenseMonitorTheme {
        HomeScreen(
            rememberNavController(), MockHomeScreenViewModel(),
            MockSmsViewModel()
        )
    }
}

@Composable
@Preview
private fun TrPreview() {
    ExpenseMonitorTheme() {
        CreateTransactionView(
            MockHomeScreenViewModel(), {}, null,
            { }, SmsMessage(
                "asdfa", "8822",
                "Test body Test body Test body Test body Test body Test body Test body Test body Test body Test body Test body Test body Test body",
                1234567890, 1
            )
        )
    }
}

@Preview
@Composable
private fun DropDownPrev() {
    ExpenseMonitorTheme {
        val suggestions = listOf(
            "apple", "storm", "whisper", "galaxy", "river",
            "canvas", "marble", "echo", "lantern", "crystal",
            "ember", "horizon", "cascade", "meadow", "quartz",
            "serene", "voyage", "zephyr", "harbor", "myth",
            "forest", "breeze", "shadow", "flame", "aurora",
            "twilight", "dream", "pulse", "oasis", "spire",
            "dusk", "glimmer", "velvet", "shard", "ripple",
            "sage", "drift", "mystic", "clover", "solstice",
            "radiant", "silken", "celestial", "wander", "opal",
            "thistle", "ashen", "luminous", "echoes", "serenade"
        ).map {
            AutoCompleteObj(it, it)
        }
        Column {
            DropDownOutlineTextField("Account", suggestions, {})
        }
        DropDownOutlineTextField("Account", suggestions, {})
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun DatePickerPrev() {
    ExpenseMonitorTheme {
        DatePickerPopUp(MockHomeScreenViewModel(), Calendar.getInstance()) {}
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun TimePickPrev() {
    ExpenseMonitorTheme {
        TimePickerDialog(MockHomeScreenViewModel(), LocalTime.now()) {}
    }
}
