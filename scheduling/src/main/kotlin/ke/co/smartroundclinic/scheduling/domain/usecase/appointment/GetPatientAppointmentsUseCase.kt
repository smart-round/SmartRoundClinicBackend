package ke.co.smartroundclinic.scheduling.domain.usecase.appointment

import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.DoctorSpecialitiesResolver
import ke.co.smartroundclinic.common.PatientNameResolver
import ke.co.smartroundclinic.common.UserProfilePictureResolver
import ke.co.smartroundclinic.scheduling.domain.repository.AppointmentRepository
import ke.co.smartroundclinic.scheduling.presentation.dto.response.AppointmentRes
import ke.co.smartroundclinic.scheduling.presentation.dto.response.toRes

class GetPatientAppointmentsUseCase(
    private val repository: AppointmentRepository,
    private val patientNameResolver: PatientNameResolver? = null,
    private val userProfilePictureResolver: UserProfilePictureResolver? = null,
    private val doctorSpecialitiesResolver: DoctorSpecialitiesResolver? = null,
) {
    suspend operator fun invoke(patientId: String): DefaultResponse<List<AppointmentRes>?> {
        val resource = repository.getByPatient(patientId)
        val entities = resource.data ?: emptyList()

        val doctorIds = entities.map { it.doctorId }.distinct()

        val doctorNames: Map<String, String> = patientNameResolver
            ?.getPatientNames(doctorIds)
            ?: emptyMap()

        val doctorPictures: Map<String, String?> = userProfilePictureResolver
            ?.getProfilePictureUrls(doctorIds)
            ?: emptyMap()

        val doctorSpecialities: Map<String, String?> = if (doctorSpecialitiesResolver != null) {
            doctorIds.associateWith { id ->
                doctorSpecialitiesResolver.getDoctorSpecialityNames(id).firstOrNull()
            }
        } else emptyMap()

        return resource.toDefaultResponse { items ->
            items?.map { entity ->
                val appointment = entity.toModel()
                appointment.toRes(
                    doctorName = doctorNames[appointment.doctorId],
                    doctorProfilePicture = doctorPictures[appointment.doctorId],
                    doctorSpeciality = doctorSpecialities[appointment.doctorId],
                )
            }
        }
    }
}
