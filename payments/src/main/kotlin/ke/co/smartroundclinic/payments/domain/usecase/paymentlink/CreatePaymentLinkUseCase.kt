package ke.co.smartroundclinic.payments.domain.usecase.paymentlink

import io.ktor.http.HttpStatusCode
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.infra.IntaSendConfig
import ke.co.smartroundclinic.payments.data.remote.dto.request.CreatePaymentLinkReq
import ke.co.smartroundclinic.payments.data.remote.dto.response.PaymentLinkRes
import ke.co.smartroundclinic.payments.domain.repository.IntaSendRepository
import ke.co.smartroundclinic.payments.presentation.dto.request.CreatePaymentLinkBody


class CreatePaymentLinkUseCase(
    private val repository: IntaSendRepository,
    private val config: IntaSendConfig,
) {
    suspend operator fun invoke(body: CreatePaymentLinkBody): DefaultResponse<PaymentLinkRes?> =
        repository.createPaymentLink(
            CreatePaymentLinkReq(
                title = "SmartRound Clinic Payment",
                isActive = body.isActive,
                redirectUrl = config.callbackUrl,
                amount = body.amount,
                usageLimit = 5,
                currency = body.currency,
                mobileTarrif = config.mobileTarrif,
                cardTarrif = config.cardTarrif,
            )
        ).toDefaultResponse(
            successStatusCode = HttpStatusCode.Created.value,
            failedStatusCode = HttpStatusCode.BadGateway.value,
        ) { it }
}
