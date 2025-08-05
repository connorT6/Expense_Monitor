package com.connort6.expensemonitor.ui.views


import android.app.Application
import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract
import android.provider.Telephony
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.connort6.expensemonitor.repo.SMSOperator
import com.connort6.expensemonitor.repo.SMSOperatorRepo
import com.connort6.expensemonitor.repo.SMSParserRepo
import com.connort6.expensemonitor.repo.SmsMessage
import com.connort6.expensemonitor.repo.SmsRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
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

enum class OpenType {
    GENERAL,
    SELECTION
}

// ISmsViewModel.kt
interface ISmsViewModel {
    val smsMessages: StateFlow<List<SmsMessage>>
    val smsSenders: StateFlow<List<SMSOperator>>
    val isLoading: StateFlow<Boolean>
    val error: StateFlow<String?>
    val openType: StateFlow<OpenType>
    val selectedSmsMessage: StateFlow<SmsMessage?>

    fun loadSmsMessages(smsLoadMethod: SMSLoadMethod, allowedSenders: List<String> = emptyList())
    fun clearError()
    fun saveSmsMessage(smsMessage: SmsMessage)
    fun filterSmsByAccountId(accountId: String)
    fun setOpenType(openType: OpenType)
    fun selectSmsMessage(smsMessage: SmsMessage)
}


class SmsViewModel(application: Application) : AndroidViewModel(application), ISmsViewModel {

    private val _smsMessages = MutableStateFlow<List<SmsMessage>>(emptyList())
    override val smsMessages: StateFlow<List<SmsMessage>> = _smsMessages.asStateFlow()

    private val _smsSenders = MutableStateFlow<List<SMSOperator>>(emptyList())
    override val smsSenders: StateFlow<List<SMSOperator>> = _smsSenders.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    override val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    override val error: StateFlow<String?> = _error.asStateFlow()

    private val _openType = MutableStateFlow(OpenType.GENERAL)
    override val openType = _openType.asStateFlow()

    private val _selectedSmsMessage = MutableStateFlow<SmsMessage?>(null)
    override val selectedSmsMessage = _selectedSmsMessage.asStateFlow()

    private val contentResolver: ContentResolver = application.contentResolver
    private val smsRepo = SmsRepo.getInstance()
    private val smsOperatorRepo = SMSOperatorRepo.getInstance()
    private val smsParserRepo = SMSParserRepo.getInstance()


    init {
//        loadAllSmsSenders()
        viewModelScope.launch {
            smsOperatorRepo.operators.collect({
                _smsSenders.value = it
            })
        }
    }

    override fun loadSmsMessages(smsLoadMethod: SMSLoadMethod, allowedSenders: List<String>) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                var addresses: List<String> = allowedSenders
                if (smsLoadMethod == SMSLoadMethod.ALL) {
                    addresses = smsOperatorRepo.operators.value.map { it.address }
                }
                val messageTask = async { readSmsFromProvider(addresses) }
                val processedSmsDetails = async { smsRepo.smsDetails.value }
                val result = Pair(messageTask.await(), processedSmsDetails.await())
                val messages = result.first.map { smsMessage ->
                    result.second.smsIds.find { it == smsMessage.id }?.let {
                        smsMessage.processed = true
                        smsMessage
                    } ?: smsMessage
                }
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
                Log.e(this@SmsViewModel::class.java.simpleName, "Error loading SMS messages", e)
            } finally {
                cursor?.close()
            }
            smsList
        }
    }

    override fun clearError() {
        _error.value = null
    }

    override fun saveSmsMessage(smsMessage: SmsMessage) {
//        viewModelScope.launch {
//            smsRepo.saveSms(smsMessage)
//        }
    }

    private fun loadAllSmsSenders() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val senders = getSmsSenders()
                _smsSenders.value = senders.map { SMSOperator(it) }
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

    override fun filterSmsByAccountId(accountId: String) {
        viewModelScope.launch {
            val smsParsers = smsParserRepo.findAllByAccountId(accountId)
            val operatorIds = smsParsers.map { it.smsOperatorId }.toSet()
            val filteredOperators = smsOperatorRepo.operators.value.filter { it.id in operatorIds }
            _smsSenders.value = filteredOperators
            loadSmsMessages(SMSLoadMethod.BOUNDED_ONLY, filteredOperators.map { it.address })
        }
    }

    override fun setOpenType(openType: OpenType) {
        _openType.value = openType
    }

    override fun selectSmsMessage(smsMessage: SmsMessage) {
        _selectedSmsMessage.value = smsMessage
    }
}


class MockSmsViewModel : ISmsViewModel {

