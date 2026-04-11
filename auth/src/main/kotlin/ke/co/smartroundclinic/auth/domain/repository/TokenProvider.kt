package ke.co.smartroundclinic.auth.domain.repository


data class AuthToken(
    val accessToken: String,
    val refreshToken: String
)

interface TokenProvider {
    fun generateTokens(userId: String, role: String): AuthToken
    fun verifyRefreshToken(token: String): String? // returns userId if valid
}
