package ke.co.smartroundclinic.payments.data.remote.dto.response

import kotlinx.serialization.Serializable

@Serializable
data class PaginatedPaymentLinksRes(
    val count: Int,
    val next: String? = null,
    val previous: String? = null,
    val results: List<PaymentLinkRes>,
)
