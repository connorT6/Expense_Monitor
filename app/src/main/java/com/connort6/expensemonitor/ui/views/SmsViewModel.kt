package com.connort6.expensemonitor.ui.views


import android.app.Application
import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri
import android.provider.Telephony
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.connort6.expensemonitor.repo.SmsMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Your existing SmsMessage data class (can stay in SmsMessage.kt or move here if preferred)
// data class SmsMessage(
// val id: String,
// val address: String,
// val body: String,
// val date: Long,
// val type: Int
// )

class SmsViewModel(application: Application) : AndroidViewModel(application) {

    private val _smsMessages = MutableStateFlow<List<SmsMessage>>(emptyList())
    val smsMessages: StateFlow<List<SmsMessage>> = _smsMessages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val contentResolver: ContentResolver = application.contentResolver

    fun loadSmsMessages(allowedAddresses: List<String>) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val messages = readSmsFromProvider(allowedAddresses)
                _smsMessages.value = messages
            } catch (e: Exception) {
                e.printStackTrace()
                _error.value = "Failed to load SMS messages: ${e.message}"
                _smsMessages.value = emptyList() // Clear messages on error
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Moved the readSms logic into the ViewModel
    private suspend fun readSmsFromProvider(allowedAddresses: List<String>): List<SmsMessage> {
        // Use withContext to move the blocking call to an I/O dispatcher
        return withContext(Dispatchers.IO) {
            val smsList = mutableListOf<SmsMessage>()
            val uri: Uri = Telephony.Sms.Inbox.CONTENT_URI

            val projection = arrayOf(
                Telephony.Sms._ID,
                Telephony.Sms.ADDRESS,
                Telephony.Sms.BODY,
                Telephony.Sms.DATE,
                Telephony.Sms.TYPE
            )

            val selectionClauses = mutableListOf<String>()
            val selectionArgsList = mutableListOf<String>()

            selectionClauses.add("${Telephony.Sms.TYPE} = ?")
            selectionArgsList.add(Telephony.Sms.MESSAGE_TYPE_INBOX.toString())

            if (allowedAddresses.isNotEmpty()) {
                val addressPlaceholders = List(allowedAddresses.size) { "?" }.joinToString(", ")
                selectionClauses.add("${Telephony.Sms.ADDRESS} IN ($addressPlaceholders)")
                selectionArgsList.addAll(allowedAddresses)
            }

            val selection = selectionClauses.joinToString(" AND ").ifEmpty { null }
            val selectionArgs = selectionArgsList.toTypedArray().ifEmpty { null }
            val sortOrder = "${Telephony.Sms.DATE} DESC"

            var cursor: Cursor? = null
            try {
                cursor = contentResolver.query(
                    uri,
                    projection,
                    selection,
                    selectionArgs,
                    sortOrder
                )

                cursor?.use {
                    val idColumn = it.getColumnIndexOrThrow(Telephony.Sms._ID)
                    val addressColumn = it.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
                    val bodyColumn = it.getColumnIndexOrThrow(Telephony.Sms.BODY)
                    val dateColumn = it.getColumnIndexOrThrow(Telephony.Sms.DATE)
                    val typeColumn =
                        it.getColumnIndexOrThrow(Telephony.Sms.TYPE) // Kept for completeness

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
                e.printStackTrace()
            } finally {
                cursor?.close()
            }
            // Catching specific exceptions can be more robust here
            // For now, a general Exception is fine for demonstration
            // The try-catch in loadSmsMessages will handle the UI update for errors

            // No finally to close cursor here, as cursor?.use handles it.
            // The try-catch in the calling function (loadSmsMessages) handles errors for UI.
            smsList // Return the list
        }
    }

    fun clearError() {
        _error.value = null
    }
}