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
    suspend fun emailSignIn(email: String, password: String): Resource<AuthToken?>
}