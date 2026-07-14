package ke.co.smartroundclinic.payments.domain.usecase.stkpush

import io.ktor.http.HttpStatusCode
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.infra.IntaSendConfig
import ke.co.smartroundclinic.payments.data.entity.PaymentEntity
import ke.co.smartroundclinic.payments.data.lookup.AppointmentInfoLookup
import ke.co.smartroundclinic.payments.data.lookup.DoctorTierPriceLookup
import ke.co.smartroundclinic.payments.data.remote.instasend.computeStkPushSignature
import ke.co.smartroundclinic.payments.data.remote.instasend.request.STKPushReq
import ke.co.smartroundclinic.payments.domain.repository.IntaSendRepository
import ke.co.smartroundclinic.payments.domain.repository.PaymentRepository
import ke.co.smartroundclinic.payments.presentation.dto.response.StkPushInitiationRes
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.UUID
import kotlin.time.Clock

private const val COMPLETED = "COMPLETED"

class InitiateStkPushPreBookingUseCase(
    private val intaSendRepository: IntaSendRepository,
    private val paymentRepository: PaymentRepository,
    private val config: IntaSendConfig,
    private val doctorTierPriceLookup: DoctorTierPriceLookup,
    private val appointmentInfoLookup: AppointmentInfoLookup,
) {
    private val log = LoggerFactory.getLogger(InitiateStkPushPreBookingUseCase::class.java)

    suspend operator fun invoke(
        doctorId: String,
        patientId: String,
        phoneNumber: String,
        isRebooking: Boolean,
        previousAppointmentId: String?,
    ): DefaultResponse<StkPushInitiationRes?> {

        val amount: Double
        val apiRef: String

        if (isRebooking) {
            if (previousAppointmentId.isNullOrBlank()) return DefaultResponse(
                httpStatusCode = HttpStatusCode.BadRequest.value,
                status = false,
                message = "previousAppointmentId is required for rebooking",
                data = null,
            )

            val prevAppt = appointmentInfoLookup.getForRebooking(previousAppointmentId)
                ?: return DefaultResponse(
                    httpStatusCode = HttpStatusCode.NotFound.value,
                    status = false,
                    message = "Previous appointment not found",
                    data = null,
                )

            if (prevAppt.patientId != patientId) return DefaultResponse(
                httpStatusCode = HttpStatusCode.Forbidden.value,
                status = false,
                message = "Previous appointment does not belong to this patient",
                data = null,
            )

            if (prevAppt.doctorId != doctorId) return DefaultResponse(
                httpStatusCode = HttpStatusCode.BadRequest.value,
                status = false,
                message = "Rebooking must be with the same doctor as the previous appointment",
                data = null,
            )

            if (prevAppt.status != COMPLETED) return DefaultResponse(
                httpStatusCode = HttpStatusCode.UnprocessableEntity.value,
                status = false,
                message = "Rebooking is only allowed for completed appointments. Previous appointment status: ${prevAppt.status}",
                data = null,
            )

            val prevDate = runCatching { LocalDate.parse(prevAppt.date) }.getOrNull()
                ?: return DefaultResponse(
                    httpStatusCode = HttpStatusCode.UnprocessableEntity.value,
                    status = false,
                    message = "Previous appointment has an invalid date",
                    data = null,
                )

            if (ChronoUnit.DAYS.between(prevDate, LocalDate.now()) > 30) return DefaultResponse(
                httpStatusCode = HttpStatusCode.UnprocessableEntity.value,
                status = false,
                message = "Rebooking is only allowed within 30 days of the previous appointment",
                data = null,
            )

            amount = prevAppt.followUpFee
            apiRef = "rebooking_${patientId}_${doctorId}_${UUID.randomUUID().toString().take(8)}"
        } else {
            val tierInfo = doctorTierPriceLookup.getTierInfo(doctorId)
                ?: return DefaultResponse(
                    httpStatusCode = HttpStatusCode.UnprocessableEntity.value,
                    status = false,
                    message = "Doctor is not yet configured for appointments. Please contact support.",
                    data = null,
                )
            amount = tierInfo.tierPrice
            apiRef = "prebooking_${patientId}_${doctorId}_${UUID.randomUUID().toString().take(8)}"
        }

        val commissionRate = doctorTierPriceLookup.getTierInfo(doctorId)?.commissionRate ?: 0.0
        val amountStr = amount.toInt().toString()

        val result = intaSendRepository.stkPush(
            STKPushReq(
                amount = amountStr,
                apiRef = apiRef,
                phoneNumber = phoneNumber,
                signature = computeStkPushSignature(config.secretKey, phoneNumber, amountStr, apiRef),
                walletId = config.collectionsWalletId,
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
                        amount = amount,
                        currency = "KES",
                        status = PaymentEntity.PaymentStatus.PENDING,
                        paymentMethod = "STK_PUSH",
                        transactionRef = apiRef,
                        invoiceId = result.data?.invoice?.invoiceId,
                        commissionRate = commissionRate,
                        createdAt = Clock.System.now().toString(),
                    )
                )
            }.onFailure {
                log.error(
                    "Failed to save STK push ${if (isRebooking) "rebooking" else "pre-booking"} payment " +
                    "for doctorId=$doctorId patientId=$patientId — ${it.message}", it
                )
            }
        }

        return result.toDefaultResponse(
            successStatusCode = HttpStatusCode.Created.value,
            failedStatusCode = HttpStatusCode.BadGateway.value,
        ) { res ->
            res?.let {
                StkPushInitiationRes(
                    invoiceId = it.invoice.invoiceId,
                    transactionRef = apiRef,
                    state = it.invoice.state,
                    amount = amount,
                    currency = "KES",
                )
            }
        }
    }
}
