package ke.co.smartroundclinic.payments.domain.usecase.withdrawal

import io.ktor.http.HttpStatusCode
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.infra.IntaSendConfig
import ke.co.smartroundclinic.payments.data.entity.CommissionPaymentEntry
import ke.co.smartroundclinic.payments.data.entity.PaymentEntity
import ke.co.smartroundclinic.payments.data.entity.PlatformCommissionLogEntity
import ke.co.smartroundclinic.payments.data.entity.WithdrawalEntity
import ke.co.smartroundclinic.payments.data.entity.WithdrawalTransactionRecord
import ke.co.smartroundclinic.payments.data.lookup.DoctorPaymentDetailsLookup
import ke.co.smartroundclinic.payments.data.remote.instasend.request.CreateSendMoneyRequestReq
import ke.co.smartroundclinic.payments.data.remote.instasend.request.CreateSendMoneyTransaction
import ke.co.smartroundclinic.payments.data.remote.instasend.reseponse.ApproveSendMoneyRequestRes
import ke.co.smartroundclinic.payments.data.remote.instasend.reseponse.toApproveSendMoneyRequest
import ke.co.smartroundclinic.payments.domain.repository.IntaSendRepository
import ke.co.smartroundclinic.payments.domain.repository.PaymentRepository
import ke.co.smartroundclinic.payments.domain.repository.PlatformCommissionLogRepository
import ke.co.smartroundclinic.payments.domain.repository.WithdrawalRepository
import ke.co.smartroundclinic.payments.presentation.dto.request.WithdrawInitiateReq
import org.slf4j.LoggerFactory

private const val MIN_WITHDRAWAL_KES = 100.0
private const val WITHDRAWAL_CURRENCY = "KES"
private const val WITHDRAWAL_PROVIDER = "PESALINK"

