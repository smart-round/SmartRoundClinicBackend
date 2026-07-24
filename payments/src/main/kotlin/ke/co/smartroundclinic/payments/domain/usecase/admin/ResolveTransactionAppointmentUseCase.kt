package ke.co.smartroundclinic.payments.domain.usecase.admin

import io.ktor.http.HttpStatusCode
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.infra.IntaSendConfig
import ke.co.smartroundclinic.payments.data.lookup.AppointmentInfoLookup
import ke.co.smartroundclinic.payments.data.lookup.AppointmentSummary
import ke.co.smartroundclinic.payments.data.remote.instasend.reseponse.WalletTransactionItem
import ke.co.smartroundclinic.payments.domain.repository.IntaSendRepository
import ke.co.smartroundclinic.payments.domain.repository.PaymentRepository
import ke.co.smartroundclinic.payments.domain.service.DoctorWalletResolver
import ke.co.smartroundclinic.payments.presentation.dto.response.AppointmentSummaryRes
import ke.co.smartroundclinic.payments.presentation.dto.response.TransactionAppointmentRes

private const val MAX_PAGES_SCANNED = 25

private val appointmentSuffixRegex = Regex("""\(APT-([0-9a-fA-F]{6})\)""")

/**
 * Resolves an IntaSend wallet transaction id (as seen in a wallet statement, e.g. "KQBOZ5O") back
 * to the appointment it belongs to, for admin support/debugging. IntaSend has no "get single wallet
 * transaction by id" endpoint, so this pages through the already-proven [IntaSendRepository.getWalletTransactions]
 * list until it finds a match. Once found:
 * - SALE transactions carry an `invoice` — resolved via [PaymentRepository.getByInvoiceId].
 * - Everything else (PAYOUT/RESERVE/CHARGE) has no invoice, but the narrative embeds the
 *   appointment id's last 6 hex chars as "(APT-xxxxxx)" (see CreditDoctorEarningsUseCase) — resolved
 *   via [AppointmentInfoLookup.findIdBySuffix].
 */
class ResolveTransactionAppointmentUseCase(
    private val intaSendRepository: IntaSendRepository,
    private val paymentRepository: PaymentRepository,
    private val appointmentInfoLookup: AppointmentInfoLookup,
    private val walletResolver: DoctorWalletResolver,
    private val config: IntaSendConfig,
) {
    suspend operator fun invoke(
        transactionId: String,
        wallet: String?,
        doctorId: String?,
    ): DefaultResponse<TransactionAppointmentRes?> {
        val walletId = when (wallet?.lowercase()) {
            null, "collections" -> config.collectionsWalletId
            "commission" -> config.commissionWalletId
            "doctor" -> {
                if (doctorId == null) {
                    return DefaultResponse(
                        httpStatusCode = HttpStatusCode.BadRequest.value,
                        status = false,
                        message = "doctorId is required when wallet=doctor",
                        data = null,
                    )
                }
                walletResolver.resolve(doctorId)
                    ?: return DefaultResponse(
                        httpStatusCode = HttpStatusCode.BadGateway.value,
                        status = false,
                        message = "Unable to reach this doctor's wallet right now. Please try again shortly.",
                        data = null,
                    )
            }
            else -> return DefaultResponse(
                httpStatusCode = HttpStatusCode.BadRequest.value,
                status = false,
                message = "wallet must be one of: collections, commission, doctor",
                data = null,
            )
        }

        val item = findTransaction(walletId, transactionId)
            ?: return DefaultResponse(
                httpStatusCode = HttpStatusCode.NotFound.value,
                status = false,
                message = "Transaction $transactionId not found in the ${wallet ?: "collections"} wallet's statement",
                data = null,
            )

        val invoiceId = item.invoice?.invoiceId
        var matchedVia: String? = null
        var appointmentId: String? = null
        var paymentId: String? = null

        if (item.transType == "SALE" && invoiceId != null) {
            val payment = (paymentRepository.getByInvoiceId(invoiceId) as? Resource.Success)?.data
            if (payment != null) {
                matchedVia = "invoice"
                appointmentId = payment.appointmentId
                paymentId = payment.id
            }
        }

        if (appointmentId == null) {
            val suffix = appointmentSuffixRegex.find(item.narrative ?: "")?.groupValues?.get(1)
            if (suffix != null) {
                appointmentInfoLookup.findIdBySuffix(suffix)?.let { id ->
                    matchedVia = "narrative"
                    appointmentId = id
                }
            }
        }

        if (paymentId == null && appointmentId != null) {
            paymentId = (paymentRepository.getByAppointmentId(appointmentId) as? Resource.Success)?.data?.id
        }

        val appointment = appointmentId?.let { appointmentInfoLookup.getSummary(it) }

        return DefaultResponse(
            httpStatusCode = HttpStatusCode.OK.value,
            status = true,
            message = if (appointment != null) "Appointment resolved successfully" else "Transaction found, but it isn't linked to an appointment",
            data = TransactionAppointmentRes(
                transactionId = item.transactionId,
                transType = item.transType,
                narrative = item.narrative,
                value = item.value,
                matchedVia = matchedVia,
                paymentId = paymentId,
                appointment = appointment?.toRes(),
            )
        )
    }

    private suspend fun findTransaction(walletId: String, transactionId: String): WalletTransactionItem? {
        var page = 1
        while (page <= MAX_PAGES_SCANNED) {
            val result = intaSendRepository.getWalletTransactions(walletId, page)
            val body = (result as? Resource.Success)?.data ?: return null
            body.results.firstOrNull { it.transactionId == transactionId }?.let { return it }
            if (body.next == null) return null
            page++
        }
        return null
    }

    private fun AppointmentSummary.toRes() = AppointmentSummaryRes(
        id = id,
        doctorId = doctorId,
        doctorName = doctorName,
        patientId = patientId,
        patientName = patientName,
        status = status,
        date = date,
        serviceTierId = serviceTierId,
        tierPrice = tierPrice,
        followUpFee = followUpFee,
    )
}
