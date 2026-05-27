package ke.co.smartroundclinic.auth.data.entity

import ke.co.smartroundclinic.auth.domain.model.User
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId
import kotlin.time.Clock

data class UserEntity(
    val id: String = ObjectId().toString(),
    val fullName: String,
    val email: String,
    val password: String,
    val gender: Gender = Gender.NON_BINARY,
    val role: Role = Role.PATIENT,
    val accountStatus: AccountStatus = AccountStatus.ACTIVE,
    val verificationStatus: VerificationStatus = VerificationStatus.UNVERIFIED,
    val kraPin: String? = null,
    val otpCode: String? = null,
    val otpExpiresAt: Long? = null, // epoch milliseconds
    val phoneNumber: String? = null,
    val dateOfBirth: String? = null,
    val profilePicture: String? = null,
    val policyGroupIds: List<String>? = emptyList(),
    val createdAt: String = Clock.System.now().toString(),
    val updatedAt: String? = null,
) {

    enum class Gender {
        MALE, FEMALE, NON_BINARY, OTHER
    }
    enum class Role{
        SUPER_ADMIN, ADMIN, PATIENT, DOCTOR
    }
    enum class AccountStatus{
        ACTIVE,INACTIVE,SUSPENDED
    }
    enum class VerificationStatus{
        VERIFIED, UNVERIFIED, PENDING_APPROVAL, REJECTED
    }

    fun toModel(): User  = User(
        id = id,
        fullName = fullName,
        email = email,
        gender = gender,
        role = role,
        accountStatus = accountStatus,
        verificationStatus = verificationStatus,
        kraPin = kraPin,
        password = password,
        phoneNumber = phoneNumber,
        dateOfBirth = dateOfBirth,
        policyGroupIds = policyGroupIds ?: emptyList(),
        createdAt = createdAt,
        updatedAt = updatedAt,
        profilePicture = profilePicture
    )

    companion object{
        val allRoles = Role.entries.map { it.name }.toTypedArray()

    }
}
