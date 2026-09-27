package com.example.executor

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import android.util.Log
import androidx.core.content.ContextCompat

class LunaContactsManager(private val context: Context) {

    companion object {
        private const val TAG = "LunaContactsManager"
    }

    data class ContactMatch(
        val name: String,
        val phoneNumber: String
    )

    data class CallResult(
        val success: Boolean,
        val message: String,
        val matchedContacts: List<ContactMatch> = emptyList()
    )

    /**
     * Resolves contact or number and performs the call.
     */
    fun callContactOrNumber(query: String): CallResult {
        val cleanQuery = query.trim()

        // Check if query is directly a phone number (e.g. "08012345678", "+1234567890")
        val isNumeric = cleanQuery.replace(Regex("[+\\-\\s()]"), "").all { it.isDigit() } && cleanQuery.length >= 3
        if (isNumeric) {
            return initiateCall(cleanQuery, "Calling $cleanQuery.")
        }

        // Check contacts permission
        val hasContactsPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasContactsPermission) {
            return CallResult(
                success = false,
                message = "Contacts permission is required to find \"$cleanQuery\". Please grant contacts permission in Settings."
            )
        }

        val matches = searchContacts(cleanQuery)

        return when {
            matches.isEmpty() -> {
                CallResult(
                    success = false,
                    message = "I couldn't find \"$cleanQuery\" in your contacts."
                )
            }
            matches.size == 1 -> {
                val match = matches[0]
                initiateCall(match.phoneNumber, "Calling ${match.name}.")
            }
            else -> {
                // Multiple matches found - check if names are identical (different numbers) or different names
                val distinctNames = matches.map { it.name }.distinct()
                if (distinctNames.size == 1) {
                    // Same person, pick first number
                    val match = matches[0]
                    initiateCall(match.phoneNumber, "Calling ${match.name}.")
                } else {
                    val nameList = distinctNames.take(3).joinToString(" or ")
                    CallResult(
                        success = false,
                        message = "I found multiple contacts matching $cleanQuery: $nameList. Which one would you like to call?",
                        matchedContacts = matches
                    )
                }
            }
        }
    }

    fun findPhoneNumberForName(name: String): String? {
        val matches = searchContacts(name)
        return matches.firstOrNull()?.phoneNumber
    }

    private fun searchContacts(query: String): List<ContactMatch> {
        val results = mutableListOf<ContactMatch>()
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )
        val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("%$query%")

        try {
            val cursor = context.contentResolver.query(
                uri,
                projection,
                selection,
                selectionArgs,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
            )

            cursor?.use {
                val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                while (it.moveToNext()) {
                    val name = it.getString(nameIndex) ?: ""
                    val number = it.getString(numberIndex) ?: ""
                    if (number.isNotEmpty()) {
                        results.add(ContactMatch(name, number))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying contacts for: $query", e)
        }
        return results
    }

    private fun initiateCall(phoneNumber: String, successMessage: String): CallResult {
        val hasCallPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED

        val intent = if (hasCallPermission) {
            Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:${Uri.encode(phoneNumber)}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } else {
            // Safe fallback to dialer if CALL_PHONE permission not yet granted
            Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:${Uri.encode(phoneNumber)}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }

        return try {
            context.startActivity(intent)
            CallResult(success = true, message = successMessage)
        } catch (e: Exception) {
            Log.e(TAG, "Error launching call intent", e)
            CallResult(success = false, message = "Unable to start the phone call.")
        }
    }
}
