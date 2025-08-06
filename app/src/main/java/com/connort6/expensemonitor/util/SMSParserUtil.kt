package com.connort6.expensemonitor.util

import android.util.Log
import com.connort6.expensemonitor.repo.SMSParseKeys
import com.connort6.expensemonitor.repo.SMSParser
import com.connort6.expensemonitor.repo.SmsMessage
import java.math.BigDecimal
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.incrementAndFetch


data class ParsedData(val amount: BigDecimal)

class SMSParserUtil {

    companion object {
        @OptIn(ExperimentalAtomicApi::class)
        fun parseMessage(smsMessage: SmsMessage, smsParser: SMSParser): ParsedData? {
            var pattern = smsParser.pattern

            val captureGroupIndex = AtomicInt(0)

            val captureGroupToKey = mutableMapOf<Int, SMSParseKeys>()

            //TODO to check whether there are more capture groups in pattern
            SMSParseKeys.entries.forEach { key ->
                if (pattern.contains(key.name)) { // if the key is available inside the pattern
                    pattern = pattern.replace(
                        key.name,
                        "(${key.matchingRegex})"
                    ) // adding as a capture group
                    captureGroupToKey[captureGroupIndex.incrementAndFetch()] = key
                }
            }

            if (captureGroupToKey.isEmpty()) {
                return null
            }

            val regex = Regex(pattern)
            val matches = regex.find(smsMessage.body)
            if (matches == null) {
                return null
            }

            val matchesForKey = mutableMapOf<SMSParseKeys, String>()

            captureGroupToKey.forEach { (index, key) ->
                matchesForKey[key] = matches.groupValues[index]
            }

            try {
                val amount = BigDecimal(matchesForKey[SMSParseKeys.AMOUNT])
                return ParsedData(amount)
            } catch (e: Exception) {
                Log.e("SMSParserUtil", "Error parsing message", e)
                return null
            }
        }
    }
}