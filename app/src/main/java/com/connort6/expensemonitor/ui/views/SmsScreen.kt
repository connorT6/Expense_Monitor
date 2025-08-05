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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.connort6.expensemonitor.repo.SmsMessage
import com.connort6.expensemonitor.ui.theme.ExpenseMonitorTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


@Composable
fun SmsReaderScreen(
    smsViewModel: ISmsViewModel,
    navController: NavController
) {
    val context = LocalContext.current

    smsViewModel.selectSmsMessage(null)

    // States from ViewModel
    val smsMessages by smsViewModel.smsMessages.collectAsStateWithLifecycle()
    val isLoading by smsViewModel.isLoading.collectAsStateWithLifecycle()
    val error by smsViewModel.error.collectAsStateWithLifecycle()
    val smsSenders by smsViewModel.smsSenders.collectAsState()
    val openType by smsViewModel.openType.collectAsState()

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
            smsViewModel.loadSmsMessages(
                SMSLoadMethod.BOUNDED_ONLY,
                smsSenders.map { it.address }
            )
        }
    }

    var showSMSList by remember { mutableStateOf(true) }

    Column(
    ) {

        if (!permissionGranted) {
            Text("READ_SMS permission is required to display messages.")
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { permissionLauncher.launch(Manifest.permission.READ_SMS) }) {
                Text("Request Permission")
            }
        } else {
            // Content when permission is granted
            if (error != null) {
                Text("Error: $error", color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { smsViewModel.loadSmsMessages(SMSLoadMethod.BOUNDED_ONLY) }) {
                    Text("Retry")
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Absolute.Right,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DropdownTextField(smsSenders, {
                        smsViewModel.loadSmsMessages(
                            SMSLoadMethod.ALL,
                            listOf(it.address)
                        )
                    }, { it.address }, Modifier.weight(1f))

                    Spacer(modifier = Modifier.height(8.dp))

                    MinimalDropdownMenu({ showSMSList = !showSMSList })
                }
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (smsMessages.isEmpty()) {
                    Text("No matching SMS messages found.")
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) { // Fill available space
                        if (showSMSList) {
                            items(
                                smsMessages,
                                key = { it.id }) { sms -> // Add a key for better performance
                                SmsItemView(sms) {
                                    smsViewModel.selectSmsMessage(it)
                                    if (openType == OpenType.SELECTION) {
                                        navController.popBackStack()
                                    }
                                }
                                HorizontalDivider()
                            }
                        } else {
                            items(smsSenders) { sender ->
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
        SmsReaderScreen(MockSmsViewModel(), rememberNavController())
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