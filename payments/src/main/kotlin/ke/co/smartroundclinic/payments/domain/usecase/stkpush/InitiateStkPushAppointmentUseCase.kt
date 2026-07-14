package ke.co.smartroundclinic.payments.domain.usecase.stkpush

import io.ktor.http.HttpStatusCode
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.infra.IntaSendConfig
import ke.co.smartroundclinic.payments.data.entity.PaymentEntity
import ke.co.smartroundclinic.payments.data.lookup.AppointmentInfoLookup
import ke.co.smartroundclinic.payments.data.remote.instasend.computeStkPushSignature
import ke.co.smartroundclinic.payments.data.remote.instasend.request.STKPushReq
import ke.co.smartroundclinic.payments.domain.repository.IntaSendRepository
import ke.co.smartroundclinic.payments.domain.repository.PaymentRepository
import ke.co.smartroundclinic.payments.presentation.dto.response.StkPushInitiationRes
import org.slf4j.LoggerFactory
import java.util.UUID
import kotlin.time.Clock

class InitiateStkPushAppointmentUseCase(
    private val intaSendRepository: IntaSendRepository,
    private val paymentRepository: PaymentRepository,
    private val config: IntaSendConfig,
    private val lookup: AppointmentInfoLookup,
) {
    private val log = LoggerFactory.getLogger(InitiateStkPushAppointmentUseCase::class.java)

    suspend operator fun invoke(
        appointmentId: String,
        phoneNumber: String,
        requestingPatientId: String,
    ): DefaultResponse<StkPushInitiationRes?> {
        val participants = lookup.getParticipants(appointmentId) ?: return DefaultResponse(
            httpStatusCode = HttpStatusCode.NotFound.value,
            status = false,
            message = "Appointment not found",
            data = null,
        )

        if (participants.patientId != requestingPatientId) return DefaultResponse(
            httpStatusCode = HttpStatusCode.Forbidden.value,
            status = false,
            message = "You are not authorized to pay for this appointment",
            data = null,
        )

        val existing = (paymentRepository.getByAppointmentId(appointmentId) as? Resource.Success)?.data
        if (existing?.status == PaymentEntity.PaymentStatus.COMPLETED) return DefaultResponse(
            httpStatusCode = HttpStatusCode.Conflict.value,
            status = false,
            message = "This appointment has already been paid for",
            data = null,
        )

        val amount = participants.tierPrice.toInt().toString()

        val result = intaSendRepository.stkPush(
            STKPushReq(
                amount = amount,
                apiRef = appointmentId,
                phoneNumber = phoneNumber,
                signature = computeStkPushSignature(config.secretKey, phoneNumber, amount, appointmentId),
                walletId = config.collectionsWalletId,
            )
        )

        if (result is Resource.Success) {
            runCatching {
                paymentRepository.save(
                    PaymentEntity(
                        id = UUID.randomUUID().toString(),
                        appointmentId = appointmentId,
                        patientId = participants.patientId,
                        doctorId = participants.doctorId,
                        amount = participants.tierPrice,
                        currency = "KES",
                        status = PaymentEntity.PaymentStatus.PENDING,
                        paymentMethod = "STK_PUSH",
                        transactionRef = appointmentId,
                        invoiceId = result.data?.invoice?.invoiceId,
                        commissionRate = participants.commissionRate,
                        createdAt = Clock.System.now().toString(),
                    )
                )
            }.onFailure {
                log.error("Failed to save STK push payment for appointmentId=$appointmentId — ${it.message}", it)
            }
        }

        return result.toDefaultResponse(
            successStatusCode = HttpStatusCode.Created.value,
            failedStatusCode = HttpStatusCode.BadGateway.value,
        ) { res ->
            res?.let {
                StkPushInitiationRes(
                    invoiceId = it.invoice.invoiceId,
                    transactionRef = appointmentId,
                    state = it.invoice.state,
                    amount = participants.tierPrice,
                    currency = "KES",
                )
            }
        }
    }
}
