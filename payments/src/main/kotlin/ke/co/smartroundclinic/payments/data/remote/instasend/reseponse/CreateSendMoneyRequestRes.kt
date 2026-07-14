package ke.co.smartroundclinic.payments.data.remote.instasend.reseponse


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreateSendMoneyRequestRes(
    @SerialName("batch_reference")
    val batchReference: String?,
    @SerialName("charge_estimate")
    val chargeEstimate: Double,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("device_id")
    val deviceId: String?,
    @SerialName("file_id")
    val fileId: String,
    @SerialName("nonce")
    val nonce: String,
    @SerialName("status")
    val status: String,
    @SerialName("status_code")
    val statusCode: String,
    @SerialName("total_amount")
    val totalAmount: Double,
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

@Serializable
data class Transaction(
    @SerialName("account")
    val account: String,
    @SerialName("amount")
    val amount: Double,
    @SerialName("bank_code")
    val bankCode: String,
    @SerialName("id_number")
    val idNumber: String,
    @SerialName("idempotency_key")
    val idempotencyKey: String?,
    @SerialName("name")
    val name: String,
    @SerialName("narrative")
    val narrative: String?,
    @SerialName("request_reference_id")
    val requestReferenceId: String,
    @SerialName("status")
    val status: String,
    @SerialName("status_code")
    val statusCode: String,
    @SerialName("status_description")
    val statusDescription: String,
    @SerialName("transaction_id")
    val transactionId: String
)

@Serializable
data class Wallet(
    @SerialName("available_balance")
    val availableBalance: Double,
    @SerialName("can_disburse")
    val canDisburse: Boolean,
    @SerialName("currency")
    val currency: String,
    @SerialName("current_balance")
    val currentBalance: Double,
    @SerialName("label")
    val label: String,
    @SerialName("updated_at")
    val updatedAt: String,
    @SerialName("wallet_id")
    val walletId: String,
    @SerialName("wallet_type")
    val walletType: String
)
