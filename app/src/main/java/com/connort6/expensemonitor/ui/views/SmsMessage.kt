package com.connort6.expensemonitor.ui.views // Or your specific package

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.connort6.expensemonitor.repo.SmsMessage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


@Composable
fun SmsReaderScreen(
    smsViewModel: SmsViewModel = viewModel() // Obtain ViewModel instance
) {
    val context = LocalContext.current
    // States from ViewModel
    val smsMessages by smsViewModel.smsMessages.collectAsStateWithLifecycle()
    val isLoading by smsViewModel.isLoading.collectAsStateWithLifecycle()
    val error by smsViewModel.error.collectAsStateWithLifecycle()

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
            smsViewModel.loadSmsMessages(allowedSenders)
        } else {
            // Optionally, show a persistent message or disable features
        }
    }

    // Effect to load messages if permission is already granted when screen enters
    LaunchedEffect(key1 = permissionGranted, key2 = allowedSenders) {
        if (permissionGranted) {
            smsViewModel.loadSmsMessages(allowedSenders)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (!permissionGranted) {
            Text("READ_SMS permission is required to display messages.")
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = {
                permissionLauncher.launch(Manifest.permission.READ_SMS)
            }) {
                Text("Request Permission")
            }
        } else {
            // Content when permission is granted
            if (isLoading) {
                CircularProgressIndicator()
            } else if (error != null) {
                Text("Error: $error", color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = {
                    smsViewModel.clearError() // Clear the error
                    smsViewModel.loadSmsMessages(allowedSenders) // Retry loading
                }) {
                    Text("Retry")
                }
            } else if (smsMessages.isEmpty()) {
                Text("No matching SMS messages found.")
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) { // Fill available space
                    items(smsMessages, key = { it.id }) { sms -> // Add a key for better performance
                        SmsItemView(sms) {
                            smsViewModel.saveSmsMessage(it)
                        }
                        HorizontalDivider()
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
            .padding(vertical = 8.dp)
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

// REMOVE the old global readSms function from this file:
// fun readSms(contentResolver: ContentResolver, allowedAddresses: List<String>): List<SmsMessage> { ... }
// This function is now effectively private and suspendable inside SmsViewModel.