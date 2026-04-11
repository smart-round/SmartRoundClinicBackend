package ke.co.smartroundclinic.auth.domain.model

import ke.co.smartroundclinic.auth.data.entity.UserEntity
import ke.co.smartroundclinic.auth.data.entity.UserEntity.Gender
import ke.co.smartroundclinic.auth.data.entity.UserEntity.Role
import ke.co.smartroundclinic.auth.presentation.dto.response.UserRes


data class User(
    val id: String = "",
    val fullName: String,
    val email: String,
    val gender: Gender,
    val role: Role = Role.PATIENT,
    val accountStatus: UserEntity.AccountStatus,
    val verificationStatus: UserEntity.VerificationStatus,
    val password: String,
    val phoneNumber: String?,
    val profilePicture: String? = null,
    val dateOfBirth: String?,
    val createdAt: String = "",
    val updatedAt: String? = null,
){
    fun toEntity() = UserEntity(
        fullName = fullName,
        email = email,
        gender = gender,
        role = role,
        accountStatus = accountStatus,
        verificationStatus = verificationStatus,
        phoneNumber = phoneNumber,
        dateOfBirth = dateOfBirth,
        password = password,
        profilePicture = profilePicture

    )
    fun toUserRes() = UserRes(
        id = id,
        fullName = fullName,
        email = email,
        gender = gender,
        role = role,
        accountStatus = accountStatus,
        verificationStatus = verificationStatus,
        phoneNumber = phoneNumber,
        dateOfBirth = dateOfBirth,
        createdAt = createdAt,
        updatedAt = updatedAt,
        profilePicture = profilePicture
    )
}