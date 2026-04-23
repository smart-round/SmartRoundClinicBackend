package ke.co.smartroundclinic.scheduling.domain.usecase.appointment

import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.DoctorSpecialitiesResolver
import ke.co.smartroundclinic.common.PatientNameResolver
import ke.co.smartroundclinic.scheduling.domain.repository.AppointmentRepository
import ke.co.smartroundclinic.scheduling.presentation.dto.response.DoctorAppointmentDetailRes
import ke.co.smartroundclinic.scheduling.presentation.dto.response.toDetailRes

class GetDoctorAppointmentDetailsUseCase(
    private val repository: AppointmentRepository,
    private val patientNameResolver: PatientNameResolver?,
    private val doctorSpecialitiesResolver: DoctorSpecialitiesResolver?,
) {
    suspend operator fun invoke(doctorId: String): DefaultResponse<List<DoctorAppointmentDetailRes>?> {
        val resource = repository.getByDoctor(doctorId)
        val entities = resource.data ?: emptyList()

        val patientNames: Map<String, String> = patientNameResolver
            ?.getPatientNames(entities.map { it.patientId })
            ?: emptyMap()

        val specialities: List<String> = doctorSpecialitiesResolver
            ?.getDoctorSpecialityNames(doctorId)
            ?: emptyList()

        return resource.toDefaultResponse { items ->
            items?.map { entity ->
                val appointment = entity.toModel()
                appointment.toDetailRes(
                    patientName = patientNames[appointment.patientId],
                    doctorSpecialities = specialities,
                )
            }
        }
    }
}
