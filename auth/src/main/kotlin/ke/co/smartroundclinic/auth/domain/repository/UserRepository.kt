package ke.co.smartroundclinic.auth.domain.repository

import ke.co.smartroundclinic.auth.data.entity.UserEntity
import ke.co.smartroundclinic.common.Resource

interface UserRepository {
    suspend fun create(user: UserEntity): Resource<UserEntity?>
    suspend fun updateUser(
        userId:String,
        fullName:String? = null,
        email:String? = null,
        phoneNumber:String? = null,
        gender: UserEntity.Gender? = null
    ): Resource<UserEntity?>
    suspend fun getUser(userId:String): Resource<UserEntity?>
    suspend fun getUserByEmail(email:String): Resource<UserEntity?>
    suspend fun updateAccountVerificationStatus(email:String): Resource<UserEntity?>
    suspend fun updateOtp(userId:String, otpCode:String, otpExpiresAt:Long): Resource<Nothing>
    suspend fun updatePassword(userId:String,newPassword:String): Resource<Nothing>
    suspend fun updateProfilePicture(userId: String, url: String?): Resource<Nothing>
    suspend fun emailSignIn(email: String, password: String): Resource<AuthToken?>
    suspend fun getUsersByRole(
        role: UserEntity.Role,
        page: Int,
        size: Int,
        search: String?,
    ): Resource<Pair<List<UserEntity>, Long>>
    suspend fun adminUpdateUser(
        userId: String,
        accountStatus: UserEntity.AccountStatus?,
        verificationStatus: UserEntity.VerificationStatus?,
    ): Resource<UserEntity?>
    suspend fun filterUsers(
        role: UserEntity.Role,
        page: Int,
        size: Int,
        accountStatus: UserEntity.AccountStatus?,
        createdFrom: String?,
        createdTo: String?,
    ): Resource<Pair<List<UserEntity>, Long>>
    suspend fun updateAdminPolicyGroup(userId: String, policyGroupId: String?): Resource<Nothing>
}