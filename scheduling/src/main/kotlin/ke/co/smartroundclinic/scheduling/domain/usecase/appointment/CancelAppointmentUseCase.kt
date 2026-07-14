package ke.co.smartroundclinic.scheduling.domain.usecase.appointment

import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.NotificationChannel
import ke.co.smartroundclinic.common.PushNotificationEvents
import ke.co.smartroundclinic.common.NotificationDestination
import ke.co.smartroundclinic.common.NotificationSender
import ke.co.smartroundclinic.common.RefundProcessor
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.scheduling.data.lookup.AppointmentAdminLookup
import ke.co.smartroundclinic.scheduling.data.lookup.RefundDoc
import ke.co.smartroundclinic.scheduling.data.lookup.RefundLookup
import ke.co.smartroundclinic.scheduling.domain.repository.AppointmentRepository
import ke.co.smartroundclinic.scheduling.presentation.dto.response.AppointmentRes
import ke.co.smartroundclinic.scheduling.presentation.dto.response.toAppointmentRefundRes
import ke.co.smartroundclinic.scheduling.presentation.dto.response.toRes
import org.slf4j.LoggerFactory

class CancelAppointmentUseCase(
    private val repository: AppointmentRepository,
    private val paymentLookup: AppointmentAdminLookup,
    private val refundLookup: RefundLookup,
    private val notificationSender: NotificationSender? = null,
    private val refundProcessor: RefundProcessor? = null,
) {
    private val log = LoggerFactory.getLogger(CancelAppointmentUseCase::class.java)

    suspend operator fun invoke(
        id: String,
        userId: String,
        role: String,
        reason: String? = null,
    ): DefaultResponse<AppointmentRes?> {
        val existing = repository.getById(id)
        if (existing is Resource.Error) return existing.toDefaultResponse(failedStatusCode = 404) { null }
        val entity = existing.data ?: return Resource.Error<Nothing>("Appointment not found")
            .toDefaultResponse(failedStatusCode = 404) { null }

        val authorized = (role == "PATIENT" && entity.patientId == userId) ||
                (role == "DOCTOR" && entity.doctorId == userId)
        if (!authorized) return Resource.Error<Nothing>("Not authorized to cancel this appointment")
            .toDefaultResponse(failedStatusCode = 403) { null }

        var createdRefund: RefundDoc? = null
        val result = repository.updateStatus(id, "CANCELLED", cancellationReason = reason, cancelledBy = userId)
        if (result is Resource.Success && result.data != null) {
            runCatching {
                if (role == "DOCTOR") {
                    notificationSender?.send(
                        title = PushNotificationEvents.APPOINTMENT_CANCELLED,
                        message = "Your appointment on ${entity.date} at ${entity.slotStart} has been cancelled by your doctor",
                        channel = NotificationChannel.PUSH_NOTIFICATION,
                        destination = NotificationDestination.PATIENT,
                        recipientId = entity.patientId,
                        metadata = mapOf("event" to PushNotificationEvents.APPOINTMENT_CANCELLED, "appointmentId" to id),
                    )
                } else {
                    notificationSender?.send(
                        title = PushNotificationEvents.APPOINTMENT_CANCELLED,
                        message = "A patient has cancelled their appointment on ${entity.date} at ${entity.slotStart}",
                        channel = NotificationChannel.PUSH_NOTIFICATION,
                        destination = NotificationDestination.DOCTOR,
                        recipientId = entity.doctorId,
                        metadata = mapOf("event" to PushNotificationEvents.APPOINTMENT_CANCELLED, "appointmentId" to id),
                    )
                }
            }
            runCatching {
                val payment = paymentLookup.getPaymentByAppointmentId(id)
                if (payment == null || payment.status != "COMPLETED") {
                    log.info("No completed payment found for appointmentId=$id — skipping refund record")
                    return@runCatching
                }
                val refund = RefundDoc(
                    appointmentId = id,
                    doctorId = entity.doctorId,
                    patientId = entity.patientId,
                    paymentId = payment.paymentId,
                    amount = payment.amount,
                    currency = payment.currency,
                    reason = reason,
                    cancelledBy = userId,
                    cancelledByRole = role,
                )
                refundLookup.create(refund)
                createdRefund = refund
                runCatching { refundProcessor?.processRefund(refund.id) }
                    .onFailure { log.error("Refund processing failed for refundId=${refund.id} appointmentId=$id — ${it.message}", it) }
            }
        }
        return result.toDefaultResponse { it?.toModel()?.toRes(refund = createdRefund?.toAppointmentRefundRes()) }
    }
}
