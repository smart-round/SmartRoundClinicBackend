package ke.co.smartroundclinic.payments.data.remote.instasend.reseponse


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ApproveSendMoneyRequestRes(
    @SerialName("actual_charges")
    val actualCharges: String,
    @SerialName("batch_reference")
    val batchReference: Any?,
    @SerialName("charge_estimate")
    val chargeEstimate: Double,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("failed_amount")
    val failedAmount: Int,
    @SerialName("file_id")
    val fileId: String,
    @SerialName("paid_amount")
    val paidAmount: String,
    @SerialName("status")
    val status: String,
    @SerialName("status_code")
    val statusCode: String,
    @SerialName("total_amount")
    val totalAmount: Int,
    @SerialName("total_amount_estimate")
    val totalAmountEstimate: Double,
    @SerialName("tracking_id")
    val trackingId: String,
    @SerialName("transactions")
    val transactions: List<Transaction>,
    @SerialName("transactions_count")
    val transactionsCount: Int,
    @SerialName("updated_at")
    val updatedAt: String,
    @SerialName("wallet")
    val wallet: Wallet
)