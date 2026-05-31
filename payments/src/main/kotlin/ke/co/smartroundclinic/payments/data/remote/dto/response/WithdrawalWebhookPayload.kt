package ke.co.smartroundclinic.payments.data.remote.dto.response

data class WithdrawalWebhookTransaction(
    val transactionId: String? = null,
    val status: String? = null,
    val statusCode: String? = null,
    val statusDescription: String? = null,
    val provider: String? = null,
    val bankCode: String? = null,
    val name: String? = null,
    val account: String? = null,
    val amount: String? = null,
    val charge: String? = null,
    val currency: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

data class WithdrawalWebhookPayload(
    val fileId: String? = null,
    val trackingId: String? = null,
    val batchReference: String? = null,
    val status: String? = null,
    val statusCode: String? = null,
    val transactions: List<WithdrawalWebhookTransaction>? = null,
    val actualCharges: String? = null,
    val paidAmount: String? = null,
    val failedAmount: Double? = null,
    val chargeEstimate: String? = null,
    val totalAmountEstimate: String? = null,
    val totalAmount: String? = null,
    val transactionsCount: Int? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val challenge: String? = null,
    val topic: String? = null,
)
