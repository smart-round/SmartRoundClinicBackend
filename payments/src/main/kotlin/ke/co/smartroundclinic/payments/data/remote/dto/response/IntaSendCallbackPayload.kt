package ke.co.smartroundclinic.payments.data.remote.dto.response

data class CardInfo(
    val binCountry: String? = null,
    val cardType: String? = null,
)

data class IntaSendCallbackPayload(
    val invoiceId: String? = null,
    val state: String? = null,
    val provider: String? = null,
    val charges: String? = null,
    val netAmount: String? = null,
    val currency: String? = null,
    val value: String? = null,
    val account: String? = null,
    val apiRef: String? = null,
    val providerRef: String? = null,
    val clearingStatus: String? = null,
    val mpesaReference: String? = null,
    val host: String? = null,
    val cardInfo: CardInfo? = null,
    val retryCount: Double? = null,
    val failedReason: String? = null,
    val failedCode: String? = null,
    val failedCodeLink: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val challenge: String? = null,
    val topic: String? = null,
)
