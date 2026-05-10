package ke.co.smartroundclinic.auth.presentation.dto.request

import ke.co.smartroundclinic.auth.data.entity.UserEntity

data class UpdateUserReq(
    val fullName: String?,
    val email: String?,
    val phoneNumber: String?,
    val gender: UserEntity.Gender?,
    val dateOfBirth: String?
)