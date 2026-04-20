package ke.co.smartroundclinic.auth.domain.usecase

import ke.co.smartroundclinic.auth.data.entity.UserEntity
import ke.co.smartroundclinic.auth.domain.repository.UserRepository
import ke.co.smartroundclinic.auth.presentation.dto.response.UserRes
import ke.co.smartroundclinic.common.DefaultResponse
import io.ktor.http.HttpStatusCode

class AdminUpdateUserUseCase(private val repository: UserRepository) {
    suspend operator fun invoke(
        userId: String,
        accountStatus: UserEntity.AccountStatus?,
        verificationStatus: UserEntity.VerificationStatus?,
    ): DefaultResponse<UserRes?> =
        repository.adminUpdateUser(userId, accountStatus, verificationStatus)
            .toDefaultResponse(failedStatusCode = HttpStatusCode.NotFound.value) { it?.toModel()?.toUserRes() }
}