    private val _smsMessages = MutableStateFlow<List<SmsMessage>>(emptyList())
    override val smsMessages: StateFlow<List<SmsMessage>> = _smsMessages.asStateFlow()
    // Or for non-interface version:
    // val smsMessages: StateFlow<List<SmsMessage>> get() = _smsMessages.asStateFlow()


    private val _smsSenders = MutableStateFlow<List<SMSOperator>>(emptyList())
    override val smsSenders: StateFlow<List<SMSOperator>> = _smsSenders.asStateFlow()
    // Or for non-interface version:
    // val smsSenders: StateFlow<List<SMSOperator>> get() = _smsSenders.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    override val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    // Or for non-interface version:
    // val isLoading: StateFlow<Boolean> get() = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    override val error: StateFlow<String?> = _error.asStateFlow()
    // Or for non-interface version:
    // val error: StateFlow<String?> get() = _error.asStateFlow()

    private val _openType = MutableStateFlow(OpenType.GENERAL)
    override val openType = _openType.asStateFlow()

    override val selectedSmsMessage: StateFlow<SmsMessage?>
        get() = TODO("Not yet implemented")


    init {
        // Populate with sample data
        _smsMessages.value = listOf(
            SmsMessage(
                "1",
                "Savings Bank",
                "Your account balance is $1,250.50",
                System.currentTimeMillis() - 100000,
                1
            ),
            SmsMessage(
                "2",
                "Credit Card Co.",
                "Alert: A transaction of $75.20 was made.",
                System.currentTimeMillis() - 200000,
                1
            ),
            SmsMessage(
                "3",
                "Mobile Carrier",
                "Your bill for $45.00 is due on 07/15.",
                System.currentTimeMillis() - 300000,
                1
            ),
            SmsMessage(
                "4",
                "Utility Services",
                "Reminder: Payment for electricity is $88.90.",
                System.currentTimeMillis() - 400000,
                1
            )
        )

        _smsSenders.value = listOf(
            SMSOperator("Savings Bank"),
            SMSOperator("Credit Card Co."),
            SMSOperator("Mobile Carrier"),
            SMSOperator("Utility Services"),
            SMSOperator("Known Contact")
        )

        _isLoading.value = false
        _error.value = null // Or set a sample error: "Sample error message"
    }

    // Mock implementations of the methods
    override fun loadSmsMessages(smsLoadMethod: SMSLoadMethod, allowedSenders: List<String>) {
//        _isLoading.value = true
        // Simulate a delay
        // In a real test, you might use TestCoroutineDispatcher here
        GlobalScope.launch { // Use an appropriate scope for tests/previews
            delay(500) // Simulate network delay
            if (smsLoadMethod == SMSLoadMethod.ALL) {
                _smsMessages.value = listOf(
                    SmsMessage(
                        "1",
                        "Savings Bank",
                        "All: Your account balance is $1,250.50",
                        System.currentTimeMillis() - 100000,
                        1
                    ),
                    SmsMessage(
                        "2",
                        "Credit Card Co.",
                        "All: Alert: A transaction of $75.20 was made.",
                        System.currentTimeMillis() - 200000,
                        1
                    ),
                    SmsMessage(
                        "3",
                        "Mobile Carrier",
                        "All: Your bill for $45.00 is due on 07/15.",
                        System.currentTimeMillis() - 300000,
                        1
                    )
                )
            } else { // BOUNDED_ONLY
                _smsMessages.value = if (allowedSenders.isEmpty()) {
                    // Simulating BOUNDED_ONLY with current mock senders
                    listOf(
                        SmsMessage(
                            "1",
                            "Savings Bank",
                            "Bounded: Your account balance is $1,250.50",
                            System.currentTimeMillis() - 100000,
                            1
                        ),
                        SmsMessage(
                            "2",
                            "Credit Card Co.",
                            "Bounded: Alert: A transaction of $75.20 was made.",
                            System.currentTimeMillis() - 200000,
                            1
                        )
                    )
                } else {
                    // Filter based on allowedSenders for more specific mock behavior
                    _smsMessages.value.filter { msg -> allowedSenders.contains(msg.address) }
                }
            }
            _isLoading.value = false
        }
    }

    override fun clearError() {
        _error.value = null
    }

    override fun saveSmsMessage(smsMessage: SmsMessage) {
        // You could add the message to the list for testing, or just log
        println("MockSmsViewModel: saveSmsMessage called with: $smsMessage")
        val currentMessages = _smsMessages.value.toMutableList()
        currentMessages.add(0, smsMessage) // Add to top for visibility
        _smsMessages.value = currentMessages
    }

    override fun filterSmsByAccountId(accountId: String) {
        TODO("Not yet implemented")
    }

    override fun setOpenType(openType: OpenType) {
        _openType.value = openType
    }

    override fun selectSmsMessage(smsMessage: SmsMessage) {
        TODO("Not yet implemented")
    }
}