package ke.co.smartroundclinic.auth.presentation.dto.request

import ke.co.smartroundclinic.auth.data.entity.UserEntity
import ke.co.smartroundclinic.auth.data.entity.UserEntity.Gender
import ke.co.smartroundclinic.auth.data.entity.UserEntity.Role
import ke.co.smartroundclinic.auth.domain.model.User

data class CreateAdminReq(
    val fullName: String,
    val email: String,
    val gender: Gender = Gender.NON_BINARY,
    val password: String,
    val phoneNumber: String? = null,
    val dateOfBirth: String? = null,
){
    fun toModel() = User(
        fullName = fullName,
        email = email,
        gender = gender,
        role = Role.ADMIN,
        accountStatus = UserEntity.AccountStatus.ACTIVE,
        verificationStatus = UserEntity.VerificationStatus.VERIFIED,
        password = password,
        phoneNumber = phoneNumber,
        dateOfBirth = dateOfBirth,
    )
}