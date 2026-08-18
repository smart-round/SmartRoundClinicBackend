package ke.co.smartroundclinic.scheduling.domain.usecase.appointment

import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.NotificationChannel
import ke.co.smartroundclinic.common.PushNotificationEvents
import ke.co.smartroundclinic.common.NotificationDestination
import ke.co.smartroundclinic.common.NotificationSender
import ke.co.smartroundclinic.common.ReferralLinker
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.scheduling.data.entity.toEntity
import ke.co.smartroundclinic.scheduling.data.lookup.PaymentVerificationLookup
import ke.co.smartroundclinic.scheduling.data.repository.ConsultationInfoResult
import ke.co.smartroundclinic.scheduling.data.repository.ServiceTierLookup
import ke.co.smartroundclinic.scheduling.domain.repository.AppointmentRepository
import ke.co.smartroundclinic.scheduling.domain.repository.DoctorScheduleRepository
import ke.co.smartroundclinic.scheduling.domain.repository.SlotOverrideRepository
import ke.co.smartroundclinic.scheduling.domain.usecase.SlotEngine
import ke.co.smartroundclinic.scheduling.presentation.dto.request.BookAppointmentReq
import ke.co.smartroundclinic.scheduling.presentation.dto.response.AppointmentRes
import ke.co.smartroundclinic.scheduling.presentation.dto.response.toRes
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.slf4j.LoggerFactory

