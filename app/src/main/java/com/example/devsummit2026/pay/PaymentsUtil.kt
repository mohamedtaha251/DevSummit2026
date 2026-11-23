package com.example.devsummit2026.pay

import android.content.Context
import com.google.android.gms.wallet.PaymentsClient
import com.google.android.gms.wallet.Wallet
import com.google.android.gms.wallet.WalletConstants
import org.json.JSONArray
import org.json.JSONObject

/**
 * Helper utility for Google Pay integration using TEST environment.
 */
object PaymentsUtil {

    private const val CENTS = 100

    /** Base configuration with API version 2.0 */
    private val baseRequest: JSONObject
        get() = JSONObject()
            .put("apiVersion", 2)
            .put("apiVersionMinor", 0)

    /** Allowed card networks for demo */
    private val allowedCardNetworks = JSONArray(listOf("VISA", "MASTERCARD"))

    /** Allowed authentication methods */
    private val allowedCardAuthMethods = JSONArray(listOf("PAN_ONLY", "CRYPTOGRAM_3DS"))

    /**
     * Gateway tokenization specification (test placeholders).
     */
    private val gatewayTokenizationSpecification: JSONObject
        get() = JSONObject()
            .put("type", "PAYMENT_GATEWAY")
            .put(
                "parameters", JSONObject()
                    .put("gateway", "example")
                    .put("gatewayMerchantId", "exampleGatewayMerchantId")
            )

    /**
     * Base card payment method without tokenization (for IsReadyToPay).
     */
    private val baseCardPaymentMethod: JSONObject
        get() = JSONObject()
            .put("type", "CARD")
            .put(
                "parameters", JSONObject()
                    .put("allowedAuthMethods", allowedCardAuthMethods)
                    .put("allowedCardNetworks", allowedCardNetworks)
            )

    /**
     * Card payment method with tokenization (for PaymentDataRequest).
     */
    private val cardPaymentMethod: JSONObject
        get() = baseCardPaymentMethod
            .put("tokenizationSpecification", gatewayTokenizationSpecification)

    /**
     * Creates a PaymentsClient configured for TEST environment.
     */
    fun createPaymentsClient(context: Context): PaymentsClient {
        val walletOptions = Wallet.WalletOptions.Builder()
            .setEnvironment(WalletConstants.ENVIRONMENT_TEST)
            .build()
        return Wallet.getPaymentsClient(context, walletOptions)
    }

    /**
     * Builds IsReadyToPayRequest JSON string.
     */
    fun isReadyToPayRequest(): JSONObject {
        return baseRequest
            .put("allowedPaymentMethods", JSONArray().put(baseCardPaymentMethod))
    }

    /**
     * Builds PaymentDataRequest JSON string with specified price and merchant details.
     */
    fun getPaymentDataRequest(price: String = "12.50", currencyCode: String = "USD"): JSONObject {
        val transactionInfo = JSONObject()
            .put("totalPrice", price)
            .put("totalPriceStatus", "FINAL")
            .put("currencyCode", currencyCode)

        val merchantInfo = JSONObject()
            .put("merchantName", "Demo Store")

        return baseRequest
            .put("allowedPaymentMethods", JSONArray().put(cardPaymentMethod))
            .put("transactionInfo", transactionInfo)
            .put("merchantInfo", merchantInfo)
    }
}
