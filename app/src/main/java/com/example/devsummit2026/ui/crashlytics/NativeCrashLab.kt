package com.example.devsummit2026.ui.crashlytics

object NativeCrashLab {
    init {
        System.loadLibrary("native_crash_lab")
    }

    external fun triggerGwpAsanOrMteCrash()
}
