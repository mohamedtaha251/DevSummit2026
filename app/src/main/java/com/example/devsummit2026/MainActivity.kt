package com.example.devsummit2026

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.ComposeUiFlags
import androidx.compose.ui.ExperimentalComposeUiApi
import com.example.devsummit2026.firebase.FirebaseAppCheckManager
import com.example.devsummit2026.navigation.AppNavigation
import com.example.devsummit2026.ui.crashlytics.MemoryDiagnosticsHelper
import com.example.devsummit2026.ui.theme.DevSummit2026Theme

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalComposeUiApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ComposeUiFlags.isMediaQueryIntegrationEnabled = true
        FirebaseAppCheckManager.initialize(BuildConfig.DEBUG)
        MemoryDiagnosticsHelper.reportPendingMemoryKill(this)
        enableEdgeToEdge()
        setContent {
            DevSummit2026Theme {
                AppNavigation()
            }
        }
    }
}
