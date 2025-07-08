package com.connort6.expensemonitor.ui.views

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerState
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
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.painterResource
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.connort6.expensemonitor.R
import com.connort6.expensemonitor.repo.Transaction
import com.connort6.expensemonitor.ui.theme.ExpenseMonitorTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class AutoCompleteObj(val id: String, val name: String, val obj: Any? = null)

@Composable
fun HomeScreen(navController: NavController) {

    val homeScreenViewModel: HomeScreenViewModel = viewModel()
    val accountTotal by homeScreenViewModel.accountTotal.collectAsState()
    val accounts by homeScreenViewModel.accounts.collectAsState()
    val categories by homeScreenViewModel.categories.collectAsState()

    HomeScreenContent(
        navController, accountTotal.balance,
        accounts.map {
            AutoCompleteObj(it.id, it.name, it)
        }, categories.map {
            AutoCompleteObj(it.id, it.name, it)
        }
    ) {

    }
}

@Composable
fun HomeScreenContent(
    navController: NavController,
    accountTotalBalance: Double,
    accounts: List<AutoCompleteObj>,
    categories: List<AutoCompleteObj>,
    onSave: () -> Unit
) {
    var showCreateTransaction by remember { mutableStateOf(false) }

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
                    text = "LKR ${"%.2f".format(accountTotalBalance)}",
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
                        showCreateTransaction = true
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
                        showCreateTransaction = true
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
                        showCreateTransaction = true
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
        CreateTransactionView(
            accounts, categories,
            onSave, {
                showCreateTransaction = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTransactionView(
    accounts: List<AutoCompleteObj>,
    categories: List<AutoCompleteObj>,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
    onAccountSelect: (AutoCompleteObj) -> Unit,
    onCategorySelect: (AutoCompleteObj) -> Unit
) {
    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val timeFormatter = SimpleDateFormat("hh:mm a", Locale.getDefault())

    var calendar = Calendar.getInstance()
    val datePickerState = rememberDatePickerState()
    val timePickerState = rememberTimePickerState(
        initialHour = calendar.get(Calendar.HOUR),
        initialMinute = calendar.get(Calendar.MINUTE),
    )

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }


    val selectedDate = datePickerState.selectedDateMillis?.let {
        formatter.format(Date(it))
    } ?: formatter.format(calendar.time)


    val selectedTime = timePickerState.hour.let {
        calendar.set(Calendar.HOUR_OF_DAY, timePickerState.hour)
        calendar.set(Calendar.MINUTE, timePickerState.minute)
        timeFormatter.format(calendar.time)
    }

    if (showDatePicker) {
        DatePick(datePickerState, { showDatePicker = false }, {})
    }

    if (showTimePicker) {
        TimePickerDialog({
            showTimePicker = false
        }, {
            showTimePicker = false
        }) {
            TimePicker(
                state = timePickerState,
            )
        }
    }
    Dialog(
        onSave,
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
                    accounts,
                    {
                        onAccountSelect.invoke(it)
                    }
                )

                Spacer(Modifier.height(12.dp))

                DropDownOutlineTextField(
                    "Category",
                    categories,
                    {
                        onCategorySelect.invoke(it)
                    }
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
                    value = "0.0", onValueChange = { typedValue ->
                        if (typedValue.all { it.isDigit() }) {
//                                onBalanceEdit.invoke(typedValue)
                        }
                    },
                    label = {
                        Text("Amount")
                    },
                    keyboardOptions = KeyboardOptions.Default.copy(

                        keyboardType = KeyboardType.Number
                    )
                )
                Spacer(Modifier.height(12.dp))

                DialogBottomRow(onSave, onDismiss, { true })
            }
        }
    }
}

@Composable
private fun DropDownOutlineTextField(
    label: String,
    suggestions: List<AutoCompleteObj>,
    onItemSelect: (AutoCompleteObj) -> Unit,
    focusRequester: FocusRequester? = null
) {
    var text by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var fieldValue = TextFieldValue(text = text, selection = TextRange(text.length))

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
@Composable
fun DatePick(datePickerState: DatePickerState, onDismiss: () -> Unit, onItemSelect: () -> Unit) {
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
                    onDismiss.invoke()
//                    snackScope.launch {
//                        snackState.showSnackbar(
//                            "Selected date timestamp: ${datePickerState.selectedDateMillis}"
//                        )
//                    }
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

@Composable
fun TimePickerDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    content: @Composable () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        dismissButton = {
            TextButton(onClick = { onDismiss() }) {
                Text("Dismiss")
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm() }) {
                Text("OK")
            }
        },
        text = { content() }
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


@Composable
@Preview
private fun TrPreview() {
    ExpenseMonitorTheme() {
        CreateTransactionView(
            listOf(
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
            }, listOf(
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
            }, {}, {}, {}, {})
    }
}

@Composable
@Preview
private fun HomePreview() {
    ExpenseMonitorTheme {
        HomeScreenContent(
            rememberNavController(), 12345.67, listOf(
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
            }, listOf(
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
            }) {

        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun DatePickerPrev() {
    ExpenseMonitorTheme {
        val datePickerState = rememberDatePickerState()
        DatePicker(
            state = datePickerState,
            showModeToggle = false
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun TimePickPrev() {
    val timePickerState = rememberTimePickerState()
    ExpenseMonitorTheme {
        TimePickerDialog({}, {}) {
            TimePicker(
                state = timePickerState,
            )
        }
    }
}
