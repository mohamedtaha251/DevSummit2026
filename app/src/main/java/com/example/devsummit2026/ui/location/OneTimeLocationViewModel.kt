package com.example.devsummit2026.ui.location

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class LocationMode(val title: String) {
    ONE_TIME_BUTTON("One-Time Button"),
    NORMAL_PERMISSION("Normal Permission")
}

data class LocationUiState(
    val selectedMode: LocationMode = LocationMode.ONE_TIME_BUTTON,
    val isAcquiring: Boolean = false,
    val locationFix: Pair<Double, Double>? = null,
    val timestamp: String = "",
    val isExpired: Boolean = false,
    val statusMessage: String? = null,
    val isPermissionGranted: Boolean = false,
    val accessDescription: String = ""
)

class OneTimeLocationViewModel(
    private val locationHelper: LocationHelper = LocationHelper()
) : ViewModel() {

    var uiState by mutableStateOf(LocationUiState())
        private set

    private var expiryJob: Job? = null

    fun checkPermission(context: Context) {
        val granted = locationHelper.hasLocationPermission(context)
        uiState = uiState.copy(isPermissionGranted = granted)
    }

    fun selectMode(mode: LocationMode) {
        expiryJob?.cancel()
        uiState = uiState.copy(
            selectedMode = mode,
            locationFix = null,
            statusMessage = null,
            isExpired = false,
            accessDescription = ""
        )
    }

    /**
     * Mode 1: One-Time Location flow (single permission / ephemeral access).
     */
    fun onOneTimePermissionResult(granted: Boolean, context: Context) {
        if (granted) {
            uiState = uiState.copy(isPermissionGranted = true, statusMessage = null)
            fetchOneTimeLocation(context)
        } else {
            uiState = uiState.copy(
                isAcquiring = false,
                isPermissionGranted = false,
                statusMessage = "One-time location permission was not granted."
            )
        }
    }

    /**
     * Acquires one-time location and applies the ephemeral auto-expiry lifecycle.
     */
    fun fetchOneTimeLocation(context: Context) {
        if (uiState.isAcquiring) return
        expiryJob?.cancel()

        uiState = uiState.copy(
            isAcquiring = true,
            isExpired = false,
            statusMessage = null,
            accessDescription = "One-Time Ephemeral Grant"
        )

        locationHelper.fetchLocation(context) { location, error ->
            uiState = uiState.copy(
                isAcquiring = false,
                timestamp = locationHelper.formatTimestamp(),
                statusMessage = error
            )

            if (location != null) {
                uiState = uiState.copy(
                    locationFix = location.latitude to location.longitude,
                    statusMessage = null
                )
                // Ephemeral grant auto-expires after 2.5 seconds to demonstrate the single-use lifecycle
                expiryJob = viewModelScope.launch {
                    delay(2500)
                    uiState = uiState.copy(isExpired = true)
                }
            }
        }
    }

    /**
     * Mode 2: Normal Permission Flow (standard runtime permission dialog).
     */
    fun onNormalPermissionResult(granted: Boolean, context: Context) {
        if (granted) {
            uiState = uiState.copy(isPermissionGranted = true, statusMessage = null)
            fetchNormalLocation(context)
        } else {
            uiState = uiState.copy(
                isAcquiring = false,
                isPermissionGranted = false,
                statusMessage = "Location permission denied in system dialog."
            )
        }
    }

    /**
     * Acquires location under standard persistent permission.
     */
    fun fetchNormalLocation(context: Context) {
        if (uiState.isAcquiring) return
        expiryJob?.cancel()

        uiState = uiState.copy(
            isAcquiring = true,
            isExpired = false,
            statusMessage = null,
            accessDescription = "Standard Persistent Grant"
        )

        locationHelper.fetchLocation(context) { location, error ->
            uiState = uiState.copy(
                isAcquiring = false,
                timestamp = locationHelper.formatTimestamp(),
                statusMessage = error
            )

            if (location != null) {
                uiState = uiState.copy(
                    locationFix = location.latitude to location.longitude,
                    statusMessage = null
                )
            }
        }
    }
}
