package ke.co.smartroundclinic.payments.domain.service

import ke.co.smartroundclinic.payments.data.remote.dto.response.IntaSendCallbackPayload
import ke.co.smartroundclinic.payments.data.remote.dto.response.WithdrawalWebhookPayload
import ke.co.smartroundclinic.payments.domain.usecase.payment.HandleIntaSendWebhookUseCase
import ke.co.smartroundclinic.payments.domain.usecase.paymentlink.CreateAppointmentPaymentLinkUseCase
import ke.co.smartroundclinic.payments.domain.usecase.paymentlink.CreatePaymentLinkUseCase
import ke.co.smartroundclinic.payments.domain.usecase.paymentlink.CreatePreBookingPaymentLinkUseCase
import ke.co.smartroundclinic.payments.domain.usecase.paymentlink.GetPaymentLinkUseCase
import ke.co.smartroundclinic.payments.domain.usecase.paymentlink.ListPaymentLinksUseCase
import ke.co.smartroundclinic.payments.domain.usecase.paymentlink.UpdatePaymentLinkUseCase
import ke.co.smartroundclinic.payments.domain.usecase.withdrawal.CheckWithdrawalStatusUseCase
import ke.co.smartroundclinic.payments.domain.usecase.withdrawal.GetWithdrawalBalanceUseCase
import ke.co.smartroundclinic.payments.domain.usecase.withdrawal.HandleWithdrawalWebhookUseCase
import ke.co.smartroundclinic.payments.domain.usecase.withdrawal.WithdrawalUseCase
import ke.co.smartroundclinic.payments.presentation.dto.request.CreateAppointmentPaymentLinkBody
import ke.co.smartroundclinic.payments.presentation.dto.request.CreatePaymentLinkBody
import ke.co.smartroundclinic.payments.presentation.dto.request.UpdatePaymentLinkBody
import ke.co.smartroundclinic.payments.presentation.dto.request.WithdrawInitiateReq

class IntaSendService(
    private val createUseCase: CreatePaymentLinkUseCase,
    private val listUseCase: ListPaymentLinksUseCase,
    private val getUseCase: GetPaymentLinkUseCase,
    private val updateUseCase: UpdatePaymentLinkUseCase,
    private val createForAppointmentUseCase: CreateAppointmentPaymentLinkUseCase,
    private val createPreBookingUseCase: CreatePreBookingPaymentLinkUseCase,
    private val handleWebhookUseCase: HandleIntaSendWebhookUseCase,
    private val withdrawalUseCase: WithdrawalUseCase,
    private val getWithdrawalBalanceUseCase: GetWithdrawalBalanceUseCase,
    private val checkWithdrawalStatusUseCase: CheckWithdrawalStatusUseCase,
    private val handleWithdrawalWebhookUseCase: HandleWithdrawalWebhookUseCase,
) {
    suspend fun create(body: CreatePaymentLinkBody) = createUseCase(body)
    suspend fun list(page: Int) = listUseCase(page)
    suspend fun get(id: String) = getUseCase(id)
    suspend fun update(id: String, body: UpdatePaymentLinkBody) = updateUseCase(id, body)
    suspend fun createForAppointment(body: CreateAppointmentPaymentLinkBody, patientId: String) =
        createForAppointmentUseCase(body, patientId)
    suspend fun createPreBookingLink(doctorId: String, patientId: String) =
        createPreBookingUseCase(doctorId, patientId)
    suspend fun handleWebhook(payload: IntaSendCallbackPayload) = handleWebhookUseCase(payload)
    suspend fun withdraw(doctorId: String, req: WithdrawInitiateReq) = withdrawalUseCase(doctorId, req)
    suspend fun getWithdrawalBalance(doctorId: String) = getWithdrawalBalanceUseCase(doctorId)
    suspend fun checkWithdrawalStatus(trackingId: String) = checkWithdrawalStatusUseCase(trackingId)
    suspend fun handleWithdrawalWebhook(payload: WithdrawalWebhookPayload) = handleWithdrawalWebhookUseCase(payload)
}
