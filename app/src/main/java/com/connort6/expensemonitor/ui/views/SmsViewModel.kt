package com.connort6.expensemonitor.ui.views


import android.app.Application
import android.content.ContentResolver
import android.database.Cursor
import android.provider.ContactsContract
import android.net.Uri
import android.provider.Telephony
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.connort6.expensemonitor.repo.AccountRepo
import com.connort6.expensemonitor.repo.SmsMessage
import com.connort6.expensemonitor.repo.SmsRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Your existing SmsMessage data class (can stay in SmsScreen.kt or move here if preferred)
// data class SmsMessage(
// val id: String,
// val address: String,
// val body: String,
// val date: Long,
// val type: Int
// )

enum class SMSLoadMethod {
    ALL,
    BOUNDED_ONLY
}

class SmsViewModel(application: Application) : AndroidViewModel(application) {

    private val _smsMessages = MutableStateFlow<List<SmsMessage>>(emptyList())
    val smsMessages: StateFlow<List<SmsMessage>> = _smsMessages.asStateFlow()

    private val _smsSenders = MutableStateFlow<List<String>>(emptyList())
    val smsSenders: StateFlow<List<String>> = _smsSenders.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val contentResolver: ContentResolver = application.contentResolver

    private val smsRepo = SmsRepo.getInstance()

    private val accountRepo = AccountRepo.getInstance()

    init {
        loadSmsSenders()
    }

    fun loadSmsMessages(smsLoadMethod: SMSLoadMethod) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                var addresses: List<String> = listOf()
                if (smsLoadMethod == SMSLoadMethod.BOUNDED_ONLY) {
                    addresses = accountRepo.allSmsSendersFlow.value.map { it.address }
                }
                val messages = readSmsFromProvider(addresses)
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

    fun saveSmsMessage(smsMessage: SmsMessage) {
        viewModelScope.launch {
            smsRepo.saveSms(smsMessage)
        }
    }

    fun loadSmsSenders() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val senders = getSmsSenders()
                _smsSenders.value = senders
            } catch (e: Exception) {
                e.printStackTrace()
                _error.value = "Failed to load SMS senders: ${e.message}"
                _smsSenders.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun getSmsSenders(): List<String> {
        return withContext(Dispatchers.IO) {
            val senderList = mutableSetOf<String>() // Use a Set to avoid duplicates
            val uri: Uri = Telephony.Sms.Inbox.CONTENT_URI
            val projection = arrayOf(Telephony.Sms.ADDRESS)
            val selection = "${Telephony.Sms.TYPE} = ?"
            val selectionArgs = arrayOf(Telephony.Sms.MESSAGE_TYPE_INBOX.toString())
            val sortOrder =
                "${Telephony.Sms.ADDRESS} ASC" // Sort for potentially easier processing later

            var cursor: Cursor? = null
            try {
                cursor = contentResolver.query(uri, projection, selection, selectionArgs, sortOrder)
                cursor?.use {
                    val addressColumn = it.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
                    while (it.moveToNext()) {
                        val address = it.getString(addressColumn)
                        if (!address.isNullOrBlank()) {
                            // Optionally, try to get contact name here if performance allows
                            // For now, just adding the address.
                            // You might want a separate function to resolve contact names
                            // to avoid slowing down this initial sender retrieval.
                            senderList.add(getContactName(address) ?: address)
                        }
                    }
                }
            } finally {
                cursor?.close()
            }
            senderList.toList()
        }
    }

    private fun getContactName(phoneNumber: String): String? {
        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(phoneNumber)
        )
        val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)
        var contactName: String? = null
        val cursor = contentResolver.query(uri, projection, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                contactName =
                    it.getString(it.getColumnIndexOrThrow(ContactsContract.PhoneLookup.DISPLAY_NAME))
            }
        }
        return contactName
    }
}