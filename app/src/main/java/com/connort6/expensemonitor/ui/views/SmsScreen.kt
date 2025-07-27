package com.connort6.expensemonitor.ui.views // Or your specific package

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.connort6.expensemonitor.repo.SMSOperator
import com.connort6.expensemonitor.repo.SmsMessage
import com.connort6.expensemonitor.ui.theme.ExpenseMonitorTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


@Composable
fun SmsReaderScreen(
    smsViewModel: SmsViewModel = viewModel()
) {
    val context = LocalContext.current
    // States from ViewModel
    val smsMessages by smsViewModel.smsMessages.collectAsStateWithLifecycle()
    val isLoading by smsViewModel.isLoading.collectAsStateWithLifecycle()
    val error by smsViewModel.error.collectAsStateWithLifecycle()
    val smsSenders by smsViewModel.smsSenders.collectAsState()

    var permissionGranted by remember {
        mutableStateOf(
            context.checkSelfPermission(Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
        )
    }

    val allowedSenders =
        remember { listOf("5555"/*, "ANOTHER_SENDER_ID"*/) } // Define your allowed senders

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        permissionGranted = isGranted
        if (isGranted) {
            smsViewModel.loadSmsMessages(SMSLoadMethod.BOUNDED_ONLY)
        } else {
            // Optionally, show a persistent message or disable features
        }
    }

    // Effect to load messages if permission is already granted when screen enters
    LaunchedEffect(key1 = permissionGranted, key2 = allowedSenders) {
        if (permissionGranted) {
            smsViewModel.loadSmsMessages(SMSLoadMethod.BOUNDED_ONLY)
        }
    }

    SmsReaderScreenView(
        smsMessages = smsMessages,
        isLoading = isLoading,
        error = error,
        permissionGranted = permissionGranted,
        onRequestPermission = {
            permissionLauncher.launch(Manifest.permission.READ_SMS)
        },
        onRetry = { smsViewModel.loadSmsMessages(SMSLoadMethod.BOUNDED_ONLY) },
        onSaveSms = { smsViewModel.saveSmsMessage(it) },
        allSenders = smsSenders,
        { filterOperators ->
            smsViewModel.loadSmsMessages(
                SMSLoadMethod.ALL,
                filterOperators.map { it.address })
        }
    )
}

