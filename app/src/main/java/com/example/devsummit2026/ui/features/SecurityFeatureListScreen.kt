package com.example.devsummit2026.ui.features

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.devsummit2026.navigation.Screen

data class SecurityFeatureItem(
    val id: String,
    val title: String,
    val badge: String,
    val description: String,
    val route: String,
    val icon: ImageVector
)

val defaultSecurityFeatureItems = listOf(
    SecurityFeatureItem(
        id = "advanced_protection",
        title = "Advanced Protection Mode",
        badge = "API 35+",
        description = "Detect enrollment in Advanced Protection and gate high-risk actions with step-up biometric authentication.",
        route = Screen.SecurityFeatures.route,
        icon = Icons.Default.Shield
    ),
    SecurityFeatureItem(
        id = "play_integrity",
        title = "Play Integrity API",
        badge = "Attestation",
        description = "Runtime verification of app binary, genuine hardware, and user licensing against fraud and tampering.",
        route = Screen.PlayIntegrity.route,
        icon = Icons.Default.Security
    ),
    SecurityFeatureItem(
        id = "sms_otp",
        title = "SMS OTP: Old vs New",
        badge = "Zero-Permission",
        description = "Compare dangerous legacy RECEIVE_SMS permission against modern zero-permission SMS Retriever & User Consent APIs.",
        route = Screen.SmsVerification.route,
        icon = Icons.Default.Sms
    ),
    SecurityFeatureItem(
        id = "one_time_location",
        title = "One-Time Location Access",
        badge = "Modern Privacy",
        description = "Zero-popup friction, single-tap consent, ephemeral permission with onlyForLocationButton flags.",
        route = Screen.OneTimeLocation.route,
        icon = Icons.Default.NearMe
    ),
    SecurityFeatureItem(
        id = "cronet_quic_upload",
        title = "Cronet & QUIC Migration",
        badge = "HTTP/3 Handover",
        description = "Seamless Wi-Fi to Cellular handover with QUIC connection migration during active streaming upload without socket resets.",
        route = Screen.CronetUpload.route,
        icon = Icons.Default.CloudUpload
    ),
    SecurityFeatureItem(
        id = "google_pay_checkout",
        title = "Google Pay Checkout",
        badge = "Tokenized Pay",
        description = "Test mode in-app checkout demonstrating secure payment tokenization without exposing sensitive card PAN.",
        route = Screen.GooglePayDemo.route,
        icon = Icons.Default.Payment
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityFeatureListScreen(
    onItemClick: (SecurityFeatureItem) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    items: List<SecurityFeatureItem> = defaultSecurityFeatureItems
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Security & Privacy",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "Protection, Zero-Permission & Ephemeral APIs",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { insets ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(insets),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "Security & Privacy Labs",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            items(items, key = { it.id }) { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onItemClick(item) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier
                                    .padding(12.dp)
                                    .size(24.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.padding(bottom = 4.dp)
                            ) {
                                Text(
                                    text = item.badge,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = item.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Navigate to ${item.title}",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
