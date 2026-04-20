package ke.co.smartroundclinic.scheduling.domain.usecase.appointment

import ke.co.smartroundclinic.admin.domain.repository.ServiceTierRepository
import ke.co.smartroundclinic.admin.domain.repository.SpecialityRepository
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.scheduling.data.entity.toEntity
import ke.co.smartroundclinic.scheduling.domain.repository.AppointmentRepository
import ke.co.smartroundclinic.scheduling.domain.repository.DoctorScheduleRepository
import ke.co.smartroundclinic.scheduling.presentation.dto.request.BookAppointmentReq
import ke.co.smartroundclinic.scheduling.presentation.dto.response.AppointmentRes
import ke.co.smartroundclinic.scheduling.presentation.dto.response.toRes
import kotlinx.datetime.LocalDate

class BookAppointmentUseCase(
    private val appointmentRepository: AppointmentRepository,
    private val scheduleRepository: DoctorScheduleRepository,
    private val specialityRepository: SpecialityRepository,
    private val serviceTierRepository: ServiceTierRepository,
) {
    suspend operator fun invoke(req: BookAppointmentReq, patientId: String): DefaultResponse<AppointmentRes?> {
        val localDate = try {
            LocalDate.parse(req.date)
        } catch (_: Exception) {
            return Resource.Error<Nothing>("Invalid date format, expected YYYY-MM-DD")
                .toDefaultResponse(failedStatusCode = 400) { null }
        }
        val dayOfWeek = localDate.dayOfWeek.ordinal  // 0=Mon..6=Sun

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

        val specialityResource = specialityRepository.getSpecialityById(req.specialityId)
        val speciality = if (specialityResource is Resource.Success && specialityResource.data != null) {
            specialityResource.data!!.toModel()
        } else {
            return Resource.Error<Nothing>("Speciality not found")
                .toDefaultResponse(failedStatusCode = 404) { null }
        }

        val serviceTierId = speciality.serviceTierId
            ?: return Resource.Error<Nothing>("Speciality has no service tier configured")
                .toDefaultResponse(failedStatusCode = 422) { null }

        val tierResource = serviceTierRepository.getServiceTierById(serviceTierId)
        val serviceTier = if (tierResource is Resource.Success && tierResource.data != null) {
            tierResource.data!!.toModel()
        } else {
            return Resource.Error<Nothing>("Service tier not found")
                .toDefaultResponse(failedStatusCode = 404) { null }
        }

        val totalSlotMinutes = ((serviceTier.consultationDuration + serviceTier.gracePeriod) / 60_000L).toInt()
        val slotEnd = addMinutes(req.slotStart, totalSlotMinutes)

        return appointmentRepository.book(req.toModel(patientId, slotEnd).toEntity())
            .toDefaultResponse(successStatusCode = 201, failedStatusCode = 409) { it?.toModel()?.toRes() }
    }

    private fun addMinutes(time: String, minutes: Int): String {
        val (h, m) = time.split(":").map { it.toInt() }
        val total = h * 60 + m + minutes
        return "%02d:%02d".format(total / 60, total % 60)
    }
}
