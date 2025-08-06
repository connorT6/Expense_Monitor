package com.connort6.expensemonitor.repo

import com.connort6.expensemonitor.mainCollection
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.ServerTimestamp

data class ProcessedSmsDetails(
    val smsIds: List<String> = mutableListOf(),
    @ServerTimestamp val lastUpdated: Timestamp? = null
)

data class SmsMessage(
    @DocumentId
    override var id: String,
    val address: String, // Sender's phone number
    val body: String,
    val date: Long,
    val type: Int, // e.g., Telephony.Sms.MESSAGE_TYPE_INBOX
    var docId: String = "",
    @ServerTimestamp override var lastUpdated: Timestamp? = null,
    @get:Exclude
    var processed: Boolean = false,
    override val deleted: Boolean = false

) : BaseEntity

class SmsRepo private constructor() :
    MainRepository<SmsMessage>(SmsMessage::class.java, { it.lastUpdated }) {

    private val messageDocRef = mainCollection.document("messages")
    override var collection = messageDocRef.collection("sms")


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

    companion object {
        @Volatile
        private var instance: SmsRepo? = null
        fun getInstance(): SmsRepo = instance ?: synchronized(this) {
            instance ?: SmsRepo().also { instance = it }
        }
    }

    private fun listenDocChange() {
        messageDocRef.addSnapshotListener { snapShot, _ ->
            if (snapShot == null || !snapShot.exists()) {
                return@addSnapshotListener
            }

        }
    }

    suspend fun findByAddressAndDate(address: String, date: Long): SmsMessage? {
        return findByQuery {
            it.whereEqualTo(SmsMessage::address.name, address)
                .whereEqualTo(SmsMessage::date.name, date)
        }
    }

}

