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
    override var id: String = "",
    val smsId: String = "",
    val address: String = "", // Sender's phone number
    val body: String = "",
    val date: Long = System.currentTimeMillis(),
    val type: Int = 1, // e.g., Telephony.Sms.MESSAGE_TYPE_INBOX
    @ServerTimestamp override var lastUpdated: Timestamp? = null,
    @get:Exclude
    var processed: Boolean = false,
    override val deleted: Boolean = false
) : BaseEntity {
    constructor(
        smsId: String,
        address: String,
        body: String,
        date: Long,
        type: Int
    ) : this("", smsId, address, body, date, type)
}

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

