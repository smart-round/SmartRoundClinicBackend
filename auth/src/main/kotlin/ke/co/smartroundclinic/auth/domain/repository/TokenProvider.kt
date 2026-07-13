package ke.co.smartroundclinic.auth.domain.repository

import ke.co.smartroundclinic.auth.data.entity.UserEntity

data class AuthToken(
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val role: String? = null,
    val accountStatus: UserEntity.AccountStatus = UserEntity.AccountStatus.INACTIVE,
    val verificationStatus: UserEntity.VerificationStatus = UserEntity.VerificationStatus.UNVERIFIED,
    val policyGroupIds: List<String> = emptyList(),
    val permissions: List<String> = emptyList(),
)

interface TokenProvider {
    fun generateTokens(userId: String, role: String, permissions: List<String> = emptyList()): AuthToken
    fun verifyRefreshToken(token: String): String?
}
