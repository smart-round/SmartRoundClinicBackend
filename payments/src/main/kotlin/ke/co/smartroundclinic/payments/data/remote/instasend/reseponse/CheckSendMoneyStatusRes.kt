package ke.co.smartroundclinic.payments.data.remote.instasend.reseponse

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CheckSendMoneyStatusRes(
    @SerialName("actual_charges")
    val actualCharges: String,
    @SerialName("batch_reference")
    val batchReference: String?,
    @SerialName("charge_estimate")
    val chargeEstimate: String,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("failed_amount")
    val failedAmount: Double,
    @SerialName("file_id")
    val fileId: String,
    @SerialName("paid_amount")
    val paidAmount: String,
    @SerialName("status")
    val status: String,
    @SerialName("status_code")
    val statusCode: String,
    @SerialName("total_amount")
    val totalAmount: String,
    @SerialName("total_amount_estimate")
    val totalAmountEstimate: String,
    @SerialName("tracking_id")
    val trackingId: String,
    @SerialName("transactions")
    val transactions: List<CheckSendMoneyStatusTransaction>,
    @SerialName("transactions_count")
    val transactionsCount: Int,
    @SerialName("updated_at")
    val updatedAt: String,
    @SerialName("wallet")
    val wallet: CheckSendMoneyStatusWallet,
)

@Serializable
data class CheckSendMoneyStatusTransaction(
    @SerialName("account")
    val account: String,
    @SerialName("account_reference")
    val accountReference: String?,
    @SerialName("account_type")
    val accountType: String?,
    @SerialName("amount")
    val amount: String,
    @SerialName("bank_code")
    val bankCode: String,
    @SerialName("charge")
    val charge: String,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("currency")
    val currency: String,
    @SerialName("file_id")
    val fileId: String,
    @SerialName("idempotency_key")
    val idempotencyKey: String?,
    @SerialName("name")
    val name: String,
    @SerialName("narrative")
    val narrative: String?,
    @SerialName("provider")
    val provider: String,
    @SerialName("provider_account_name")
    val providerAccountName: String?,
    @SerialName("provider_reference")
    val providerReference: String?,
    @SerialName("request_reference_id")
    val requestReferenceId: String,
    @SerialName("status")
    val status: String,
    @SerialName("status_code")
    val statusCode: String,
    @SerialName("status_description")
    val statusDescription: String,
    @SerialName("transaction_id")
    val transactionId: String,
    @SerialName("updated_at")
    val updatedAt: String,
)

@Serializable
data class CheckSendMoneyStatusWallet(
    @SerialName("available_balance")
    val availableBalance: String,
    @SerialName("can_disburse")
    val canDisburse: Boolean,
    @SerialName("currency")
    val currency: String,
    @SerialName("current_balance")
    val currentBalance: String,
    @SerialName("label")
    val label: String,
    @SerialName("updated_at")
    val updatedAt: String,
    @SerialName("wallet_id")
    val walletId: String,
    @SerialName("wallet_type")
    val walletType: String,
)
