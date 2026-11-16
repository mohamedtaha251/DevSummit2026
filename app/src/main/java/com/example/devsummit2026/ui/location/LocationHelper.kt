package com.example.devsummit2026.ui.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LocationHelper {

    fun hasLocationPermission(context: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    @SuppressLint("MissingPermission")
    fun fetchLocation(
        context: Context,
        onResult: (location: Location?, error: String?) -> Unit
    ) {
        if (!hasLocationPermission(context)) {
            onResult(null, "Location permission not granted. Please allow permission to continue.")
            return
        }

        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        if (locationManager == null) {
            onResult(null, "Location service is not available on this device.")
            return
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                locationManager.getCurrentLocation(
                    LocationManager.FUSED_PROVIDER,
                    null,
                    context.mainExecutor
                ) { location ->
                    handleLocationCallback(location, locationManager, onResult)
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                locationManager.getCurrentLocation(
                    LocationManager.GPS_PROVIDER,
                    null,
                    context.mainExecutor
                ) { location ->
                    handleLocationCallback(location, locationManager, onResult)
                }
            } else {
                val loc = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
                    ?: locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                handleLocationCallback(loc, locationManager, onResult)
            }
        } catch (e: SecurityException) {
            val fallback = try {
                locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
            } catch (_: SecurityException) {
                null
            }
            if (fallback != null) {
                onResult(fallback, null)
            } else {
                onResult(null, "Location permission required. Please allow access.")
            }
        } catch (e: Exception) {
            onResult(null, e.message ?: "Unable to retrieve location.")
        }
    }

    @SuppressLint("MissingPermission")
    private fun handleLocationCallback(
        location: Location?,
        locationManager: LocationManager,
        onResult: (location: Location?, error: String?) -> Unit
    ) {
        if (location != null) {
            onResult(location, null)
        } else {
            val fallback = try {
                locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
                    ?: locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            } catch (_: SecurityException) {
                null
            }
            if (fallback != null) {
                onResult(fallback, null)
            } else {
                onResult(null, "Waiting for GPS fix or location service...")
            }
        }
    }

    fun formatTimestamp(date: Date = Date()): String {
        return SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(date)
    }
}
