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
    val invoice: String? = null,
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
