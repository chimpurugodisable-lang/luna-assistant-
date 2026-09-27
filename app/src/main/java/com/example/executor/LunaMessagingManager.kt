package com.example.executor

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.telephony.SmsManager
import android.util.Log
import androidx.core.content.ContextCompat

class LunaMessagingManager(
    private val context: Context,
    private val contactsManager: LunaContactsManager
) {

    companion object {
        private const val TAG = "LunaMessagingManager"
    }

    data class MessageResult(
        val success: Boolean,
        val message: String
    )

    /**
     * Dispatches messaging request to WhatsApp, WhatsApp Business, or SMS.
     */
    fun sendMessage(
        recipient: String,
        messageText: String,
        isWhatsApp: Boolean = false,
        isWhatsAppBusiness: Boolean = false
    ): MessageResult {
        val phoneNumber = contactsManager.findPhoneNumberForName(recipient) ?: if (recipient.replace(Regex("[+\\-\\s()]"), "").all { it.isDigit() }) recipient else null

        if (isWhatsApp || isWhatsAppBusiness) {
            val targetPkg = if (isWhatsAppBusiness) "com.whatsapp.w4b" else "com.whatsapp"
            val appLabel = if (isWhatsAppBusiness) "WhatsApp Business" else "WhatsApp"

            return if (!phoneNumber.isNullOrEmpty()) {
                val cleanNumber = phoneNumber.replace(Regex("[^0-9+]"), "").removePrefix("+")
                val url = "https://api.whatsapp.com/send?phone=$cleanNumber&text=${Uri.encode(messageText)}"
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    setPackage(targetPkg)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }

                try {
                    context.startActivity(intent)
                    MessageResult(true, "Opening $appLabel to message $recipient.")
                } catch (e: Exception) {
                    // Try without package restriction
                    try {
                        val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(fallbackIntent)
                        MessageResult(true, "Opening $appLabel to message $recipient.")
                    } catch (e2: Exception) {
                        Log.e(TAG, "WhatsApp launch failed", e2)
                        MessageResult(false, "$appLabel is not installed on this device.")
                    }
                }
            } else {
                // No phone number, open WhatsApp with text share
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, messageText)
                    setPackage(targetPkg)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                try {
                    context.startActivity(shareIntent)
                    MessageResult(true, "Opening $appLabel for $recipient.")
                } catch (e: Exception) {
                    MessageResult(false, "Could not open $appLabel.")
                }
            }
        } else {
            // Standard SMS. If we have a resolved number and the SEND_SMS permission,
            // send it directly and silently — no app opens, no tap required, works with
            // the screen off. Otherwise fall back to opening the messaging app pre-filled.
            val hasSmsPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.SEND_SMS
            ) == PackageManager.PERMISSION_GRANTED

            if (!phoneNumber.isNullOrEmpty() && hasSmsPermission) {
                return try {
                    val cleanNumber = phoneNumber.replace(Regex("[^0-9+]"), "")
                    // getDefault() (not the API 31+ Context.getSystemService overload) keeps
                    // this working all the way down to minSdk 26.
                    @Suppress("DEPRECATION")
                    val smsManager = SmsManager.getDefault()
                    // Long messages must be split into multiple SMS parts or the carrier drops them.
                    val parts = smsManager.divideMessage(messageText)
                    if (parts.size > 1) {
                        smsManager.sendMultipartTextMessage(cleanNumber, null, parts, null, null)
                    } else {
                        smsManager.sendTextMessage(cleanNumber, null, messageText, null, null)
                    }
                    MessageResult(true, "Message sent to $recipient.")
                } catch (e: Exception) {
                    Log.e(TAG, "Direct SMS send failed", e)
                    // Fall back to opening the app rather than silently failing.
                    openSmsApp(phoneNumber, messageText, recipient)
                }
            } else {
                val reason = if (!hasSmsPermission) {
                    " Grant the SMS permission in Luna for hands-free sending."
                } else ""
                val result = openSmsApp(phoneNumber, messageText, recipient)
                return result.copy(message = result.message + reason)
            }
        }
    }

    private fun openSmsApp(phoneNumber: String?, messageText: String, recipient: String): MessageResult {
        val smsUri = if (!phoneNumber.isNullOrEmpty()) {
            Uri.parse("smsto:${phoneNumber.replace(Regex("[^0-9+]"), "")}")
        } else {
            Uri.parse("smsto:")
        }

        val smsIntent = Intent(Intent.ACTION_SENDTO, smsUri).apply {
            putExtra("sms_body", messageText)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return try {
            context.startActivity(smsIntent)
            MessageResult(true, "Opening messaging app to text $recipient.")
        } catch (e: Exception) {
            Log.e(TAG, "SMS launch failed", e)
            MessageResult(false, "Could not open messaging app.")
        }
    }
}
