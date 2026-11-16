package com.example.devsummit2026.ui.security

import android.Manifest
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.provider.Telephony
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.devsummit2026.security.SmsVerificationManager
import com.google.android.gms.auth.api.phone.SmsRetriever

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmsOtpComparisonScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // State for Old Approach
    var otpCodeOld by remember { mutableStateOf("") }
    var hasSmsPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECEIVE_SMS
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    var isLegacyListening by remember { mutableStateOf(false) }

    // State for New Approach
    var otpCodeNew by remember { mutableStateOf("") }

    // Legacy BroadcastReceiver for RECEIVE_SMS
    val legacyReceiver = remember {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
                    val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
                    val fullBody = messages?.joinToString("") { it.displayMessageBody }
                    val extracted = SmsVerificationManager.extractOtp(fullBody)
                    if (extracted != null) {
                        otpCodeOld = extracted
                        Toast.makeText(context, "Old Approach: OTP $extracted extracted via RECEIVE_SMS", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    // Permission launcher for Old Approach
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasSmsPermission = isGranted
        if (isGranted) {
            try {
                val filter = IntentFilter(Telephony.Sms.Intents.SMS_RECEIVED_ACTION)
                ContextCompat.registerReceiver(context, legacyReceiver, filter, ContextCompat.RECEIVER_EXPORTED)
                isLegacyListening = true
                Toast.makeText(context, "Permission granted! Listening for all SMS...", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Error registering receiver: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "RECEIVE_SMS permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    // Activity Result Launcher for SMS User Consent Dialog (New Approach)
    val consentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val message = result.data?.getStringExtra(SmsRetriever.EXTRA_SMS_MESSAGE)
            val extracted = SmsVerificationManager.extractOtp(message)
            if (extracted != null) {
                otpCodeNew = extracted
                Toast.makeText(context, "New Approach: OTP $extracted retrieved via Consent Dialog", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Modern Zero-Permission SMS Verification Manager
    val smsVerificationManager = remember {
        SmsVerificationManager(
            context = context,
            onOtpReceived = { code ->
                otpCodeNew = code
                Toast.makeText(context, "New Approach: OTP $code auto-retrieved via App Hash!", Toast.LENGTH_SHORT).show()
            },
            onConsentIntentReceived = { consentIntent ->
                consentLauncher.launch(consentIntent)
            },
            onError = { error ->
                Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Lifecycle cleanup
    DisposableEffect(Unit) {
        smsVerificationManager.getAppSignatures()
        onDispose {
            smsVerificationManager.unregisterReceiver()
            if (isLegacyListening) {
                try {
                    context.unregisterReceiver(legacyReceiver)
                } catch (_: Exception) {}
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("SMS OTP: Old vs New") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { insets ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(insets)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // CARD 1: OLD APPROACH
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "Old Approach (Legacy)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (hasSmsPermission) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
                        ) {
                            Text(
                                text = if (hasSmsPermission) "Permission Granted" else "RECEIVE_SMS Needed",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Text(
                        text = "Requires dangerous RECEIVE_SMS permission. Accesses the user's entire SMS inbox. High risk of rejection on Google Play.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = otpCodeOld,
                        onValueChange = { otpCodeOld = it },
                        label = { Text("OTP Code (Old Way)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    Button(
                        onClick = {
                            if (!hasSmsPermission) {
                                permissionLauncher.launch(Manifest.permission.RECEIVE_SMS)
                            } else if (!isLegacyListening) {
                                try {
                                    val filter = IntentFilter(Telephony.Sms.Intents.SMS_RECEIVED_ACTION)
                                    ContextCompat.registerReceiver(context, legacyReceiver, filter, ContextCompat.RECEIVER_EXPORTED)
                                    isLegacyListening = true
                                    Toast.makeText(context, "Listening for incoming SMS (Old way)...", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(context, "Already listening for SMS via RECEIVE_SMS", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text(
                            if (!hasSmsPermission) "Request RECEIVE_SMS Permission"
                            else if (isLegacyListening) "Listening (Legacy Receiver Active)"
                            else "Start Legacy SMS Receiver"
                        )
                    }
                }
            }

            // CARD 2: NEW APPROACH
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "New Approach (Modern)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "0 Permissions Needed",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    Text(
                        text = "100% zero-permission and Google Play compliant. The app never reads the user's inbox—it only receives its own verification code.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = otpCodeNew,
                        onValueChange = { otpCodeNew = it },
                        label = { Text("OTP Code (Modern Way)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    Button(
                        onClick = {
                            smsVerificationManager.startSmsRetriever(
                                onSuccess = {
                                    Toast.makeText(context, "Listening for SMS with 11-char app hash...", Toast.LENGTH_SHORT).show()
                                }
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Start SMS Retriever (Auto with Hash)")
                    }

                    OutlinedButton(
                        onClick = {
                            smsVerificationManager.startSmsUserConsent(
                                senderPhoneNumber = null,
                                onSuccess = {
                                    Toast.makeText(context, "Listening for incoming SMS (System Dialog)...", Toast.LENGTH_SHORT).show()
                                }
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Start User Consent (Dialog Fallback)")
                    }
                }
            }
        }
    }
}
