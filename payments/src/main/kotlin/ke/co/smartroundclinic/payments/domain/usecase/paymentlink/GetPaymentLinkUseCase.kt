package ke.co.smartroundclinic.payments.domain.usecase.paymentlink

import io.ktor.http.HttpStatusCode
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.payments.data.remote.dto.response.PaymentLinkRes
import ke.co.smartroundclinic.payments.domain.repository.IntaSendRepository

class GetPaymentLinkUseCase(
    private val repository: IntaSendRepository,
) {
    suspend operator fun invoke(id: String): DefaultResponse<PaymentLinkRes?> =
        repository.getPaymentLink(id).toDefaultResponse(
            failedStatusCode = HttpStatusCode.BadGateway.value,
        ) { it }
}
