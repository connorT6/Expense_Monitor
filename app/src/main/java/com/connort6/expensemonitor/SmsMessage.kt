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
import androidx.compose.material3.HorizontalDivider
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
            smsMessages = readSms(context.contentResolver, listOf("5555"))
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
            LaunchedEffect(Unit) { // Re-load if permission was granted initially
                smsMessages = readSms(context.contentResolver, listOf("5555"))
            }
            if (smsMessages.isEmpty()) {
                Text("No SMS messages found or unable to load.")
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(smsMessages) { sms ->
                        SmsItemView(sms)
                        HorizontalDivider()
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

fun readSms(contentResolver: ContentResolver, allowedAddresses: List<String>): List<SmsMessage> {
    val smsList = mutableListOf<SmsMessage>()
    val uri: Uri = Telephony.Sms.Inbox.CONTENT_URI // Directly use Inbox URI

    val projection = arrayOf(
        Telephony.Sms._ID,
        Telephony.Sms.ADDRESS,
        Telephony.Sms.BODY,
        Telephony.Sms.DATE,
        Telephony.Sms.TYPE
    )

    // Build the selection clause
    // We want messages that are of type INBOX AND whose address is in the allowedAddresses list.
    val selectionClauses = mutableListOf<String>()
    val selectionArgsList = mutableListOf<String>()

    // Filter by type: INBOX
    // Note: Since we are using Telephony.Sms.Inbox.CONTENT_URI,
    // the type filter might be redundant but it's good for clarity or if you switch URIs.
    selectionClauses.add("${Telephony.Sms.TYPE} = ?")
    selectionArgsList.add(Telephony.Sms.MESSAGE_TYPE_INBOX.toString())

    // Filter by allowed addresses
    if (allowedAddresses.isNotEmpty()) {
        val addressPlaceholders = List(allowedAddresses.size) { "?" }.joinToString(", ")
        selectionClauses.add("${Telephony.Sms.ADDRESS} IN ($addressPlaceholders)")
        selectionArgsList.addAll(allowedAddresses)
    }

    val selection = selectionClauses.joinToString(" AND ")
    val selectionArgs = selectionArgsList.toTypedArray()

    val sortOrder = "${Telephony.Sms.DATE} DESC" // Latest messages first

    var cursor: Cursor? = null
    try {
        cursor = contentResolver.query(
            uri,
            projection,
            if (selectionClauses.isEmpty()) null else selection, // Pass null if no selection criteria
            if (selectionArgsList.isEmpty()) null else selectionArgs, // Pass null if no args
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

                // Additional client-side check if needed, though SQL filter should handle it
                // if (type == Telephony.Sms.MESSAGE_TYPE_INBOX && (allowedAddresses.isEmpty() || allowedAddresses.contains(address))) {
                smsList.add(SmsMessage(id, address ?: "Unknown", body ?: "", date, type))
                // }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    } finally {
        cursor?.close()
    }
    return smsList
}