@Composable
private fun SmsReaderScreenView(
    smsMessages: List<SmsMessage>,
    isLoading: Boolean,
    error: String?,
    permissionGranted: Boolean,
    onRequestPermission: () -> Unit,
    onRetry: () -> Unit,
    onSaveSms: (SmsMessage) -> Unit,
    allSenders: List<SMSOperator>,
    filterSenders: (List<SMSOperator>) -> Unit
) {

    var showSMSList by remember { mutableStateOf(true) }

    Column(
    ) {

        if (!permissionGranted) {
            Text("READ_SMS permission is required to display messages.")
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onRequestPermission) {
                Text("Request Permission")
            }
        } else {
            // Content when permission is granted
            if (isLoading) {
                CircularProgressIndicator()
            } else if (error != null) {
                Text("Error: $error", color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onRetry) {
                    Text("Retry")
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Absolute.Right,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DropdownTextField(allSenders, {
                        filterSenders(listOf(it))
                    }, { it.address }, Modifier.weight(1f))

                    Spacer(modifier = Modifier.height(8.dp))

                    MinimalDropdownMenu({ showSMSList = !showSMSList })
                }

                if (smsMessages.isEmpty()) {
                    Text("No matching SMS messages found.")
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) { // Fill available space
                        if (showSMSList) {
                            items(
                                smsMessages,
                                key = { it.id }) { sms -> // Add a key for better performance
                                SmsItemView(sms) {
                                    onSaveSms(it)
                                }

                                HorizontalDivider()
                            }
                        } else {
                            items(allSenders) { sender ->
                                SenderItemView(sender.address)
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun SmsItemView(sms: SmsMessage, onItemClick: (SmsMessage) -> Unit = {}) {
    Column(
        modifier = Modifier
            .clickable {
                onItemClick.invoke(sms)
            }
            .padding(vertical = 8.dp, horizontal = 8.dp)
            .fillMaxWidth()
    ) {
        Text(
            "From: ${sms.address}",
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            "Date: ${
                SimpleDateFormat(
                    "dd/MM/yyyy HH:mm:ss",
                    Locale.getDefault()
                ).format(Date(sms.date))
            }",
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            sms.body,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun SenderItemView(address: String) {
    Text(
        address,
        style = MaterialTheme.typography.titleMedium
    )
}

@Composable
fun MinimalDropdownMenu(showSenders: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .padding(end = 16.dp),
        contentAlignment = Alignment.Center

    ) {
        IconButton(onClick = { expanded = !expanded }) {
            Icon(Icons.Default.MoreVert, contentDescription = "More options")
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("Senders") },
                onClick = { showSenders.invoke() }
            )
            DropdownMenuItem(
                text = { Text("Option 2") },
                onClick = { /* Do something... */ }
            )
        }
    }
}


@Preview(showBackground = true)
@Composable
fun SmsReaderScreenPreview() {
    ExpenseMonitorTheme {
        val sampleMessages = listOf(
            SmsMessage(
                "1",
                "5555",
                "This is a test SMS message body 1. It contains some text.",
                System.currentTimeMillis() - 100000,
                type = 1 // Assuming 1 for received, adjust as needed
            ),
            SmsMessage(
                "2",
                "5555",
                "Another test SMS from the same sender with different content.",
                System.currentTimeMillis() - 200000,
                type = 1 // Assuming 1 for received
            ),
            SmsMessage(
                "3",
                "Friend",
                "Hello!",
                System.currentTimeMillis() - 300000,
                type = 2 // Assuming 2 for sent, adjust as needed
            )
        )
        SmsReaderScreenView(
            smsMessages = sampleMessages,
            isLoading = false,
            error = null,
            permissionGranted = true,
            onRequestPermission = {},
            onRetry = {},
            onSaveSms = {},
            allSenders = sampleMessages.map { SMSOperator(it.address) },
            {}
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoCompleteTextView(
    modifier: Modifier = Modifier,
    label: String = "Search",
    allSuggestions: List<String> // List of all possible suggestions
) {
    var text by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var filteredSuggestions by remember { mutableStateOf(allSuggestions) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { newText ->
                text = newText
                filteredSuggestions = if (newText.isNotBlank()) {
                    allSuggestions.filter {
                        it.contains(newText, ignoreCase = true)
                    }
                } else {
                    allSuggestions
                }
                expanded = filteredSuggestions.isNotEmpty()
            },
            label = { Text(label) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(), // Important: This anchors the dropdown to the text field
            singleLine = true
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            filteredSuggestions.forEach { suggestion ->
                DropdownMenuItem(
                    text = { Text(suggestion) },
                    onClick = {
                        text = suggestion
                        expanded = false
                    }
                )
            }
        }
    }

    /*    Column(modifier = modifier) {
            OutlinedTextField(
                value = text,
                onValueChange = { newText ->
                    text = newText
                    filteredSuggestions = if (newText.isNotBlank()) {
                        allSuggestions.filter {
                            it.contains(newText, ignoreCase = true)
                        }
                    } else {
                        allSuggestions
                    }
                    expanded = filteredSuggestions.isNotEmpty()
                },
                label = { Text(label) },
                modifier = Modifier
                    .fillMaxWidth()
                    *//*.onFocusChanged { focusState ->
                    if (focusState.isFocused && text.isNotBlank() && filteredSuggestions.isNotEmpty()) {
                        expanded = true
                    } else if (!focusState.isFocused) {
                        // Delay hiding to allow click on dropdown item
                        // A more robust solution might involve interactionSource
                        // or a small delay before setting expanded to false
                        // expanded = false
                    }
                }*//*,
            singleLine = true
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = {*//* expanded = false*//* },
            modifier = Modifier.fillMaxWidth(),
            // Use PopupProperties to control the dropdown's behavior if needed
            // properties = PopupProperties(focusable = false) // Prevents stealing focus
        ) {
            filteredSuggestions.forEach { suggestion ->
                DropdownMenuItem(
                    text = { Text(suggestion) },
                    onClick = {
                        text = suggestion
                        expanded = false
//                        focusManager.clearFocus() // Clear focus after selection
                    }
                )
            }
        }
    }*/
}

@Preview(showBackground = true)
@Composable
fun AutoCompleteTextViewPreview() {
    ExpenseMonitorTheme {
        val sampleSuggestions = listOf(
            "Apple", "Banana", "Cherry", "Date", "Elderberry",
            "Fig", "Grape", "Honeydew"
        )
        AutoCompleteTextView(
            allSuggestions = sampleSuggestions,
            label = "Search for a fruit"
        )
    }
}


@Preview
@Composable
fun DropDownPreview() {
    ExpenseMonitorTheme {
        MinimalDropdownMenu({})
    }
}


// REMOVE the old global readSms function from this file:
// fun readSms(contentResolver: ContentResolver, allowedAddresses: List<String>): List<SmsMessage> { ... }
// This function is now effectively private and suspendable inside SmsViewModel.