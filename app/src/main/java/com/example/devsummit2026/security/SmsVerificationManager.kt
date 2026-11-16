package com.example.devsummit2026.security

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.util.Base64
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.auth.api.phone.SmsRetriever
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

/**
 * Manages zero-permission SMS OTP verification using Google Play Services:
 * 1. SMS Retriever API: 100% automatic retrieval using the 11-character app hash.
 * 2. SMS User Consent API: One-time system dialog fallback for arbitrary senders or messages without hash.
 *
 * Fully adheres to Google Play zero-permission SMS policy.
 */
class SmsVerificationManager(
    private val context: Context,
    var onOtpReceived: ((String) -> Unit)? = null,
    var onConsentIntentReceived: ((Intent) -> Unit)? = null,
    var onError: ((String) -> Unit)? = null
) {
    companion object {
        private const val TAG = "SmsVerificationManager"
        private const val HASH_TYPE = "SHA-256"
        private const val NUM_HASHED_BYTES = 9
        private const val NUM_BASE64_CHARS = 11

        /**
         * Clean regex helper extracting 4-to-6 digit OTP codes from the SMS body.
         */
        fun extractOtp(message: String?): String? {
            if (message.isNullOrBlank()) return null
            val regex = Regex("""\b(\d{4,6})\b""")
            return regex.find(message)?.value
        }
    }

    private var isReceiverRegistered = false

    private val smsVerificationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == SmsRetriever.SMS_RETRIEVED_ACTION) {
                val extras = intent.extras ?: return
                val status = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    extras.getParcelable(SmsRetriever.EXTRA_STATUS, Status::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    extras.getParcelable(SmsRetriever.EXTRA_STATUS)
                }

                when (status?.statusCode) {
                    CommonStatusCodes.SUCCESS -> {
                        // User Consent API returns EXTRA_CONSENT_INTENT to show user consent prompt
                        @Suppress("DEPRECATION")
                        val consentIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            extras.getParcelable(SmsRetriever.EXTRA_CONSENT_INTENT, Intent::class.java)
                        } else {
                            extras.getParcelable(SmsRetriever.EXTRA_CONSENT_INTENT)
                        }

                        if (consentIntent != null) {
                            Log.d(TAG, "Consent Intent received, requesting launcher")
                            onConsentIntentReceived?.invoke(consentIntent)
                        } else {
                            // SMS Retriever API returns the full message in EXTRA_SMS_MESSAGE
                            val message = extras.getString(SmsRetriever.EXTRA_SMS_MESSAGE)
                            Log.d(TAG, "SMS retrieved automatically: $message")
                            val otp = extractOtp(message)
                            if (otp != null) {
                                onOtpReceived?.invoke(otp)
                            } else {
                                onError?.invoke("OTP code not found in SMS message")
                            }
                        }
                    }
                    CommonStatusCodes.TIMEOUT -> {
                        Log.w(TAG, "SMS retrieval timed out")
                        onError?.invoke("SMS retrieval timed out")
                    }
                    else -> {
                        val message = status?.statusMessage ?: "Error status: ${status?.statusCode}"
                        Log.e(TAG, "SMS retrieval failed: $message")
                        onError?.invoke(message)
                    }
                }
            }
        }
    }

    /**
     * Starts the SMS Retriever API for automatic zero-permission retrieval.
     */
    fun startSmsRetriever(
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        registerReceiver()
        val client = SmsRetriever.getClient(context)
        client.startSmsRetriever()
            .addOnSuccessListener {
                Log.d(TAG, "SMS Retriever API started")
                onSuccess()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to start SMS Retriever API", e)
                onError?.invoke(e.localizedMessage ?: "Failed to start SMS Retriever")
                onFailure(e)
            }
    }

    /**
     * Starts the SMS User Consent API as a one-time dialog fallback.
     *
     * @param senderPhoneNumber Optional sender phone number filter or null for any sender.
     */
    fun startSmsUserConsent(
        senderPhoneNumber: String? = null,
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        registerReceiver()
        val client = SmsRetriever.getClient(context)
        client.startSmsUserConsent(senderPhoneNumber)
            .addOnSuccessListener {
                Log.d(TAG, "SMS User Consent API started")
                onSuccess()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to start SMS User Consent API", e)
                onError?.invoke(e.localizedMessage ?: "Failed to start SMS User Consent")
                onFailure(e)
            }
    }

    /**
     * Registers the BroadcastReceiver with SmsRetriever.SEND_PERMISSION and RECEIVER_EXPORTED.
     */
    fun registerReceiver() {
        if (!isReceiverRegistered) {
            val intentFilter = IntentFilter(SmsRetriever.SMS_RETRIEVED_ACTION)
            ContextCompat.registerReceiver(
                context,
                smsVerificationReceiver,
                intentFilter,
                SmsRetriever.SEND_PERMISSION,
                null,
                ContextCompat.RECEIVER_EXPORTED
            )
            isReceiverRegistered = true
            Log.d(TAG, "SMS BroadcastReceiver registered")
        }
    }

    /**
     * Unregisters the BroadcastReceiver safely.
     */
    fun unregisterReceiver() {
        if (isReceiverRegistered) {
            try {
                context.unregisterReceiver(smsVerificationReceiver)
                Log.d(TAG, "SMS BroadcastReceiver unregistered")
            } catch (e: Exception) {
                Log.e(TAG, "Error unregistering SMS BroadcastReceiver", e)
            }
            isReceiverRegistered = false
        }
    }

    /**
     * Calculates and logs the 11-character app hash string for local testing via `adb emu sms send`.
     */
    fun getAppSignatures(): List<String> {
        val appCodes = mutableListOf<String>()
        try {
            val packageName = context.packageName
            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val packageInfo = context.packageManager.getPackageInfo(
                    packageName,
                    PackageManager.GET_SIGNING_CERTIFICATES
                )
                val signingInfo = packageInfo.signingInfo
                if (signingInfo?.hasMultipleSigners() == true) {
                    signingInfo.apkContentsSigners
                } else {
                    signingInfo?.signingCertificateHistory ?: signingInfo?.apkContentsSigners
                }
            } else {
                @Suppress("DEPRECATION")
                val packageInfo = context.packageManager.getPackageInfo(
                    packageName,
                    PackageManager.GET_SIGNATURES
                )
                @Suppress("DEPRECATION")
                packageInfo.signatures
            }

            signatures?.forEach { signature ->
                val hash = hash(packageName, signature.toCharsString())
                if (hash != null) {
                    appCodes.add(hash)
                    Log.d(TAG, "==================================================")
                    Log.d(TAG, "SMS Retriever App Hash: $hash")
                    Log.d(TAG, "Local test via adb: adb emu sms send 123456 '<#> Your OTP is 123456 $hash'")
                    Log.d(TAG, "==================================================")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating app signature hash", e)
        }
        return appCodes
    }

    private fun hash(packageName: String, signature: String): String? {
        val appInfo = "$packageName $signature"
        return try {
            val messageDigest = MessageDigest.getInstance(HASH_TYPE)
            messageDigest.update(appInfo.toByteArray(StandardCharsets.UTF_8))
            val hashSignature = messageDigest.digest().copyOfRange(0, NUM_HASHED_BYTES)
            var base64Hash = Base64.encodeToString(hashSignature, Base64.NO_PADDING or Base64.NO_WRAP)
            base64Hash = base64Hash.substring(0, NUM_BASE64_CHARS)
            base64Hash
        } catch (e: Exception) {
            Log.e(TAG, "Error computing SHA-256 hash", e)
            null
        }
    }
}
