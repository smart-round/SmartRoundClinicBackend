package ke.co.smartroundclinic.auth.presentation.dto.request

import ke.co.smartroundclinic.auth.data.entity.UserEntity
import ke.co.smartroundclinic.auth.data.entity.UserEntity.Gender
import ke.co.smartroundclinic.auth.data.entity.UserEntity.Role
import ke.co.smartroundclinic.auth.domain.model.User
import kotlinx.serialization.Serializable

@Serializable
data class SignUpReq(
    val fullName: String,
    val email: String,
    val password: String,
    val gender: Gender = Gender.NON_BINARY,
    val role: Role = Role.PATIENT,
    val phoneNumber: String? = null,
    val dateOfBirth: String? = null,
){
    fun toModel(userRole: Role) = User(
        fullName = fullName,
        email = email,
        password = password,
        gender = gender,
        role = userRole,
        accountStatus = UserEntity.AccountStatus.INACTIVE,
        verificationStatus = UserEntity.VerificationStatus.UNVERIFIED,
        phoneNumber = phoneNumber,
        dateOfBirth = dateOfBirth,

    )
}
