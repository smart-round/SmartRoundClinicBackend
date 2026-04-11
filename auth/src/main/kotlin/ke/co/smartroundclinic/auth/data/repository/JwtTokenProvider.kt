package ke.co.smartroundclinic.auth.data.repository

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import ke.co.smartroundclinic.auth.domain.repository.AuthToken
import ke.co.smartroundclinic.auth.domain.repository.TokenProvider
import java.util.*

class JwtTokenProvider : TokenProvider {
    private val jwtAudience = System.getenv("JWT_AUDIENCE")?.trim() ?: "jwt-audience"
    private val jwtDomain = System.getenv("JWT_DOMAIN")?.trim() ?: "https://jwt-provider-domain/"
    private val jwtSecret = System.getenv("JWT_SECRET")?.trim() ?: "secret"
    private val refreshSecret = System.getenv("REFRESH_SECRET")?.trim() ?: "refresh-secret"

    override fun generateTokens(userId: String, role: String): AuthToken {
        val accessToken = JWT.create()
            .withAudience(jwtAudience)
            .withIssuer(jwtDomain)
            .withClaim("userId", userId)
            .withClaim("role", role)
            .withExpiresAt(Date(System.currentTimeMillis() + 3600000 * 24)) // 24 hours
            .sign(Algorithm.HMAC256(jwtSecret))

        val refreshToken = JWT.create()
            .withAudience(jwtAudience)
            .withIssuer(jwtDomain)
            .withClaim("userId", userId)
            .withExpiresAt(Date(System.currentTimeMillis() + 3600000L * 24 * 30)) // 30 days
            .sign(Algorithm.HMAC256(refreshSecret))

        return AuthToken(accessToken, refreshToken)
    }

    override fun verifyRefreshToken(token: String): String? {
        return try {
            val verifier = JWT
                .require(Algorithm.HMAC256(refreshSecret))
                .withAudience(jwtAudience)
                .withIssuer(jwtDomain)
                .build()
            val decodedJWT = verifier.verify(token)
            decodedJWT.getClaim("userId").asString()
        } catch (e: Exception) {
            null
        }
    }
}
