package ke.co.smartroundclinic.scheduling.domain.usecase.appointment

import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.DoctorSpecialitiesResolver
import ke.co.smartroundclinic.common.PatientNameResolver
import ke.co.smartroundclinic.scheduling.domain.repository.AppointmentRepository
import ke.co.smartroundclinic.scheduling.presentation.dto.response.AppointmentAdminRes
import ke.co.smartroundclinic.scheduling.presentation.dto.response.toAdminRes

class GetPatientAppointmentsForAdminUseCase(
    private val repository: AppointmentRepository,
    private val doctorNameResolver: PatientNameResolver?,
    private val doctorSpecialitiesResolver: DoctorSpecialitiesResolver?,
) {
    suspend operator fun invoke(patientId: String): DefaultResponse<List<AppointmentAdminRes>?> {
        val resource = repository.getByPatient(patientId)
        val entities = resource.data ?: emptyList()

        val uniqueDoctorIds = entities.map { it.doctorId }.toSet().toList()
        val doctorNames: Map<String, String> = doctorNameResolver
            ?.getPatientNames(uniqueDoctorIds)
            ?: emptyMap()
        val doctorSpecialities: Map<String, List<String>> = uniqueDoctorIds.associateWith { doctorId ->
            doctorSpecialitiesResolver?.getDoctorSpecialityNames(doctorId) ?: emptyList()
        }

        return resource.toDefaultResponse { items ->
            items?.map { entity ->
                val appointment = entity.toModel()
                appointment.toAdminRes(
                    doctorName = doctorNames[appointment.doctorId],
                    doctorSpecialities = doctorSpecialities[appointment.doctorId] ?: emptyList(),
                )
            }
        }
    }
}
