package com.example.devsummit2026.ui.crashlytics

import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.content.Context
import android.os.Build
import android.os.Debug
import com.google.firebase.Firebase
import com.google.firebase.crashlytics.crashlytics

data class MemoryStats(
    val managedUsedMb: Float,
    val managedMaxMb: Float,
    val nativeAllocatedMb: Float
)

class MemoryLimitKillException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)

object MemoryDiagnosticsHelper {

    private const val PREFS_NAME = "crashlytics_memory_prefs"
    private const val KEY_LAST_REPORTED_EXIT_TS = "last_reported_exit_ts"

    fun readMemoryStats(): MemoryStats {
        val runtime = Runtime.getRuntime()
        val usedBytes = runtime.totalMemory() - runtime.freeMemory()
        val maxBytes = runtime.maxMemory()
        val nativeBytes = Debug.getNativeHeapAllocatedSize()
        return MemoryStats(
            managedUsedMb = bytesToMb(usedBytes),
            managedMaxMb = bytesToMb(maxBytes),
            nativeAllocatedMb = bytesToMb(nativeBytes)
        )
    }

    fun readLatestExitReason(context: Context): String {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return "Unavailable (API 30+)"
        }
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            ?: return "Unavailable"
        val latest = activityManager.getHistoricalProcessExitReasons(context.packageName, 0, 1).firstOrNull()
            ?: return "None"
        return formatExitReason(latest.reason)
    }

    /**
     * Checks ApplicationExitInfo on launch and reports OS memory-limit kills (LMK / SIGKILL)
     * to Firebase Crashlytics once per exit timestamp.
     */
    fun reportPendingMemoryKill(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            ?: return
        val latest = activityManager.getHistoricalProcessExitReasons(context.packageName, 0, 1).firstOrNull()
            ?: return

        val isMemoryKill = latest.reason == ApplicationExitInfo.REASON_LOW_MEMORY ||
            latest.reason == ApplicationExitInfo.REASON_EXCESSIVE_RESOURCE_USAGE ||
            (latest.reason == ApplicationExitInfo.REASON_SIGNALED && latest.status == 9)
        if (!isMemoryKill) return

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.getLong(KEY_LAST_REPORTED_EXIT_TS, 0L) == latest.timestamp) return
        prefs.edit().putLong(KEY_LAST_REPORTED_EXIT_TS, latest.timestamp).apply()

        val reasonLabel = formatExitReason(latest.reason)
        setCustomKey("crash_type", "memory_limit_kill")
        setCustomKey("exit_reason", reasonLabel)
        setCustomKey("exit_pss_mb", latest.pss / 1024f)
        setCustomKey("exit_rss_mb", latest.rss / 1024f)
        logBreadcrumb("OS memory kill detected: $reasonLabel (PSS=${latest.pss / 1024f}MB)")
        recordException(
            MemoryLimitKillException(
                "Process killed by OS memory limit: $reasonLabel (${latest.description ?: "LMK"})"
            )
        )
    }

    fun logBreadcrumb(message: String) {
        runCatching { Firebase.crashlytics.log(message) }
    }

    fun setCustomKey(key: String, value: String) {
        runCatching { Firebase.crashlytics.setCustomKey(key, value) }
    }

    fun setCustomKey(key: String, value: Float) {
        runCatching { Firebase.crashlytics.setCustomKey(key, value) }
    }

    private fun recordException(throwable: Throwable) {
        runCatching { Firebase.crashlytics.recordException(throwable) }
    }

    private fun bytesToMb(bytes: Long): Float = bytes / (1024f * 1024f)

    private fun formatExitReason(reason: Int): String = when (reason) {
        ApplicationExitInfo.REASON_EXIT_SELF -> "EXIT_SELF ($reason)"
        ApplicationExitInfo.REASON_SIGNALED -> "SIGNALED ($reason)"
        ApplicationExitInfo.REASON_LOW_MEMORY -> "LOW_MEMORY ($reason)"
        ApplicationExitInfo.REASON_CRASH -> "CRASH ($reason)"
        ApplicationExitInfo.REASON_CRASH_NATIVE -> "CRASH_NATIVE ($reason)"
        ApplicationExitInfo.REASON_ANR -> "ANR ($reason)"
        ApplicationExitInfo.REASON_INITIALIZATION_FAILURE -> "INIT_FAILURE ($reason)"
        ApplicationExitInfo.REASON_PERMISSION_CHANGE -> "PERMISSION_CHANGE ($reason)"
        ApplicationExitInfo.REASON_EXCESSIVE_RESOURCE_USAGE -> "EXCESSIVE_RESOURCE ($reason)"
        ApplicationExitInfo.REASON_USER_REQUESTED -> "USER_REQUESTED ($reason)"
        ApplicationExitInfo.REASON_USER_STOPPED -> "USER_STOPPED ($reason)"
        ApplicationExitInfo.REASON_DEPENDENCY_DIED -> "DEPENDENCY_DIED ($reason)"
        ApplicationExitInfo.REASON_OTHER -> "OTHER ($reason)"
        else -> "UNKNOWN ($reason)"
    }
}
