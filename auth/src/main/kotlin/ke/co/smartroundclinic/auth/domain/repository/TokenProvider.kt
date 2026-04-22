package ke.co.smartroundclinic.auth.domain.repository


data class AuthToken(
    val accessToken: String,
    val refreshToken: String,
    val policyGroupIds: List<String> = emptyList(),
    val permissions: List<String> = emptyList(),
)

interface TokenProvider {
    fun generateTokens(userId: String, role: String, permissions: List<String> = emptyList()): AuthToken
    fun verifyRefreshToken(token: String): String?
}
