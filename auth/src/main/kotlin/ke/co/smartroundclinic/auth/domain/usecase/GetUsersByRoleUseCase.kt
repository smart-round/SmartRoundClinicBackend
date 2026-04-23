package ke.co.smartroundclinic.auth.domain.usecase

import ke.co.smartroundclinic.auth.data.entity.UserEntity
import ke.co.smartroundclinic.auth.domain.repository.UserRepository
import ke.co.smartroundclinic.auth.presentation.dto.response.UsersPageRes
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.PatientProfileResolver
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.infra.AppConfig
import ke.co.smartroundclinic.infra.storage.StorageRepository

class GetUsersByRoleUseCase(
    private val repository: UserRepository,
    private val storageRepository: StorageRepository,
    private val patientProfileResolver: PatientProfileResolver?,
) {
    suspend operator fun invoke(
        role: UserEntity.Role,
        page: Int,
        size: Int,
        search: String?,
    ): DefaultResponse<UsersPageRes?> {
        val resource = repository.getUsersByRole(role, page, size, search)
        val entities = resource.data?.first ?: emptyList()
        val presignedUrls: Map<String, String?> = entities
            .filter { it.profilePicture != null }
            .associate { entity ->
                entity.id to run {
                    val result = storageRepository.presignedGetUrl(AppConfig.r2.bucket, entity.profilePicture!!, 86400)
                    (result as? Resource.Success)?.data
                }
            }
        val profiles = if (role == UserEntity.Role.PATIENT && patientProfileResolver != null) {
            patientProfileResolver.getProfilesByPatientIds(entities.map { it.id })
        } else {
            emptyMap()
        }
        return resource.toDefaultResponse { pair ->
            pair?.let { (items, total) ->
                UsersPageRes(
                    items = items.map { entity ->
                        val user = entity.toModel()
                        user.toUserRes().copy(
                            profilePicture = presignedUrls[user.id],
                            personalInfo = profiles[user.id],
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
