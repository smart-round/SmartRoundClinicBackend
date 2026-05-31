package ke.co.smartroundclinic.scheduling.domain.usecase.appointment

import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.NotificationChannel
import ke.co.smartroundclinic.common.NotificationDestination
import ke.co.smartroundclinic.common.NotificationSender
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

class BookAppointmentUseCase(
    private val appointmentRepository: AppointmentRepository,
    private val scheduleRepository: DoctorScheduleRepository,
    private val overrideRepository: SlotOverrideRepository,
    private val serviceTierLookup: ServiceTierLookup,
    private val paymentVerificationLookup: PaymentVerificationLookup,
    private val notificationSender: NotificationSender? = null,
) {
    suspend operator fun invoke(req: BookAppointmentReq, patientId: String): DefaultResponse<AppointmentRes?> {
        val tierInfo = serviceTierLookup.getConsultationInfoForDoctor(req.doctorId)
        if (tierInfo is ConsultationInfoResult.Error) {
            return Resource.Error<Nothing>(tierInfo.message)
                .toDefaultResponse(failedStatusCode = tierInfo.httpStatus) { null }
        }
        val (serviceTierId, consultationDuration, gracePeriod) = tierInfo as ConsultationInfoResult.Success

        val localDate = try {
            LocalDate.parse(req.date)
        } catch (_: Exception) {
            return Resource.Error<Nothing>("Invalid date format, expected YYYY-MM-DD")
                .toDefaultResponse(failedStatusCode = 400) { null }
        }
        val dayOfWeek = localDate.dayOfWeek.ordinal

        val scheduleResource = scheduleRepository.getByDoctorAndDay(req.doctorId, dayOfWeek)
        val schedule = if (scheduleResource is Resource.Success && scheduleResource.data != null) {
            scheduleResource.data!!.toModel()
        } else {
            return Resource.Error<Nothing>("Doctor has no schedule for this day")
                .toDefaultResponse(failedStatusCode = 400) { null }
        }

        if (!schedule.isActive) {
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
            return Resource.Error<Nothing>("Slot ${req.slotStart} is not available")
                .toDefaultResponse(failedStatusCode = 400) { null }
        }

        // Verify payment before creating the appointment
        val payment = paymentVerificationLookup.getByTransactionRef(req.transactionRef)
            ?: return Resource.Error<Nothing>("Payment not found for the provided transaction reference")
                .toDefaultResponse(failedStatusCode = 400) { null }

        if (payment.status != "COMPLETED") {
            return Resource.Error<Nothing>("Payment has not been completed. Current status: ${payment.status}")
                .toDefaultResponse(failedStatusCode = 402) { null }
        }

        if (payment.doctorId != req.doctorId) {
            return Resource.Error<Nothing>("Payment was not made for this doctor")
                .toDefaultResponse(failedStatusCode = 400) { null }
        }

        if (payment.patientId != patientId) {
            return Resource.Error<Nothing>("Payment does not belong to this patient")
                .toDefaultResponse(failedStatusCode = 403) { null }
        }

        if (!payment.appointmentId.isNullOrBlank()) {
            return Resource.Error<Nothing>("This payment has already been used to book another appointment")
                .toDefaultResponse(failedStatusCode = 409) { null }
        }

        val slotEnd = SlotEngine.fromMinutes(SlotEngine.toMinutes(req.slotStart) + consultationDuration + gracePeriod)

        val result = appointmentRepository.book(req.toModel(patientId, slotEnd, serviceTierId, consultationDuration).toEntity())
        if (result is Resource.Success && result.data != null) {
            val appt = result.data!!
            runCatching { paymentVerificationLookup.linkToAppointment(payment.id, appt.id) }
            runCatching {
                notificationSender?.send(
                    title = "New Appointment Request",
                    message = "You have a new appointment request for ${appt.date} at ${appt.slotStart}",
                    channel = NotificationChannel.PUSH_NOTIFICATION,
                    destination = NotificationDestination.DOCTOR,
                    recipientId = appt.doctorId,
                )
                notificationSender?.send(
                    title = "Appointment Booked",
                    message = "Your appointment on ${appt.date} at ${appt.slotStart} has been booked successfully",
                    channel = NotificationChannel.PUSH_NOTIFICATION,
                    destination = NotificationDestination.PATIENT,
                    recipientId = appt.patientId,
                )
            }
        }
        return result.toDefaultResponse(successStatusCode = 201, failedStatusCode = 409) { it?.toModel()?.toRes() }
    }
}
