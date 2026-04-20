package ke.co.smartroundclinic.auth.domain.usecase

import ke.co.smartroundclinic.auth.data.entity.UserEntity
import ke.co.smartroundclinic.auth.domain.repository.UserRepository
import ke.co.smartroundclinic.auth.presentation.dto.response.UsersPageRes
import ke.co.smartroundclinic.common.DefaultResponse

class FilterUsersByRoleUseCase(private val repository: UserRepository) {
    suspend operator fun invoke(
        role: UserEntity.Role,
        page: Int,
        size: Int,
        accountStatus: UserEntity.AccountStatus?,
        createdFrom: String?,
        createdTo: String?,
    ): DefaultResponse<UsersPageRes?> =
        repository.filterUsers(role, page, size, accountStatus, createdFrom, createdTo)
            .toDefaultResponse { pair ->
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
