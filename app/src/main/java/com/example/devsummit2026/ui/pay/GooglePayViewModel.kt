package com.example.devsummit2026.ui.pay

import android.app.Application
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.AndroidViewModel
import com.example.devsummit2026.pay.PaymentsUtil
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.tasks.Task
import com.google.android.gms.wallet.IsReadyToPayRequest
import com.google.android.gms.wallet.PaymentData
import com.google.android.gms.wallet.PaymentDataRequest
import com.google.android.gms.wallet.PaymentsClient
import com.google.android.gms.wallet.contract.ApiTaskResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONObject

sealed interface PaymentStatus {
    data object Idle : PaymentStatus
    data object Loading : PaymentStatus
    data class Success(
        val amount: String,
        val cardNetwork: String,
        val cardLastFour: String,
        val tokenPrefix: String,
        val description: String
    ) : PaymentStatus
    data object Canceled : PaymentStatus
    data class Error(val message: String, val statusCode: Int? = null) : PaymentStatus
}

data class GooglePayUiState(
    val isReadyToPay: Boolean? = null,
    val paymentStatus: PaymentStatus = PaymentStatus.Idle,
    val productName: String = "Demo T-shirt",
    val productPrice: String = "$12.50"
)

class GooglePayViewModel(application: Application) : AndroidViewModel(application) {

    private val paymentsClient: PaymentsClient = PaymentsUtil.createPaymentsClient(application)

    private val _uiState = MutableStateFlow(GooglePayUiState())
    val uiState: StateFlow<GooglePayUiState> = _uiState.asStateFlow()

    val allowedPaymentMethods: String = PaymentsUtil.isReadyToPayRequest()
        .getJSONArray("allowedPaymentMethods")
        .toString()

    init {
        checkIsReadyToPay()
    }

    fun checkIsReadyToPay() {
        val request = IsReadyToPayRequest.fromJson(PaymentsUtil.isReadyToPayRequest().toString())
        paymentsClient.isReadyToPay(request).addOnCompleteListener { task ->
            val available = task.isSuccessful && (task.result == true)
            _uiState.update { it.copy(isReadyToPay = available) }
        }
    }

    /**
     * Executes the payment workflow by building the payment request and launching Google Pay.
     */
    fun payWithGoogle(launcher: ActivityResultLauncher<Task<PaymentData>>) {
        _uiState.update { it.copy(paymentStatus = PaymentStatus.Loading) }
        try {
            val paymentDataRequestJson = PaymentsUtil.getPaymentDataRequest("12.50", "USD").toString()
            val request = PaymentDataRequest.fromJson(paymentDataRequestJson)
            paymentsClient.loadPaymentData(request).addOnCompleteListener { completedTask ->
                launcher.launch(completedTask)
            }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(paymentStatus = PaymentStatus.Error("Failed to initiate payment: ${e.localizedMessage ?: "Unknown error"}"))
            }
        }
    }

    /**
     * Processes the result returned from the wallet ActivityResult contract.
     */
    fun handlePaymentResult(apiResult: ApiTaskResult<PaymentData>) {
        val status = apiResult.status
        when (status.statusCode) {
            CommonStatusCodes.SUCCESS -> {
                apiResult.result?.let { onPaymentResult(it) }
                    ?: onPaymentError(-1, "No payment data returned")
            }
            CommonStatusCodes.CANCELED -> onPaymentCanceled()
            else -> onPaymentError(status.statusCode, status.statusMessage)
        }
    }

    private fun onPaymentResult(paymentData: PaymentData) {
        try {
            val paymentInfo = JSONObject(paymentData.toJson())
            val paymentMethodData = paymentInfo.getJSONObject("paymentMethodData")
            val description = paymentMethodData.optString("description", "Card")
            val info = paymentMethodData.getJSONObject("info")
            val cardNetwork = info.optString("cardNetwork", "Unknown")
            val cardDetails = info.optString("cardDetails", "")

            val tokenizationData = paymentMethodData.getJSONObject("tokenizationData")
            val rawToken = tokenizationData.optString("token", "")
            // Display first 12 characters only for verification, never log or store full token
            val tokenPrefix = if (rawToken.length >= 12) rawToken.substring(0, 12) + "..." else rawToken

            _uiState.update {
                it.copy(
                    paymentStatus = PaymentStatus.Success(
                        amount = it.productPrice,
                        cardNetwork = cardNetwork,
                        cardLastFour = cardDetails,
                        tokenPrefix = tokenPrefix,
                        description = description
                    )
                )
            }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(paymentStatus = PaymentStatus.Error("Failed to parse payment data: ${e.localizedMessage ?: "Unknown error"}"))
            }
        }
    }

    private fun onPaymentCanceled() {
        _uiState.update { it.copy(paymentStatus = PaymentStatus.Canceled) }
    }

    private fun onPaymentError(statusCode: Int, message: String?) {
        _uiState.update {
            it.copy(
                paymentStatus = PaymentStatus.Error(
                    message = message ?: "Payment failed with status code $statusCode",
                    statusCode = statusCode
                )
            )
        }
    }
}
