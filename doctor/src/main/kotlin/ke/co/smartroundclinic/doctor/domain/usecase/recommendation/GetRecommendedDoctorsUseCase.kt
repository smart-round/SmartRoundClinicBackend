package ke.co.smartroundclinic.doctor.domain.usecase.recommendation

import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.doctor.domain.repository.RecommendationRepository
import ke.co.smartroundclinic.doctor.presentation.dto.response.RecommendedDoctorsPageRes
import ke.co.smartroundclinic.infra.AppConfig
import ke.co.smartroundclinic.infra.storage.StorageRepository

class GetRecommendedDoctorsUseCase(
    private val repository: RecommendationRepository,
    private val storageRepository: StorageRepository,
) {
    suspend operator fun invoke(
        specializationId: String?,
        page: Int,
        size: Int,
        excludeDoctorId: String? = null,
    ): DefaultResponse<RecommendedDoctorsPageRes?> {
        val result = repository.getRecommendations(specializationId, page, size, excludeDoctorId)

        // Pre-generate presigned URLs before entering the non-suspend toDefaultResponse lambda.
        val presignedUrlsByDoctorId: Map<String, String?> =
            (result as? Resource.Success)?.data?.first?.mapNotNull { doctor ->
                doctor.profilePicture?.let { key ->
                    val url = (storageRepository.presignedGetUrl(
                        bucket = AppConfig.r2.bucket,
                        key = key,
                        expiresInSeconds = 86400,
                    ) as? Resource.Success)?.data
                    doctor.doctorId to url
                }
            }?.toMap() ?: emptyMap()

        return result.toDefaultResponse { pair ->
            pair?.let { (items, total) ->
                RecommendedDoctorsPageRes(
                    items = items.map { doctor ->
                        doctor.copy(profilePicture = presignedUrlsByDoctorId[doctor.doctorId])
                    },
                    total = total,
                    page = page,
                    size = size,
                )
            }
        }
    }
}
