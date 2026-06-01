package ke.co.smartroundclinic.payments.domain.usecase.paymentlink

import io.ktor.http.HttpStatusCode
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.infra.IntaSendConfig
import ke.co.smartroundclinic.payments.data.entity.PaymentEntity
import ke.co.smartroundclinic.payments.data.lookup.AppointmentInfoLookup
import ke.co.smartroundclinic.payments.data.lookup.DoctorTierPriceLookup
import ke.co.smartroundclinic.payments.data.remote.dto.request.CreatePaymentLinkReq
import ke.co.smartroundclinic.payments.data.remote.dto.response.PaymentLinkRes
import ke.co.smartroundclinic.payments.domain.repository.IntaSendRepository
import ke.co.smartroundclinic.payments.domain.repository.PaymentRepository
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.UUID
import kotlin.time.Clock

private const val COMPLETED = "COMPLETED"

class CreatePreBookingPaymentLinkUseCase(
    private val intaSendRepository: IntaSendRepository,
    private val paymentRepository: PaymentRepository,
    private val config: IntaSendConfig,
    private val doctorTierPriceLookup: DoctorTierPriceLookup,
    private val appointmentInfoLookup: AppointmentInfoLookup,
) {
    private val log = LoggerFactory.getLogger(CreatePreBookingPaymentLinkUseCase::class.java)

    suspend operator fun invoke(
        doctorId: String,
        patientId: String,
        isRebooking: Boolean,
        previousAppointmentId: String?,
    ): DefaultResponse<PaymentLinkRes?> {

        val amount: Int
        val title: String
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH-mm"))

        if (isRebooking) {
            // ── Rebooking path ────────────────────────────────────────────────
            if (previousAppointmentId.isNullOrBlank()) {
                return DefaultResponse(
                    httpStatusCode = HttpStatusCode.BadRequest.value,
                    status = false,
                    message = "previousAppointmentId is required for rebooking",
                    data = null,
                )
            }

            val prevAppt = appointmentInfoLookup.getForRebooking(previousAppointmentId)
                ?: return DefaultResponse(
                    httpStatusCode = HttpStatusCode.NotFound.value,
                    status = false,
                    message = "Previous appointment not found",
                    data = null,
                )

            if (prevAppt.patientId != patientId) {
                return DefaultResponse(
                    httpStatusCode = HttpStatusCode.Forbidden.value,
                    status = false,
                    message = "Previous appointment does not belong to this patient",
                    data = null,
                )
            }

            if (prevAppt.doctorId != doctorId) {
                return DefaultResponse(
                    httpStatusCode = HttpStatusCode.BadRequest.value,
                    status = false,
                    message = "Rebooking must be with the same doctor as the previous appointment",
                    data = null,
                )
            }

            if (prevAppt.status != COMPLETED) {
                return DefaultResponse(
                    httpStatusCode = HttpStatusCode.UnprocessableEntity.value,
                    status = false,
                    message = "Rebooking is only allowed for completed appointments. Previous appointment status: ${prevAppt.status}",
                    data = null,
                )
            }

            val prevDate = runCatching { LocalDate.parse(prevAppt.date) }.getOrNull()
                ?: return DefaultResponse(
                    httpStatusCode = HttpStatusCode.UnprocessableEntity.value,
                    status = false,
                    message = "Previous appointment has an invalid date",
                    data = null,
                )

            val today = LocalDate.now()
            val daysSince = ChronoUnit.DAYS.between(prevDate, today)
            if (daysSince > 30) {
                return DefaultResponse(
                    httpStatusCode = HttpStatusCode.UnprocessableEntity.value,
                    status = false,
                    message = "Rebooking is only allowed within 30 days of the previous appointment",
                    data = null,
                )
            }

            amount = prevAppt.followUpFee.toInt()
            title = "Follow-Up Consultation $timestamp"

        } else {
            // ── Standard pre-booking path ─────────────────────────────────────
            val tierInfo = doctorTierPriceLookup.getTierInfo(doctorId)
                ?: return DefaultResponse(
                    httpStatusCode = HttpStatusCode.UnprocessableEntity.value,
                    status = false,
                    message = "Doctor is not yet configured for appointments. Please contact support.",
                    data = null,
                )
            amount = tierInfo.tierPrice.toInt()
            title = "Consultation Payment $timestamp"
        }

        val tierInfo = doctorTierPriceLookup.getTierInfo(doctorId)
        val commissionRate = tierInfo?.commissionRate ?: 0.0

        val result = intaSendRepository.createPaymentLink(
            CreatePaymentLinkReq(
                id = UUID.randomUUID().toString(),
                apiRef = if (isRebooking) "rebooking_${patientId}_${doctorId}" else "prebooking_${patientId}_${doctorId}",
                title = title,
                isActive = true,
                redirectUrl = config.callbackPaymentsUrl,
                amount = amount,
                usageLimit = 10,
                currency = "KES",
                mobileTarrif = config.mobileTarrif,
                cardTarrif = config.cardTarrif,
            )
        )

        if (result is Resource.Success) {
            runCatching {
                paymentRepository.save(
                    PaymentEntity(
                        id = UUID.randomUUID().toString(),
                        appointmentId = null,
                        patientId = patientId,
                        doctorId = doctorId,
                        amount = amount.toDouble(),
                        currency = "KES",
                        status = PaymentEntity.PaymentStatus.PENDING,
                        paymentMethod = "M-PESA",
                        transactionRef = result.data?.id,
                        commissionRate = commissionRate,
                        createdAt = Clock.System.now().toString(),
                    )
                )
            }.onFailure {
                log.error(
                    "Failed to save ${if (isRebooking) "rebooking" else "pre-booking"} payment entity " +
                    "for doctorId=$doctorId patientId=$patientId — ${it.message}", it
                )
            }
        }

        return result.toDefaultResponse(
            successStatusCode = HttpStatusCode.Created.value,
            failedStatusCode = HttpStatusCode.BadGateway.value,
        ) { it }
    }
}
