package ke.co.smartroundclinic.scheduling.domain.usecase.appointment

import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.DoctorSpecialitiesResolver
import ke.co.smartroundclinic.common.PatientNameResolver
import ke.co.smartroundclinic.common.ReferralDisplayResolver
import ke.co.smartroundclinic.common.UserProfilePictureResolver
import ke.co.smartroundclinic.scheduling.data.repository.ServiceTierLookup
import ke.co.smartroundclinic.scheduling.domain.repository.AppointmentRepository
import ke.co.smartroundclinic.scheduling.presentation.dto.response.DoctorAppointmentDetailRes
import ke.co.smartroundclinic.scheduling.presentation.dto.response.toDetailRes
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class GetDoctorAppointmentDetailsUseCase(
    private val repository: AppointmentRepository,
    private val patientNameResolver: PatientNameResolver?,
    private val doctorSpecialitiesResolver: DoctorSpecialitiesResolver?,
    private val userProfilePictureResolver: UserProfilePictureResolver? = null,
    private val referralDisplayResolver: ReferralDisplayResolver? = null,
    private val serviceTierLookup: ServiceTierLookup? = null,
) {
    suspend operator fun invoke(doctorId: String, filter: String? = null): DefaultResponse<List<DoctorAppointmentDetailRes>?> {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()
        val resource = repository.getByDoctorFiltered(doctorId, filter, today)
        val entities = resource.data ?: emptyList()

        val patientIds = entities.map { it.patientId }

        val patientNames: Map<String, String> = patientNameResolver
            ?.getPatientNames(patientIds)
            ?: emptyMap()

        val patientPictures: Map<String, String?> = userProfilePictureResolver
            ?.getProfilePictureUrls(patientIds)
            ?: emptyMap()

        val specialities: List<String> = doctorSpecialitiesResolver
            ?.getDoctorSpecialityNames(doctorId)
            ?: emptyList()

        val referralIds = entities.mapNotNull { it.referralId }.distinct()
        val referralDisplayInfo = referralDisplayResolver?.let { resolver ->
            referralIds.associateWith { resolver.getReferralDisplayInfo(it) }
        } ?: emptyMap()

        // The appointment's amount is the price of the tier it was booked against.
        val tierPrices: Map<String, Double> = serviceTierLookup
            ?.getTierPrices(entities.map { it.serviceTierId })
            ?: emptyMap()

        return resource.toDefaultResponse { items ->
            items?.map { entity ->
                val appointment = entity.toModel()
                val referralInfo = appointment.referralId?.let { referralDisplayInfo[it] }
                appointment.toDetailRes(
                    patientName = patientNames[appointment.patientId],
                    patientProfilePicture = patientPictures[appointment.patientId],
                    doctorSpecialities = specialities,
                    referredByDoctorName = referralInfo?.referringDoctorName,
                    referredByDoctorPicture = referralInfo?.referringDoctorPicture,
                    amount = tierPrices[appointment.serviceTierId],
                )
            }
        }
    }
}
