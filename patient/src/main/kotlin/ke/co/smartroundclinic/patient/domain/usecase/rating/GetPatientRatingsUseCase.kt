package ke.co.smartroundclinic.patient.domain.usecase.rating

import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.PatientNameResolver
import ke.co.smartroundclinic.common.UserProfilePictureResolver
import ke.co.smartroundclinic.patient.domain.model.toModel
import ke.co.smartroundclinic.patient.domain.repository.PatientRatingRepository
import ke.co.smartroundclinic.patient.presentation.dto.response.PatientRatingsPageRes
import ke.co.smartroundclinic.patient.presentation.dto.response.toRes

class GetPatientRatingsUseCase(
    private val repository: PatientRatingRepository,
    private val patientNameResolver: PatientNameResolver? = null,
    private val userProfilePictureResolver: UserProfilePictureResolver? = null,
) {
    suspend operator fun invoke(
        patientId: String,
        page: Int,
        size: Int,
    ): DefaultResponse<PatientRatingsPageRes?> {
        val resource = repository.getByPatientId(patientId, page, size)
        val entities = resource.data?.first ?: emptyList()
        val doctorIds = entities.map { it.doctorId }.distinct()

        val doctorNames = patientNameResolver?.getPatientNames(doctorIds) ?: emptyMap()
        val doctorPictures = userProfilePictureResolver?.getProfilePictureUrls(doctorIds) ?: emptyMap()

        return resource.toDefaultResponse { pair ->
            pair?.let { (items, total) ->
                PatientRatingsPageRes(
                    items = items.map { entity ->
                        val rating = entity.toModel()
                        rating.toRes(
                            doctorName = doctorNames[rating.doctorId],
                            doctorProfilePicture = doctorPictures[rating.doctorId],
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
