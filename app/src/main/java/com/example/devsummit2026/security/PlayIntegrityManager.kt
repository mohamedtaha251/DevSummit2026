package com.example.devsummit2026.security

import android.content.Context
import android.util.Base64
import com.google.android.play.core.integrity.IntegrityManager
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.android.play.core.integrity.IntegrityTokenRequest
import kotlinx.coroutines.suspendCancellableCoroutine
import java.security.SecureRandom
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Breakdown representing the 3 core integrity pillars verified by Google Play Integrity API.
 */
enum class IntegrityPillar(
    val title: String,
    val description: String,
    val badge: String
) {
    APP_BINARY(
        title = "Genuine App Binary",
        description = "Validates APK signature and package name against Google Play.",
        badge = "App Binary"
    ),
    HARDWARE(
        title = "Genuine Hardware",
        description = "Device meets basic, device, or strong hardware-backed attestation.",
        badge = "Hardware"
    ),
    ACCOUNT(
        title = "Licensed User Account",
        description = "Account has a valid Google Play license.",
        badge = "Account"
    )
}

/**
 * Lightweight educational helper for the Google Play Integrity API.
 */
class PlayIntegrityManager(context: Context) {

    private val integrityManager: IntegrityManager = IntegrityManagerFactory.create(context.applicationContext)

    companion object {
        const val CLOUD_PROJECT_NUMBER: Long = 269097251598L

        /**
         * Generates a cryptographically secure, URL-safe Base64-encoded nonce.
         */
        fun generateNonce(): String {
            val bytes = ByteArray(32)
            SecureRandom().nextBytes(bytes)
            return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        }
    }

    /**
     * Requests a runtime integrity token from Google Play Integrity API.
     */
    suspend fun requestIntegrityToken(nonce: String = generateNonce()): Result<String> = runCatching {
        val request = IntegrityTokenRequest.builder()
            .setCloudProjectNumber(CLOUD_PROJECT_NUMBER)
            .setNonce(nonce)
            .build()

        suspendCancellableCoroutine { continuation ->
            integrityManager.requestIntegrityToken(request)
                .addOnSuccessListener { response ->
                    continuation.resume(response.token())
                }
                .addOnFailureListener { exception ->
                    continuation.resumeWithException(exception)
                }
        }
    }
}
