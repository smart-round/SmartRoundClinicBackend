package ke.co.smartroundclinic.payments.domain.usecase.paymentlink

import io.ktor.http.HttpStatusCode
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.infra.IntaSendConfig
import ke.co.smartroundclinic.payments.data.entity.PaymentEntity
import ke.co.smartroundclinic.payments.data.lookup.DoctorTierPriceLookup
import ke.co.smartroundclinic.payments.data.remote.dto.request.CreatePaymentLinkReq
import ke.co.smartroundclinic.payments.data.remote.dto.response.PaymentLinkRes
import ke.co.smartroundclinic.payments.domain.repository.IntaSendRepository
import ke.co.smartroundclinic.payments.domain.repository.PaymentRepository
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlin.time.Clock

class CreatePreBookingPaymentLinkUseCase(
    private val intaSendRepository: IntaSendRepository,
    private val paymentRepository: PaymentRepository,
    private val config: IntaSendConfig,
    private val doctorTierPriceLookup: DoctorTierPriceLookup,
) {
    private val log = LoggerFactory.getLogger(CreatePreBookingPaymentLinkUseCase::class.java)

    suspend operator fun invoke(doctorId: String, patientId: String): DefaultResponse<PaymentLinkRes?> {
        val tierInfo = doctorTierPriceLookup.getTierInfo(doctorId)
            ?: return DefaultResponse(
                httpStatusCode = HttpStatusCode.UnprocessableEntity.value,
                status = false,
                message = "Doctor is not yet configured for appointments. Please contact support.",
                data = null,
            )

        val amount = tierInfo.tierPrice.toInt()
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH-mm"))
        val title = "Consultation Payment $timestamp"

        val result = intaSendRepository.createPaymentLink(
            CreatePaymentLinkReq(
                id = UUID.randomUUID().toString(),
                apiRef = "prebooking_${patientId}_${doctorId}",
                title = title,
                isActive = true,
                redirectUrl = config.callbackPaymentsUrl,
                amount = amount,
                usageLimit = 1,
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
                        commissionRate = tierInfo.commissionRate,
                        createdAt = Clock.System.now().toString(),
                    )
                )
            }.onFailure { log.error("Failed to save pre-booking payment entity for doctorId=$doctorId patientId=$patientId — ${it.message}", it) }
        }

        return result.toDefaultResponse(
            successStatusCode = HttpStatusCode.Created.value,
            failedStatusCode = HttpStatusCode.BadGateway.value,
        ) { it }
    }
}
