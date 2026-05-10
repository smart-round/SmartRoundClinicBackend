package ke.co.smartroundclinic.auth.domain.usecase

import io.ktor.http.HttpStatusCode
import ke.co.smartroundclinic.auth.data.entity.UserEntity
import ke.co.smartroundclinic.auth.domain.repository.UserRepository
import ke.co.smartroundclinic.auth.presentation.dto.response.UserRes
import ke.co.smartroundclinic.common.DefaultResponse

class UpdateUserUseCase(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(
        userId: String,
        fullName: String?,
        email: String?,
        phoneNumber: String?,
        gender: UserEntity.Gender?,
        dateOfBirth: String?
    ): DefaultResponse<UserRes?>{

        return userRepository.updateUser(
            userId = userId,
            fullName = fullName,
            email = email,
            phoneNumber = phoneNumber,
            gender = gender,
            dateOfBirth = dateOfBirth
        ).toDefaultResponse(HttpStatusCode.OK.value) { it?.toModel()?.toUserRes() }

    }
}