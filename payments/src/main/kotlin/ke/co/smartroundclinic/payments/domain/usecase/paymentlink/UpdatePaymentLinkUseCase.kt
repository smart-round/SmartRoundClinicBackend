package ke.co.smartroundclinic.payments.domain.usecase.paymentlink

import io.ktor.http.HttpStatusCode
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.infra.IntaSendConfig
import ke.co.smartroundclinic.payments.data.remote.dto.request.UpdatePaymentLinkReq
import ke.co.smartroundclinic.payments.data.remote.dto.response.PaymentLinkRes
import ke.co.smartroundclinic.payments.domain.repository.IntaSendRepository
import ke.co.smartroundclinic.payments.presentation.dto.request.UpdatePaymentLinkBody

class UpdatePaymentLinkUseCase(
    private val repository: IntaSendRepository,
    private val config: IntaSendConfig,
) {
    suspend operator fun invoke(id: String, body: UpdatePaymentLinkBody): DefaultResponse<PaymentLinkRes?> =
        repository.updatePaymentLink(
            id,
            UpdatePaymentLinkReq(
                title = body.title,
                isActive = body.isActive,
                redirectUrl = config.callbackUrl,
                amount = body.amount?.toInt(),
                usageLimit = body.usageLimit,
                currency = body.currency,
                mobileTarrif = config.mobileTarrif,
                cardTarrif = config.cardTarrif,
            )
        ).toDefaultResponse(
            failedStatusCode = HttpStatusCode.BadGateway.value,
        ) { it }
}
