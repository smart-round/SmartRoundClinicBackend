package ke.co.smartroundclinic.payments.domain.usecase.paymentlink

import io.ktor.http.HttpStatusCode
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.infra.IntaSendConfig
import ke.co.smartroundclinic.payments.data.entity.PaymentEntity
import ke.co.smartroundclinic.payments.data.lookup.AppointmentInfoLookup
import ke.co.smartroundclinic.payments.data.remote.dto.request.CreatePaymentLinkReq
import ke.co.smartroundclinic.payments.data.remote.dto.response.PaymentLinkRes
import ke.co.smartroundclinic.payments.domain.repository.IntaSendRepository
import ke.co.smartroundclinic.payments.domain.repository.PaymentRepository
import ke.co.smartroundclinic.payments.presentation.dto.request.CreateAppointmentPaymentLinkBody
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlin.time.Clock

class CreateAppointmentPaymentLinkUseCase(
    private val repository: IntaSendRepository,
    private val paymentRepository: PaymentRepository,
    private val config: IntaSendConfig,
    private val lookup: AppointmentInfoLookup,
) {
    private val log = LoggerFactory.getLogger(CreateAppointmentPaymentLinkUseCase::class.java)

    suspend operator fun invoke(
        body: CreateAppointmentPaymentLinkBody,
        requestingPatientId: String,
    ): DefaultResponse<PaymentLinkRes?> {
        val participants = lookup.getParticipants(body.appointmentId)
            ?: return DefaultResponse(
                httpStatusCode = HttpStatusCode.NotFound.value,
                status = false,
                message = "Appointment not found",
                data = null,
            )

        if (participants.patientId != requestingPatientId) {
            return DefaultResponse(
                httpStatusCode = HttpStatusCode.Forbidden.value,
                status = false,
                message = "You are not authorized to create a payment link for this appointment",
                data = null,
            )
        }

        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH-mm"))
        val title = "Appointment with Dr ${participants.doctorName} - ${participants.patientName} $timestamp"
            .replace(Regex("[^a-zA-Z0-9_ \\-]"), " ")
            .replace(Regex(" {2,}"), " ")
            .trim()
            .take(140)

        val amount = participants.tierPrice.toInt()

        val result = repository.createPaymentLink(
            CreatePaymentLinkReq(
                id = UUID.randomUUID().toString(),
                apiRef = body.appointmentId,
                title = title,
                isActive = true,
                redirectUrl = config.callbackUrl,
                amount = amount,
                usageLimit = 5,
                currency = "KES",
                mobileTarrif = config.mobileTarrif,
                cardTarrif = config.cardTarrif,
            )
        )

        if (result is ke.co.smartroundclinic.common.Resource.Success) {
            runCatching {
                paymentRepository.save(
                    PaymentEntity(
                        id = UUID.randomUUID().toString(),
                        appointmentId = body.appointmentId,
                        patientId = participants.patientId,
                        doctorId = participants.doctorId,
                        amount = amount.toDouble(),
                        currency = "KES",
                        status = PaymentEntity.PaymentStatus.PENDING,
                        paymentMethod = "M-PESA",
                        transactionRef = result.data?.id,
                        createdAt = Clock.System.now().toString(),
                    )
                )
            }.onFailure { log.error("Failed to save payment entity for appointmentId=${body.appointmentId} — ${it.message}", it) }
        }

        return result.toDefaultResponse(
            successStatusCode = HttpStatusCode.Created.value,
            failedStatusCode = HttpStatusCode.BadGateway.value,
        ) { it }
    }
}
