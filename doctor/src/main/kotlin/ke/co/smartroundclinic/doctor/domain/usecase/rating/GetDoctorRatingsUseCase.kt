package ke.co.smartroundclinic.doctor.domain.usecase.rating

import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.PatientNameResolver
import ke.co.smartroundclinic.common.UserProfilePictureResolver
import ke.co.smartroundclinic.doctor.domain.model.toModel
import ke.co.smartroundclinic.doctor.domain.repository.DoctorRatingRepository
import ke.co.smartroundclinic.doctor.presentation.dto.response.RatingsPageRes
import ke.co.smartroundclinic.doctor.presentation.dto.response.toRes

class GetDoctorRatingsUseCase(
    private val repository: DoctorRatingRepository,
    private val patientNameResolver: PatientNameResolver? = null,
    private val userProfilePictureResolver: UserProfilePictureResolver? = null,
) {
    suspend operator fun invoke(
        doctorId: String,
        page: Int,
        size: Int,
    ): DefaultResponse<RatingsPageRes?> {
        val resource = repository.getByDoctorId(doctorId, page, size)
        val entities = resource.data?.first ?: emptyList()
        val patientIds = entities.map { it.patientId }.distinct()

        val patientNames = patientNameResolver?.getPatientNames(patientIds) ?: emptyMap()
        val patientPictures = userProfilePictureResolver?.getProfilePictureUrls(patientIds) ?: emptyMap()

        return resource.toDefaultResponse { pair ->
            pair?.let { (items, total) ->
                RatingsPageRes(
                    items = items.map { entity ->
                        val rating = entity.toModel()
                        rating.toRes(
                            patientName = patientNames[rating.patientId],
                            patientProfilePicture = patientPictures[rating.patientId],
                        )
                    },
                    total = total,
                    page = page,
                    size = size,
                )
            }
        }
    }
}