class BookAppointmentUseCase(
    private val appointmentRepository: AppointmentRepository,
    private val scheduleRepository: DoctorScheduleRepository,
    private val overrideRepository: SlotOverrideRepository,
    private val serviceTierLookup: ServiceTierLookup,
    private val paymentVerificationLookup: PaymentVerificationLookup,
    private val notificationSender: NotificationSender? = null,
    private val referralLinker: ReferralLinker? = null,
) {
    private val log = LoggerFactory.getLogger(BookAppointmentUseCase::class.java)

    suspend operator fun invoke(req: BookAppointmentReq, patientId: String): DefaultResponse<AppointmentRes?> {
        val logCtx = "patientId=$patientId doctorId=${req.doctorId} date=${req.date} slotStart=${req.slotStart} transactionRef=${req.transactionRef}"

        val tierInfo = serviceTierLookup.getConsultationInfoForDoctor(req.doctorId)
        if (tierInfo is ConsultationInfoResult.Error) {
            log.warn("Booking rejected — {} — {}", tierInfo.message, logCtx)
            return Resource.Error<Nothing>(tierInfo.message)
                .toDefaultResponse(failedStatusCode = tierInfo.httpStatus) { null }
        }
        val (serviceTierId, consultationDuration, gracePeriod) = tierInfo as ConsultationInfoResult.Success

        val localDate = try {
            LocalDate.parse(req.date)
        } catch (_: Exception) {
            log.warn("Booking rejected — invalid date format — {}", logCtx)
            return Resource.Error<Nothing>("Invalid date format, expected YYYY-MM-DD")
                .toDefaultResponse(failedStatusCode = 400) { null }
        }
        val dayOfWeek = localDate.dayOfWeek.ordinal

        val scheduleResource = scheduleRepository.getByDoctorAndDay(req.doctorId, dayOfWeek)
        val schedule = if (scheduleResource is Resource.Success && scheduleResource.data != null) {
            scheduleResource.data!!.toModel()
        } else {
            log.warn("Booking rejected — doctor has no schedule for this day — {}", logCtx)
            return Resource.Error<Nothing>("Doctor has no schedule for this day")
                .toDefaultResponse(failedStatusCode = 400) { null }
        }

        if (!schedule.isActive) {
            log.warn("Booking rejected — doctor not available on this day — {}", logCtx)
            return Resource.Error<Nothing>("Doctor is not available on this day")
                .toDefaultResponse(failedStatusCode = 400) { null }
        }

        val bookedIntervals = (appointmentRepository.getByDoctorAndDate(req.doctorId, req.date) as? Resource.Success)
            ?.data
            ?.filter { it.status == "BOOKED" || it.status == "CONFIRMED" }
            ?.map { SlotEngine.toMinutes(it.slotStart) to SlotEngine.toMinutes(it.slotEnd) }
            ?: emptyList()

        val overrides = (overrideRepository.getByDoctorAndDate(req.doctorId, req.date) as? Resource.Success)
            ?.data
            ?.map { it.toModel() } ?: emptyList()

        val tz = TimeZone.of(schedule.timezone)
        val nowLdt = Clock.System.now().toLocalDateTime(tz)
        val today = nowLdt.date.toString()
        val nowMinutes = if (req.date == today) nowLdt.hour * 60 + nowLdt.minute else null

        val availableSlots = SlotEngine.computeAvailableSlots(schedule, consultationDuration, bookedIntervals, overrides, nowMinutes, gridStep = schedule.slotDuration)

        if (req.slotStart !in availableSlots) {
            log.warn("Booking rejected — slot {} not available (available={}) — {}", req.slotStart, availableSlots, logCtx)
            return Resource.Error<Nothing>("Slot ${req.slotStart} is not available")
                .toDefaultResponse(failedStatusCode = 400) { null }
        }

        // Verify payment before creating the appointment
        val payment = paymentVerificationLookup.getByTransactionRef(req.transactionRef)
            ?: run {
                log.warn("Booking rejected — no payment found for transactionRef — {}", logCtx)
                return Resource.Error<Nothing>("Payment not found for the provided transaction reference")
                    .toDefaultResponse(failedStatusCode = 400) { null }
            }

        if (payment.status != "COMPLETED") {
            log.warn("Booking rejected — payment status={} — {}", payment.status, logCtx)
            return Resource.Error<Nothing>("Payment has not been completed. Current status: ${payment.status}")
                .toDefaultResponse(failedStatusCode = 402) { null }
        }

        if (payment.doctorId != req.doctorId) {
            log.warn("Booking rejected — payment.doctorId={} does not match req.doctorId — {}", payment.doctorId, logCtx)
            return Resource.Error<Nothing>("Payment was not made for this doctor")
                .toDefaultResponse(failedStatusCode = 400) { null }
        }

        if (payment.patientId != patientId) {
            log.warn("Booking rejected — payment.patientId={} does not match caller — {}", payment.patientId, logCtx)
            return Resource.Error<Nothing>("Payment does not belong to this patient")
                .toDefaultResponse(failedStatusCode = 403) { null }
        }

        if (!payment.appointmentId.isNullOrBlank()) {
            log.warn("Booking rejected — payment already linked to appointmentId={} — {}", payment.appointmentId, logCtx)
            return Resource.Error<Nothing>("This payment has already been used to book another appointment")
                .toDefaultResponse(failedStatusCode = 409) { null }
        }

        if (!req.referralId.isNullOrBlank()) {
            val referralError = referralLinker?.validateForBooking(req.referralId, req.doctorId, patientId)
            if (referralError != null) {
                log.warn("Booking rejected — referral invalid: {} — {}", referralError, logCtx)
                return Resource.Error<Nothing>(referralError)
                    .toDefaultResponse(failedStatusCode = 400) { null }
            }
        }

        val slotEnd = SlotEngine.fromMinutes(SlotEngine.toMinutes(req.slotStart) + consultationDuration + gracePeriod)

        val result = appointmentRepository.book(req.toModel(patientId, slotEnd, serviceTierId, consultationDuration).toEntity())
        if (result is Resource.Success && result.data != null) {
            val appt = result.data!!
            runCatching { paymentVerificationLookup.linkToAppointment(payment.id, appt.id) }
            if (!req.referralId.isNullOrBlank()) {
                runCatching { appointmentRepository.setReferralId(appt.id, req.referralId) }
                    .onSuccess { tagResult ->
                        val tagged = (tagResult as? Resource.Success)?.data == true
                        if (!tagged) log.error("Referral tagging did not take effect for appointmentId=${appt.id} referralId=${req.referralId} — result={} — {}", tagResult, logCtx)
                    }
                    .onFailure { e -> log.error("Referral tagging threw for appointmentId=${appt.id} referralId=${req.referralId} — {}", logCtx, e) }
                runCatching { referralLinker?.linkResultingAppointment(req.referralId, appt.id) }
                    .onFailure { e -> log.error("Referral linkResultingAppointment threw for appointmentId=${appt.id} referralId=${req.referralId} — {}", logCtx, e) }
            }
            runCatching {
                notificationSender?.send(
                    title = PushNotificationEvents.APPOINTMENT_REQUEST,
                    message = "You have a new appointment request for ${appt.date} at ${appt.slotStart}",
                    channel = NotificationChannel.PUSH_NOTIFICATION,
                    destination = NotificationDestination.DOCTOR,
                    recipientId = appt.doctorId,
                    metadata = mapOf("event" to PushNotificationEvents.APPOINTMENT_REQUEST, "appointmentId" to appt.id),
                )
                notificationSender?.send(
                    title = PushNotificationEvents.APPOINTMENT_BOOKED,
                    message = "Your appointment on ${appt.date} at ${appt.slotStart} has been booked successfully",
                    channel = NotificationChannel.PUSH_NOTIFICATION,
                    destination = NotificationDestination.PATIENT,
                    recipientId = appt.patientId,
                    metadata = mapOf("event" to PushNotificationEvents.APPOINTMENT_BOOKED, "appointmentId" to appt.id),
                )
            }.onFailure { e -> log.error("Appointment-booked push notification threw for appointmentId=${appt.id} — {}", logCtx, e) }
        }
        return result.toDefaultResponse(successStatusCode = 201, failedStatusCode = 409) { it?.toModel()?.toRes() }
    }
}
