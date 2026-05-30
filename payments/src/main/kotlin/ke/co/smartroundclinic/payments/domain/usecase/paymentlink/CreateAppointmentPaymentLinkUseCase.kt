package ke.co.smartroundclinic.payments.domain.usecase.paymentlink

import io.ktor.http.HttpStatusCode
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.infra.IntaSendConfig
import ke.co.smartroundclinic.payments.data.lookup.AppointmentInfoLookup
import ke.co.smartroundclinic.payments.data.remote.dto.request.CreatePaymentLinkReq
import ke.co.smartroundclinic.payments.data.remote.dto.response.PaymentLinkRes
import ke.co.smartroundclinic.payments.domain.repository.IntaSendRepository
import ke.co.smartroundclinic.payments.presentation.dto.request.CreateAppointmentPaymentLinkBody
import java.util.UUID

class CreateAppointmentPaymentLinkUseCase(
    private val repository: IntaSendRepository,
    private val config: IntaSendConfig,
    private val lookup: AppointmentInfoLookup,
) {
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

        val title = "Appointment with Dr ${participants.doctorName} - ${participants.patientName}"
            .replace(Regex("[^a-zA-Z0-9_ \\-]"), " ")
            .replace(Regex(" {2,}"), " ")
            .trim()
            .take(140)

        return repository.createPaymentLink(
            CreatePaymentLinkReq(
                id = UUID.randomUUID().toString(),
                title = title,
                isActive = true,
                redirectUrl = config.callbackUrl,
                amount = participants.followUpFee,
                usageLimit = 5,
                currency = "KES",
                mobileTarrif = config.mobileTarrif,
                cardTarrif = config.cardTarrif,
            )
        ).toDefaultResponse(
            successStatusCode = HttpStatusCode.Created.value,
            failedStatusCode = HttpStatusCode.BadGateway.value,
        ) { it }
    }
}
