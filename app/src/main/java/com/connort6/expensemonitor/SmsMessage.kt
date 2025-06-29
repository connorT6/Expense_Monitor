package com.connort6.expensemonitor

import android.Manifest
import android.content.ContentResolver
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.provider.Telephony
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.* // Or material3
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class SmsMessage(
    val id: String,
    val address: String, // Sender's phone number
    val body: String,
    val date: Long,
    val type: Int // e.g., Telephony.Sms.MESSAGE_TYPE_INBOX
)

@Composable
fun SmsReaderScreen() {
    val context = LocalContext.current
    var smsMessages by remember { mutableStateOf<List<SmsMessage>>(emptyList()) }
    var permissionGranted by remember {
        mutableStateOf(
            context.checkSelfPermission(Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        permissionGranted = isGranted
        if (isGranted) {
            // Permission Granted: Load SMS messages
            smsMessages = readSms(context.contentResolver)
        } else {
            // Permission Denied: Handle the denial (e.g., show a message to the user)
            // You might want to explain why the permission is needed.
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (permissionGranted) {
            Text("SMS Permission Granted. Loading messages...")
            LaunchedEffect(Unit) { // Re-load if permission was granted initially
                smsMessages = readSms(context.contentResolver)
            }
            if (smsMessages.isEmpty()) {
                Text("No SMS messages found or unable to load.")
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(smsMessages) { sms ->
                        SmsItemView(sms)
                        Divider()
                    }
                }
            }
        } else {
            Text("READ_SMS permission is required to display messages.")
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = {
                permissionLauncher.launch(Manifest.permission.READ_SMS)
            }) {
                Text("Request Permission")
            }
        }
    }
}

@Composable
fun SmsItemView(sms: SmsMessage) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            "From: ${sms.address}",
            style = MaterialTheme.typography.titleMedium // Changed from subtitle1
        )
        Text(
            "Date: ${SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date(sms.date))}",
            style = MaterialTheme.typography.bodySmall // Or bodyMedium, depending on desired emphasis
        )
        Text(
            sms.body,
            style = MaterialTheme.typography.bodyMedium // Changed from body2
        )
    }
}

fun readSms(contentResolver: ContentResolver): List<SmsMessage> {
    val smsList = mutableListOf<SmsMessage>()
    val uri: Uri = Telephony.Sms.CONTENT_URI // You can also use Telephony.Sms.Inbox.CONTENT_URI for only inbox
    // Other URIs: Telephony.Sms.Sent.CONTENT_URI, Telephony.Sms.Draft.CONTENT_URI

    // Columns to retrieve
    val projection = arrayOf(
        Telephony.Sms._ID,
        Telephony.Sms.ADDRESS,
        Telephony.Sms.BODY,
        Telephony.Sms.DATE,
        Telephony.Sms.TYPE
    )

    // You can add a selection and selectionArgs to filter messages
    // For example, to get only inbox messages:
    // val selection = "${Telephony.Sms.TYPE} = ?"
    // val selectionArgs = arrayOf(Telephony.Sms.MESSAGE_TYPE_INBOX.toString())
    // Or to get messages from a specific sender:
    // val selection = "${Telephony.Sms.ADDRESS} = ?"
    // val selectionArgs = arrayOf("1234567890") // replace with actual number

    val sortOrder = "${Telephony.Sms.DATE} DESC" // Latest messages first

    var cursor: Cursor? = null
    try {
        cursor = contentResolver.query(
            uri,
            projection,
            null, // No selection (get all) or provide your selection
            null, // No selectionArgs or provide your selectionArgs
            sortOrder
        )

        cursor?.use { // Automatically closes the cursor
            val idColumn = it.getColumnIndexOrThrow(Telephony.Sms._ID)
            val addressColumn = it.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            val bodyColumn = it.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val dateColumn = it.getColumnIndexOrThrow(Telephony.Sms.DATE)
            val typeColumn = it.getColumnIndexOrThrow(Telephony.Sms.TYPE)

            while (it.moveToNext()) {
                val id = it.getString(idColumn)
                val address = it.getString(addressColumn)
                val body = it.getString(bodyColumn)
                val date = it.getLong(dateColumn)
                val type = it.getInt(typeColumn)
                smsList.add(SmsMessage(id, address ?: "Unknown", body ?: "", date, type))
            }
        }
    } catch (e: Exception) {
        // Handle exceptions, e.g., SecurityException if permission is denied
        e.printStackTrace()
    } finally {
        cursor?.close() // Ensure cursor is closed even if 'use' is not used or an exception occurs before it
    }
    return smsList
}