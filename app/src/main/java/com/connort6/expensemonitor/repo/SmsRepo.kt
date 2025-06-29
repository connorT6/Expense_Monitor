package com.connort6.expensemonitor.repo

import com.connort6.expensemonitor.mainCollection
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ServerTimestamp
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.tasks.await

data class ProcessedSmsDetails(
    val smsIds: List<String> = mutableListOf(),
    @ServerTimestamp val lastUpdated: Timestamp? = null
)

data class SmsMessage(
    val id: String, // this is sms id
    val address: String, // Sender's phone number
    val body: String,
    val date: Long,
    val type: Int, // e.g., Telephony.Sms.MESSAGE_TYPE_INBOX
    var docId: String = "",
    @ServerTimestamp val lastUpdated: Timestamp? = null,
)

class SmsRepo private constructor() {

    private val messageDocRef = mainCollection.document("messages")
    private val collection = messageDocRef.collection("sms")


    private val _messages = MutableStateFlow<List<SmsMessage>>(emptyList())
    val messages = _messages.asStateFlow()

    private val _smsDetails = MutableStateFlow(ProcessedSmsDetails())
    val smsDetails = _smsDetails.asStateFlow()

    companion object {
        @Volatile
        private var instance: SmsRepo? = null
        fun getInstance(): SmsRepo = instance ?: synchronized(this) {
            instance ?: SmsRepo().also { instance = it }
        }
    }

    init {
        messageDocRef.get().addOnSuccessListener {
            if (!it.exists()) {
                messageDocRef.set(ProcessedSmsDetails()).addOnSuccessListener {
                    listenDocChange()
                }
            } else {
                listenDocChange()
            }
        }
    }

    private fun listenDocChange() {
        messageDocRef.addSnapshotListener { snapShot, _ ->
            if (snapShot == null || !snapShot.exists()) {
                return@addSnapshotListener
            }
            val messageDoc =
                snapShot.toObject(ProcessedSmsDetails::class.java) ?: return@addSnapshotListener
            _smsDetails.update {
                messageDoc
            }
        }
    }

    suspend fun saveSms(smsMessage: SmsMessage) {
        val existing = _smsDetails.value.smsIds.contains(smsMessage.id)
        if (existing) {
            return
        }

        val db = FirebaseFirestore.getInstance()
        return db.runTransaction({ transaction ->

            val docRef = collection.document()
            var messageDoc =
                transaction.get(messageDocRef).toObject(ProcessedSmsDetails::class.java)
            if (messageDoc == null) {
                return@runTransaction
            }
            messageDoc = messageDoc.copy(
                smsIds = (messageDoc.smsIds.toSet() + smsMessage.id).toList(),
                lastUpdated = null
            )

            transaction.set(docRef, smsMessage.copy(docId = docRef.id))
            transaction.set(
                messageDocRef, messageDoc, SetOptions.merge()
            )
            docRef.id
        }).await()
    }

}

