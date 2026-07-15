package ke.co.smartroundclinic.payments.data.remote.instasend.reseponse

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Response schema for GET /wallets/{walletId}/transactions/ — IntaSend's own wallet ledger. */
@Serializable
data class WalletTransactionsRes(
    val count: Int = 0,
    val next: String? = null,
    val previous: String? = null,
    val results: List<WalletTransactionItem> = emptyList(),
)

@Serializable
data class WalletTransactionItem(
    @SerialName("transaction_id")
    val transactionId: String,
    val invoice: WalletTransactionInvoice? = null,
    val currency: String,
    val value: Double,
    @SerialName("running_balance")
    val runningBalance: Double,
    val narrative: String? = null,
    @SerialName("trans_type")
    val transType: String,
    val status: String,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("updated_at")
    val updatedAt: String? = null,
)

/**
 * Only present on SALE-type transactions (a wallet's own M-Pesa collection settling) — null for
 * everything else (PAYOUT, RESERVE, CHARGE). IntaSend nests the original invoice/payment details
 * here rather than sending just an id, so this must be an object, not a plain string.
 */
@Serializable
data class WalletTransactionInvoice(
    @SerialName("invoice_id")
    val invoiceId: String? = null,
    val state: String? = null,
    val provider: String? = null,
    val charges: Double? = null,
    @SerialName("net_amount")
    val netAmount: String? = null,
    val currency: String? = null,
    val value: Double? = null,
    val account: String? = null,
    @SerialName("api_ref")
    val apiRef: String? = null,
    @SerialName("provider_ref")
    val providerRef: String? = null,
    @SerialName("clearing_status")
    val clearingStatus: String? = null,
    @SerialName("mpesa_reference")
    val mpesaReference: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null,
)
