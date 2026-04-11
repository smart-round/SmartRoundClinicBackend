package ke.co.smartroundclinic.auth.domain.usecase

import ke.co.smartroundclinic.auth.domain.repository.UserRepository
import ke.co.smartroundclinic.auth.presentation.dto.response.UserRes
import ke.co.smartroundclinic.common.DefaultResponse

class GetUserUserUseCase(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(userId: String): DefaultResponse<UserRes?> = userRepository.getUser(userId)
        .toDefaultResponse(){it?.toModel()?.toUserRes()}
}