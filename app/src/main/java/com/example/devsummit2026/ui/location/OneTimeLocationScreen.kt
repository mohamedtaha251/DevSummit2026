package com.example.devsummit2026.ui.location

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.locationbutton.compose.LocationButton
import androidx.core.locationbutton.compose.LocationButtonTextType
import androidx.lifecycle.viewmodel.compose.viewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OneTimeLocationScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OneTimeLocationViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState = viewModel.uiState

    LaunchedEffect(Unit) {
        viewModel.checkPermission(context)
    }

    // Normal Permission request with full dialog (Fine + Coarse)
    val normalPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val isGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        viewModel.onNormalPermissionResult(isGranted, context)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Location Access Modes") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { insets ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(insets)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Mode Selector: One-Time Button vs Normal Permission
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth()
            ) {
                LocationMode.entries.forEachIndexed { index, mode ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = LocationMode.entries.size
                        ),
                        onClick = { viewModel.selectMode(mode) },
                        selected = uiState.selectedMode == mode
                    ) {
                        Text(mode.title)
                    }
                }
            }

            // Short Description of the Selected Difference
            Text(
                text = when (uiState.selectedMode) {
                    LocationMode.ONE_TIME_BUTTON ->
                        "Location Button: Android 17 private alternative • No permission dialog • Ephemeral session"
                    LocationMode.NORMAL_PERMISSION ->
                        "Normal Request: Prompts full dialog (Precise/Approximate) • Persistent access"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Interactive Action Button
            when (uiState.selectedMode) {
                LocationMode.ONE_TIME_BUTTON -> {
                    LocationButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        cornerRadius = 12.dp,
                        textType = LocationButtonTextType.UsePreciseLocation,
                        onPermissionResult = { isGranted ->
                            viewModel.onOneTimePermissionResult(isGranted, context)
                        }
                    )
                }

                LocationMode.NORMAL_PERMISSION -> {
                    Button(
                        onClick = {
                            if (uiState.isPermissionGranted) {
                                viewModel.fetchNormalLocation(context)
                            } else {
                                normalPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            }
                        },
                        enabled = !uiState.isAcquiring,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (uiState.isAcquiring) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Acquiring...")
                        } else {
                            Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (uiState.isPermissionGranted) "Get Location (Permission Granted)"
                                else "Request Location (Show Dialog)"
                            )
                        }
                    }
                }
            }

            // Error or Status message
            uiState.statusMessage?.let { msg ->
                Text(
                    text = msg,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            // Location Result Card
            uiState.locationFix?.let { (lat, lng) ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Coordinates",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = when {
                                    uiState.selectedMode == LocationMode.NORMAL_PERMISSION ->
                                        MaterialTheme.colorScheme.primaryContainer
                                    uiState.isExpired ->
                                        MaterialTheme.colorScheme.errorContainer
                                    else -> Color(0xFFC8E6C9)
                                }
                            ) {
                                Text(
                                    text = when {
                                        uiState.selectedMode == LocationMode.NORMAL_PERMISSION -> "PERSISTENT"
                                        uiState.isExpired -> "EXPIRED"
                                        else -> "ACTIVE"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = when {
                                        uiState.selectedMode == LocationMode.NORMAL_PERMISSION ->
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        uiState.isExpired ->
                                            MaterialTheme.colorScheme.onErrorContainer
                                        else -> Color(0xFF1B5E20)
                                    },
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(
                            text = String.format(Locale.US, "Lat: %.6f, Lng: %.6f", lat, lng),
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Text(
                            text = "Access: ${uiState.accessDescription}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )

                        Text(
                            text = "Time: ${uiState.timestamp}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
