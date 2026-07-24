package ke.co.smartroundclinic.payments.domain.usecase.admin

import io.ktor.http.HttpStatusCode
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.payments.data.entity.EarningsLedgerLeg
import ke.co.smartroundclinic.payments.domain.repository.EarningsLedgerRepository
import ke.co.smartroundclinic.payments.domain.repository.PaymentRepository
import ke.co.smartroundclinic.payments.presentation.dto.response.EarningsLedgerBackfillRes
import org.slf4j.LoggerFactory

/**
 * One-time, admin-triggered, idempotent backfill of the earnings ledger from payments that were
 * already credited before the ledger existed. Only ever writes a leg whose `xCreditedAt` is
 * already set on the payment — [ke.co.smartroundclinic.payments.domain.usecase.earnings.CreditDoctorEarningsUseCase]
 * only ever writes a leg that's still null — so this can never race with or overwrite a live credit,
 * and is safe to re-run (it always re-derives the same historical values from the same source rows).
 */
class BackfillEarningsLedgerUseCase(
    private val paymentRepository: PaymentRepository,
    private val earningsLedgerRepository: EarningsLedgerRepository,
) {
    private val log = LoggerFactory.getLogger(BackfillEarningsLedgerUseCase::class.java)

    suspend operator fun invoke(): DefaultResponse<EarningsLedgerBackfillRes?> {
        val payments = (paymentRepository.getAllForAdmin() as? Resource.Success)?.data ?: emptyList()

        var doctorLegsWritten = 0
        var commissionLegsWritten = 0
        var failures = 0

        for (payment in payments) {
            val netAmount = payment.amount * (1.0 - payment.commissionRate / 100.0)
            val commissionAmount = payment.amount - netAmount

            payment.doctorCreditedAt?.let { creditedAt ->
                val result = earningsLedgerRepository.backfillLegCredited(
                    leg = EarningsLedgerLeg.DOCTOR,
                    paymentId = payment.id,
                    appointmentId = payment.appointmentId,
                    doctorId = payment.doctorId,
                    grossAmount = payment.amount,
                    commissionRate = payment.commissionRate,
                    commissionAmount = commissionAmount,
                    netAmount = netAmount,
                    creditedAt = creditedAt,
                )
                if (result is Resource.Success) doctorLegsWritten++ else {
                    failures++
                    log.error("Backfill failed doctor leg paymentId=${payment.id} — ${(result as? Resource.Error)?.message}")
                }
            }

            payment.commissionCreditedAt?.let { creditedAt ->
                val result = earningsLedgerRepository.backfillLegCredited(
                    leg = EarningsLedgerLeg.COMMISSION,
                    paymentId = payment.id,
                    appointmentId = payment.appointmentId,
                    doctorId = payment.doctorId,
                    grossAmount = payment.amount,
                    commissionRate = payment.commissionRate,
                    commissionAmount = commissionAmount,
                    netAmount = netAmount,
                    creditedAt = creditedAt,
                )
                if (result is Resource.Success) commissionLegsWritten++ else {
                    failures++
                    log.error("Backfill failed commission leg paymentId=${payment.id} — ${(result as? Resource.Error)?.message}")
                }
            }
        }

        log.info("Earnings ledger backfill complete — paymentsScanned=${payments.size} doctorLegs=$doctorLegsWritten commissionLegs=$commissionLegsWritten failures=$failures")

        return DefaultResponse(
            httpStatusCode = HttpStatusCode.OK.value,
            status = true,
            message = "Earnings ledger backfill complete",
            data = EarningsLedgerBackfillRes(
                paymentsScanned = payments.size,
                doctorLegsWritten = doctorLegsWritten,
                commissionLegsWritten = commissionLegsWritten,
                failures = failures,
            )
        )
    }
}
