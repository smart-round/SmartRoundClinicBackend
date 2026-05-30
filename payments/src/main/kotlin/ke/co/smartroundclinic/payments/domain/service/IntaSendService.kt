package ke.co.smartroundclinic.payments.domain.service

import ke.co.smartroundclinic.payments.data.remote.dto.response.IntaSendCallbackPayload
import ke.co.smartroundclinic.payments.domain.usecase.payment.HandleIntaSendWebhookUseCase
import ke.co.smartroundclinic.payments.domain.usecase.paymentlink.CreateAppointmentPaymentLinkUseCase
import ke.co.smartroundclinic.payments.domain.usecase.paymentlink.CreatePaymentLinkUseCase
import ke.co.smartroundclinic.payments.domain.usecase.paymentlink.GetPaymentLinkUseCase
import ke.co.smartroundclinic.payments.domain.usecase.paymentlink.ListPaymentLinksUseCase
import ke.co.smartroundclinic.payments.domain.usecase.paymentlink.UpdatePaymentLinkUseCase
import ke.co.smartroundclinic.payments.presentation.dto.request.CreateAppointmentPaymentLinkBody
import ke.co.smartroundclinic.payments.presentation.dto.request.CreatePaymentLinkBody
import ke.co.smartroundclinic.payments.presentation.dto.request.UpdatePaymentLinkBody

class IntaSendService(
    private val createUseCase: CreatePaymentLinkUseCase,
    private val listUseCase: ListPaymentLinksUseCase,
    private val getUseCase: GetPaymentLinkUseCase,
    private val updateUseCase: UpdatePaymentLinkUseCase,
    private val createForAppointmentUseCase: CreateAppointmentPaymentLinkUseCase,
    private val handleWebhookUseCase: HandleIntaSendWebhookUseCase,
) {
    suspend fun create(body: CreatePaymentLinkBody) = createUseCase(body)
    suspend fun list(page: Int) = listUseCase(page)
    suspend fun get(id: String) = getUseCase(id)
    suspend fun update(id: String, body: UpdatePaymentLinkBody) = updateUseCase(id, body)
    suspend fun createForAppointment(body: CreateAppointmentPaymentLinkBody, patientId: String) =
        createForAppointmentUseCase(body, patientId)
    suspend fun handleWebhook(payload: IntaSendCallbackPayload) = handleWebhookUseCase(payload)
}
