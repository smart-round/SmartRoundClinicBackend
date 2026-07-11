package ke.co.smartroundclinic.payments.data.remote.instasend.reseponse

import ke.co.smartroundclinic.payments.data.remote.instasend.request.ApproveMpesaB2CRequestReq
import ke.co.smartroundclinic.payments.data.remote.instasend.request.ApproveMpesaB2CTransaction
import ke.co.smartroundclinic.payments.data.remote.instasend.request.ApproveMpesaB2CWallet
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreateMpesaB2CRequestRes(
    @SerialName("batch_reference")
    val batchReference: String? = null,
    @SerialName("charge_estimate")
    val chargeEstimate: Double = 0.0,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("file_id")
    val fileId: String? = null,
    @SerialName("nonce")
    val nonce: String? = null,
    @SerialName("status")
    val status: String? = null,
    @SerialName("status_code")
    val statusCode: String? = null,
    @SerialName("total_amount")
    val totalAmount: Double = 0.0,
    @SerialName("total_amount_estimate")
    val totalAmountEstimate: Double = 0.0,
    @SerialName("tracking_id")
    val trackingId: String,
    @SerialName("transactions")
    val transactions: List<MpesaB2CTransaction> = emptyList(),
    @SerialName("transactions_count")
    val transactionsCount: Int = 0,
    @SerialName("updated_at")
    val updatedAt: String? = null,
    @SerialName("wallet")
    val wallet: MpesaB2CWallet? = null,
)

@Serializable
data class MpesaB2CTransaction(
    @SerialName("account")
    val account: String,
    @SerialName("amount")
    val amount: Double,
    @SerialName("idempotency_key")
    val idempotencyKey: String? = null,
    @SerialName("name")
    val name: String? = null,
    @SerialName("narrative")
    val narrative: String? = null,
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
)

@Serializable
data class MpesaB2CWallet(
    @SerialName("available_balance")
    val availableBalance: Double = 0.0,
    @SerialName("can_disburse")
    val canDisburse: Boolean = false,
    @SerialName("currency")
    val currency: String = "KES",
    @SerialName("current_balance")
    val currentBalance: Double = 0.0,
    @SerialName("label")
    val label: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null,
    @SerialName("wallet_id")
    val walletId: String? = null,
    @SerialName("wallet_type")
    val walletType: String? = null,
)

fun CreateMpesaB2CRequestRes.toApproveMpesaB2CRequest(): ApproveMpesaB2CRequestReq = ApproveMpesaB2CRequestReq(
    trackingId = trackingId,
    transactions = transactions.map {
        ApproveMpesaB2CTransaction(
            amount = it.amount.toString(),
            account = it.account,
        )
    },
    wallet = wallet?.let {
        ApproveMpesaB2CWallet(
            availableBalance = it.availableBalance.toString(),
            currency = it.currency,
            label = it.label ?: "",
            walletType = it.walletType ?: "",
        )
    },
)
