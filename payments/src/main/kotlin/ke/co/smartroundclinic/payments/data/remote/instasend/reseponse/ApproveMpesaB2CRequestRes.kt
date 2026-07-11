package ke.co.smartroundclinic.payments.data.remote.instasend.reseponse

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ApproveMpesaB2CRequestRes(
    @SerialName("actual_charges")
    val actualCharges: String? = null,
    @SerialName("batch_reference")
    val batchReference: String? = null,
    @SerialName("charge_estimate")
    val chargeEstimate: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("failed_amount")
    val failedAmount: Double = 0.0,
    @SerialName("file_id")
    val fileId: String? = null,
    @SerialName("paid_amount")
    val paidAmount: String? = null,
    @SerialName("status")
    val status: String? = null,
    @SerialName("status_code")
    val statusCode: String? = null,
    @SerialName("total_amount")
    val totalAmount: String? = null,
    @SerialName("total_amount_estimate")
    val totalAmountEstimate: String? = null,
    @SerialName("tracking_id")
    val trackingId: String,
    @SerialName("transactions")
    val transactions: List<ApproveMpesaB2CTransactionRes> = emptyList(),
    @SerialName("transactions_count")
    val transactionsCount: Int = 0,
    @SerialName("updated_at")
    val updatedAt: String? = null,
)

@Serializable
data class ApproveMpesaB2CTransactionRes(
    @SerialName("account")
    val account: String? = null,
    @SerialName("amount")
    val amount: String? = null,
    @SerialName("charge")
    val charge: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("currency")
    val currency: String? = null,
    @SerialName("name")
    val name: String? = null,
    @SerialName("narrative")
    val narrative: String? = null,
    @SerialName("provider")
    val provider: String? = null,
    @SerialName("provider_reference")
    val providerReference: String? = null,
    @SerialName("request_reference_id")
    val requestReferenceId: String? = null,
    @SerialName("status")
    val status: String? = null,
    @SerialName("status_code")
    val statusCode: String? = null,
    @SerialName("status_description")
    val statusDescription: String? = null,
    @SerialName("transaction_id")
    val transactionId: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null,
)
