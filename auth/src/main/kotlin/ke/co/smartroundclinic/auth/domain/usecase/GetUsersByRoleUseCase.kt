package ke.co.smartroundclinic.auth.domain.usecase

import ke.co.smartroundclinic.auth.data.entity.UserEntity
import ke.co.smartroundclinic.auth.domain.repository.UserRepository
import ke.co.smartroundclinic.auth.presentation.dto.response.UsersPageRes
import ke.co.smartroundclinic.common.DefaultResponse

class GetUsersByRoleUseCase(private val repository: UserRepository) {
    suspend operator fun invoke(
        role: UserEntity.Role,
        page: Int,
        size: Int,
        search: String?,
    ): DefaultResponse<UsersPageRes?> =
        repository.getUsersByRole(role, page, size, search).toDefaultResponse { pair ->
            pair?.let { (items, total) ->
                UsersPageRes(
                    items = items.map { it.toModel().toUserRes() },
                    total = total,
                    page = page,
                    size = size,
                )
            }
        }
}
