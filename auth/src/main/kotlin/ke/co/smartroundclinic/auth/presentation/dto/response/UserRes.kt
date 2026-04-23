package ke.co.smartroundclinic.auth.presentation.dto.response

import ke.co.smartroundclinic.auth.data.entity.UserEntity
import ke.co.smartroundclinic.auth.data.entity.UserEntity.Gender
import ke.co.smartroundclinic.auth.data.entity.UserEntity.Role
import ke.co.smartroundclinic.common.PatientProfile

data class UserRes(
    val id: String,
    val fullName: String,
    val email: String,
    val gender: Gender,
    val role: Role = Role.PATIENT,
    val accountStatus: UserEntity.AccountStatus,
    val verificationStatus: UserEntity.VerificationStatus,
    val phoneNumber: String?,
    val dateOfBirth: String?,
    val profilePicture: String?,
    val createdAt: String,
    val updatedAt: String?,
    val personalInfo: PatientProfile? = null,
)
