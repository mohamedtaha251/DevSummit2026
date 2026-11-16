package com.example.devsummit2026.security

import android.content.Context
import android.os.Build
import android.security.advancedprotection.AdvancedProtectionManager

/**
 * Educational helper to detect and interact with Android's Advanced Protection Mode (AAPM).
 *
 * Android Dev Summit 2026 Highlight:
 * Advanced Protection Mode introduces a system-level defense-in-depth shield for users at risk.
 * Starting in Android 15 (API 35+), apps can inspect this state via [AdvancedProtectionManager]
 * with the permission `android.permission.QUERY_ADVANCED_PROTECTION_MODE`.
 */
class SecurityManager(private val context: Context) {

    /**
     * Checks if Advanced Protection Mode is active on the current device.
     * Safely falls back to false on devices running Android 14 (API 34) or earlier,
     * or if the system service is unavailable.
     */
    fun isAdvancedProtectionEnabled(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) { // API 35+
            return try {
                val protectionManager = context.getSystemService(AdvancedProtectionManager::class.java)
                protectionManager?.isAdvancedProtectionEnabled ?: false
            } catch (e: Throwable) {
                // Safe fallback if the permission or service is unavailable on the platform
                false
            }
        }
        return false
    }

    /**
     * Returns true if the current OS level supports the AdvancedProtectionManager API (API 35+).
     */
    fun isAdvancedProtectionSupported(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM
    }

    /**
     * Returns the human-readable security status description.
     */
    fun getSecurityStatusDescription(): String {
        return when {
            !isAdvancedProtectionSupported() -> {
                "Standard Mode (Device running Android < 15, API ${Build.VERSION.SDK_INT})"
            }
            isAdvancedProtectionEnabled() -> {
                "Advanced Protection Mode: Active (Hardened System Security)"
            }
            else -> {
                "Standard Mode (Advanced Protection is currently disabled in system settings)"
            }
        }
    }
}
