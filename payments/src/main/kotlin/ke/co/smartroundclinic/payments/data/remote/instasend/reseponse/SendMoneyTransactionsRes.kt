package ke.co.smartroundclinic.payments.data.remote.instasend.reseponse

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Response schema for GET /send-money/transactions/?wallet_id={walletId} and .../transactions/{id}/ */
@Serializable
data class SendMoneyTransactionsRes(
    val count: Int = 0,
    val next: String? = null,
    val previous: String? = null,
    val results: List<SendMoneyTransactionItem> = emptyList(),
)

@Serializable
data class SendMoneyTransactionItem(
    @SerialName("transaction_id")
    val transactionId: String,
    val status: String,
    @SerialName("status_code")
    val statusCode: String? = null,
    @SerialName("status_description")
    val statusDescription: String? = null,
    @SerialName("request_reference_id")
    val requestReferenceId: String? = null,
    val provider: String? = null,
    @SerialName("bank_code")
    val bankCode: String? = null,
    val name: String? = null,
    val account: String? = null,
    @SerialName("account_type")
    val accountType: String? = null,
    @SerialName("account_reference")
    val accountReference: String? = null,
    @SerialName("provider_reference")
    val providerReference: String? = null,
    @SerialName("provider_account_name")
    val providerAccountName: String? = null,
    val amount: String,
    val charge: String? = null,
    val narrative: String? = null,
    @SerialName("file_id")
    val fileId: String? = null,
    @SerialName("idempotency_key")
    val idempotencyKey: String? = null,
    val currency: String,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("updated_at")
    val updatedAt: String? = null,
)
