package ke.co.smartroundclinic.auth.domain.usecase

import io.ktor.http.HttpStatusCode
import ke.co.smartroundclinic.auth.domain.repository.UserRepository
import ke.co.smartroundclinic.auth.presentation.dto.response.UserRes
import ke.co.smartroundclinic.common.DefaultResponse

class UpgradeToSuperAdminUseCase(private val repository: UserRepository) {
    suspend operator fun invoke(userId: String): DefaultResponse<UserRes?> =
        repository.upgradeToSuperAdmin(userId)
            .toDefaultResponse(failedStatusCode = HttpStatusCode.NotFound.value) { it?.toModel()?.toUserRes() }
}
