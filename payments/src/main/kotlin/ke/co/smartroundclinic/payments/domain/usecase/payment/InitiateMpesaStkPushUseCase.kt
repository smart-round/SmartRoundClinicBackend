package ke.co.smartroundclinic.payments.domain.usecase.payment

import io.ktor.http.HttpStatusCode
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.infra.IntaSendConfig
import ke.co.smartroundclinic.payments.data.remote.dto.request.MpesaStkPushReq
import ke.co.smartroundclinic.payments.data.remote.dto.response.PaymentSessionRes
import ke.co.smartroundclinic.payments.domain.repository.IntaSendPaymentRepository
import ke.co.smartroundclinic.payments.presentation.dto.request.MpesaPaymentBody

class InitiateMpesaStkPushUseCase(
    private val repository: IntaSendPaymentRepository,
    private val config: IntaSendConfig,
) {
    suspend operator fun invoke(body: MpesaPaymentBody): DefaultResponse<PaymentSessionRes?> =
        repository.initiateMpesaStkPush(
            MpesaStkPushReq(
                amount = body.amount,
                phoneNumber = body.phoneNumber,
                apiRef = body.appointmentId,
                mobileTarrif = config.mobileTarrif,
            )
        ).toDefaultResponse(
            failedStatusCode = HttpStatusCode.BadGateway.value,
        ) { it }
}
