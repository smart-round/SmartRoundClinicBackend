package ke.co.smartroundclinic.payments.data.remote.instasend.reseponse


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreateSendMoneyRequestRes(
    @SerialName("batch_reference")
    val batchReference: Any?,
    @SerialName("charge_estimate")
    val chargeEstimate: Double,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("device_id")
    val deviceId: Any?,
    @SerialName("file_id")
    val fileId: String,
    @SerialName("nonce")
    val nonce: String,
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