class WithdrawalUseCase(
    private val intaSendRepository: IntaSendRepository,
    private val withdrawalRepository: WithdrawalRepository,
    private val paymentRepository: PaymentRepository,
    private val commissionLogRepository: PlatformCommissionLogRepository,
    private val paymentDetailsLookup: DoctorPaymentDetailsLookup,
    private val config: IntaSendConfig,
) {
    private val log = LoggerFactory.getLogger(WithdrawalUseCase::class.java)

    suspend operator fun invoke(doctorId: String, req: WithdrawInitiateReq): DefaultResponse<ApproveSendMoneyRequestRes?> {
        // 1. Fetch doctor's registered bank account details
        val paymentDetails = paymentDetailsLookup.getByDoctorId(doctorId)
            ?: return DefaultResponse(
                httpStatusCode = HttpStatusCode.UnprocessableEntity.value,
                status = false,
                message = "No bank account details found. Please add your payment details before withdrawing.",
                data = null,
            )

        // 2. Compute gross, platform commission, and net earnings from completed payments
        val payments = (paymentRepository.getAllByDoctorId(doctorId) as? Resource.Success)?.data ?: emptyList()
        val completedPayments = payments.filter { it.status == PaymentEntity.PaymentStatus.COMPLETED }
        val totalGross = completedPayments.sumOf { it.amount }
        val totalPlatformCommission = completedPayments.sumOf { it.amount * (it.commissionRate / 100.0) }
        val totalNetEarnings = totalGross - totalPlatformCommission

        // 3. Compute total already withdrawn (PENDING + COMPLETED; exclude FAILED)
        val withdrawals = (withdrawalRepository.getByDoctorId(doctorId) as? Resource.Success)?.data ?: emptyList()
        val totalWithdrawn = withdrawals
            .filter { it.status != WithdrawalEntity.WithdrawalStatus.FAILED.name }
            .sumOf { it.amount }

        val availableBalance = totalNetEarnings - totalWithdrawn

        // 4. Minimum withdrawal limit
        if (req.amount < MIN_WITHDRAWAL_KES) {
            return DefaultResponse(
                httpStatusCode = HttpStatusCode.BadRequest.value,
                status = false,
                message = "Minimum withdrawal amount is KES $MIN_WITHDRAWAL_KES",
                data = null,
            )
        }

        // 5. Balance check
        if (req.amount > availableBalance) {
            return DefaultResponse(
                httpStatusCode = HttpStatusCode.BadRequest.value,
                status = false,
                message = "Insufficient balance. Available: KES ${"%.2f".format(availableBalance)}, requested: KES ${"%.2f".format(req.amount)}",
                data = null,
            )
        }

        // 6. Initiate disbursement with IntaSend using the doctor's registered bank details
        val initiateResult = intaSendRepository.createSendMoneyRequest(
            idNumber = req.idNumber,
            body = CreateSendMoneyRequestReq(
                callbackUrl = "${config.callBackBaseUrl}/payments/instasend/withdrawal/callback",
                currency = WITHDRAWAL_CURRENCY,
                provider = WITHDRAWAL_PROVIDER,
                transactions = listOf(
                    CreateSendMoneyTransaction(
                        account = paymentDetails.accountNumber,
                        amount = req.amount.toInt().toString(),
                        bankCode = paymentDetails.bankCode,
                        idNumber = req.idNumber,
                        name = paymentDetails.accountName,
                    )
                ),
            )
        )

        if (initiateResult !is Resource.Success) {
            return DefaultResponse(
                httpStatusCode = HttpStatusCode.BadGateway.value,
                status = false,
                message = (initiateResult as? Resource.Error)?.message ?: "Failed to initiate withdrawal with payment provider",
                data = null,
            )
        }

        val initiated = initiateResult.data ?: return DefaultResponse(
            httpStatusCode = HttpStatusCode.BadGateway.value,
            status = false,
            message = "Payment provider returned an empty response",
            data = null,
        )

        // 7. Auto-approve the disbursement
        val approveResult = intaSendRepository.approveSendMoneyRequest(initiated.toApproveSendMoneyRequest())

        // 8. Record the withdrawal regardless of approve outcome so failed ones are visible
        // Commission is proportional to the withdrawal amount, not the total gross.
        val commissionOnWithdrawal = if (totalNetEarnings > 0)
            (req.amount / totalNetEarnings) * totalPlatformCommission
        else 0.0

        val withdrawalSucceeded = approveResult is Resource.Success
        val withdrawalEntity = WithdrawalEntity(
            doctorId = doctorId,
            amount = req.amount,
            currency = WITHDRAWAL_CURRENCY,
            trackingId = initiated.trackingId,
            status = if (withdrawalSucceeded)
                WithdrawalEntity.WithdrawalStatus.PENDING.name
            else
                WithdrawalEntity.WithdrawalStatus.FAILED.name,
            provider = WITHDRAWAL_PROVIDER,
            platformCommission = commissionOnWithdrawal,
            transactions = listOf(
                WithdrawalTransactionRecord(
                    account = paymentDetails.accountNumber,
                    amount = req.amount.toInt().toString(),
                    bankCode = paymentDetails.bankCode,
                    name = paymentDetails.accountName,
                )
            ),
        )
        runCatching { withdrawalRepository.save(withdrawalEntity) }
            .onFailure { log.error("Failed to persist withdrawal record doctorId=$doctorId — ${it.message}", it) }

        // 9. Write platform commission log on success — per-payment breakdown for audit
        if (withdrawalSucceeded) {
            runCatching {
                commissionLogRepository.save(
                    PlatformCommissionLogEntity(
                        withdrawalId = withdrawalEntity.id,
                        doctorId = doctorId,
                        withdrawalAmount = req.amount,
                        totalGross = totalGross,
                        totalCommission = commissionOnWithdrawal,
                        payments = completedPayments.map { p ->
                            CommissionPaymentEntry(
                                paymentId = p.id,
                                appointmentId = p.appointmentId,
                                doctorId = p.doctorId,
                                amount = p.amount,
                                commissionRate = p.commissionRate,
                                commissionAmount = p.amount * (p.commissionRate / 100.0),
                                paidAt = p.createdAt,
                            )
                        },
                    )
                )
            }.onFailure { log.error("Failed to persist commission log withdrawalId=${withdrawalEntity.id} — ${it.message}", it) }
        }

        return approveResult.toDefaultResponse(
            successStatusCode = HttpStatusCode.Created.value,
            failedStatusCode = HttpStatusCode.BadGateway.value,
        ) { it }
    }
}
