package ke.co.smartroundclinic.payments.domain.service

import ke.co.smartroundclinic.payments.data.remote.dto.response.ChargebackWebhookPayload
import ke.co.smartroundclinic.payments.data.remote.dto.response.IntaSendCallbackPayload
import ke.co.smartroundclinic.payments.data.remote.dto.response.WithdrawalWebhookPayload
import ke.co.smartroundclinic.payments.domain.usecase.payment.HandleIntaSendWebhookUseCase
import ke.co.smartroundclinic.payments.domain.usecase.refund.HandleRefundWebhookUseCase
import ke.co.smartroundclinic.payments.domain.usecase.stkpush.GetStkPushPaymentStatusUseCase
import ke.co.smartroundclinic.payments.domain.usecase.stkpush.InitiateStkPushAppointmentUseCase
import ke.co.smartroundclinic.payments.domain.usecase.stkpush.InitiateStkPushPreBookingUseCase
import ke.co.smartroundclinic.payments.domain.usecase.wallet.GetWalletTransactionsUseCase
import ke.co.smartroundclinic.payments.domain.usecase.withdrawal.CheckWithdrawalStatusUseCase
import ke.co.smartroundclinic.payments.domain.usecase.withdrawal.GetWithdrawalBalanceUseCase
import ke.co.smartroundclinic.payments.domain.usecase.withdrawal.GetWithdrawalByIdUseCase
import ke.co.smartroundclinic.payments.domain.usecase.withdrawal.GetWithdrawalHistoryUseCase
import ke.co.smartroundclinic.payments.domain.usecase.withdrawal.HandleWithdrawalWebhookUseCase
import ke.co.smartroundclinic.payments.domain.usecase.withdrawal.WithdrawalUseCase
import ke.co.smartroundclinic.payments.presentation.dto.request.WithdrawInitiateReq

class IntaSendService(
    private val handleWebhookUseCase: HandleIntaSendWebhookUseCase,
    private val withdrawalUseCase: WithdrawalUseCase,
    private val getWithdrawalBalanceUseCase: GetWithdrawalBalanceUseCase,
    private val checkWithdrawalStatusUseCase: CheckWithdrawalStatusUseCase,
    private val handleWithdrawalWebhookUseCase: HandleWithdrawalWebhookUseCase,
    private val getWithdrawalHistoryUseCase: GetWithdrawalHistoryUseCase,
    private val getWithdrawalByIdUseCase: GetWithdrawalByIdUseCase,
    private val getWalletTransactionsUseCase: GetWalletTransactionsUseCase,
    private val stkPushAppointmentUseCase: InitiateStkPushAppointmentUseCase,
    private val stkPushPreBookingUseCase: InitiateStkPushPreBookingUseCase,
    private val getStkPushStatusUseCase: GetStkPushPaymentStatusUseCase,
    private val handleRefundWebhookUseCase: HandleRefundWebhookUseCase,
) {
    suspend fun handleWebhook(payload: IntaSendCallbackPayload) = handleWebhookUseCase(payload)
    suspend fun withdraw(doctorId: String, req: WithdrawInitiateReq) = withdrawalUseCase(doctorId, req)
    suspend fun getWithdrawalBalance(doctorId: String) = getWithdrawalBalanceUseCase(doctorId)
    suspend fun checkWithdrawalStatus(trackingId: String) = checkWithdrawalStatusUseCase(trackingId)
    suspend fun handleWithdrawalWebhook(payload: WithdrawalWebhookPayload) = handleWithdrawalWebhookUseCase(payload)
    suspend fun handleRefundWebhook(payload: ChargebackWebhookPayload) = handleRefundWebhookUseCase(payload)
    suspend fun getWithdrawalHistory(doctorId: String, page: Int) =
        getWithdrawalHistoryUseCase(doctorId, page)
    suspend fun getWithdrawalById(id: String, doctorId: String) =
        getWithdrawalByIdUseCase(id, doctorId)
    suspend fun getWalletTransactions(doctorId: String, page: Int) =
        getWalletTransactionsUseCase(doctorId, page)

    suspend fun stkPushForAppointment(appointmentId: String, phoneNumber: String, patientId: String) =
        stkPushAppointmentUseCase(appointmentId, phoneNumber, patientId)

    suspend fun stkPushPreBooking(
        doctorId: String,
        patientId: String,
        phoneNumber: String,
        isRebooking: Boolean,
        previousAppointmentId: String?,
    ) = stkPushPreBookingUseCase(doctorId, patientId, phoneNumber, isRebooking, previousAppointmentId)

    suspend fun getStkPushStatus(invoiceId: String) = getStkPushStatusUseCase(invoiceId)
}
