package ke.co.smartroundclinic.payments.domain.service

import ke.co.smartroundclinic.payments.domain.usecase.payment.InitiateMpesaStkPushUseCase
import ke.co.smartroundclinic.payments.presentation.dto.request.MpesaPaymentBody

class IntaSendPaymentService(
    private val initiateMpesaUseCase: InitiateMpesaStkPushUseCase,
) {
    suspend fun payViaMpesa(body: MpesaPaymentBody) = initiateMpesaUseCase(body)
}
