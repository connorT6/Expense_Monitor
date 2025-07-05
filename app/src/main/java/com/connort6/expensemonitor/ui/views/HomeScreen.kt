package com.connort6.expensemonitor.ui.views

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.connort6.expensemonitor.R
import com.connort6.expensemonitor.ui.theme.ExpenseMonitorTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(navController: NavController) {

    val homeScreenViewModel: HomeScreenViewModel = viewModel()
    val accountTotal by homeScreenViewModel.accountTotal.collectAsState()
    HomeScreenContent(navController, accountTotal.balance)
}

@Composable
fun HomeScreenContent(
    navController: NavController, accountTotalBalance: Double
) {
    Column(
        modifier = Modifier.fillMaxSize(),
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

                    }) {
                Image(
                    painter = painterResource(id = R.drawable.ic_minus_2),
                    contentDescription = "Expense"

                )
                Text("Expense")
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxHeight()
                    .clickable {

                    }) {
                Image(
                    painter = painterResource(id = R.drawable.ic_transfer),
                    contentDescription = "Swap",

                    )
                Text("Swap")
            }

        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTransactionView(openDate: () -> Unit, openTime: () -> Unit) {
    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val timeFormatter = SimpleDateFormat("hh:mm a", Locale.getDefault())

    var calendar = Calendar.getInstance()
    val datePickerState = rememberDatePickerState(calendar.timeInMillis)
    val timePickerState = rememberTimePickerState(
        initialHour = calendar.get(Calendar.HOUR),
        initialMinute = calendar.get(Calendar.MINUTE),
    )

    var selectedDate = datePickerState.selectedDateMillis?.let {
        formatter.format(Date(it))
    } ?: formatter.format(calendar.time)


    var selectedTime = timeFormatter.format(calendar.time)

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    var text by remember { mutableStateOf("") }
    var isExpanded by remember { mutableStateOf(false) }

    var textFieldValue = TextFieldValue(text = text, selection = TextRange(text.length))


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
    )

    val filteredSuggestions: List<String>
    if (text.isEmpty()) {
        filteredSuggestions = suggestions
    } else {
        filteredSuggestions = suggestions.filter {
            it.contains(text, ignoreCase = true) && text.isNotBlank()
        }
    }


    LaunchedEffect(datePickerState) {

    }


    Dialog({}) {
        Surface(
            shape = MaterialTheme.shapes.medium, tonalElevation = 8.dp
        ) {
            if (showDatePicker) {
                DatePicker(
                    state = datePickerState,
                    showModeToggle = false
                )
            } else {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {

                    Column() {
                        OutlinedTextField(
                            value = textFieldValue,
                            onValueChange = { newText ->
                                text = newText.text
                                if (!isExpanded) {
                                    isExpanded = true
                                }
                            },
                            placeholder = { Text("Select category") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged { state ->
                                    isExpanded = state.isFocused
                                },
                            trailingIcon = {
                                if (text.isNotEmpty()) {
                                    IconButton(onClick = {
                                        text = ""
                                        isExpanded = false
                                    }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                                    }
                                }
                            }
                        )

                        // Dropdown suggestions
                        if (isExpanded && filteredSuggestions.isNotEmpty()) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 200.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                            ) {
                                LazyColumn {
                                    items(filteredSuggestions.size) { index ->
                                        val suggestion = filteredSuggestions[index]
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = buildAnnotatedString {
                                                        val startIndex = suggestion.indexOf(
                                                            text,
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
                                                                        startIndex + text.length
                                                                    )
                                                                )
                                                            }
                                                            append(suggestion.substring(startIndex + text.length))
                                                        } else {
                                                            append(suggestion)
                                                        }
                                                    }
                                                )
                                            },
                                            onClick = {
                                                text = suggestion
                                                isExpanded = false
//                                                onSuggestionSelected(suggestion)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = selectedDate,
                        onValueChange = { },
                        label = { Text("Date") },
                        readOnly = true,
                        trailingIcon = {
                            IconButton(onClick = { showDatePicker = !showDatePicker }) {
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
                }
            }
        }
    }


}


@Composable
@Preview
private fun TrPreview() {
    ExpenseMonitorTheme() {
        CreateTransactionView({}, {})
    }
}

@Composable
@Preview
private fun HomePreview() {
    ExpenseMonitorTheme {
        HomeScreenContent(rememberNavController(), 12345.67)
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
