package ke.co.smartroundclinic.auth.data.repository

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import ke.co.smartroundclinic.auth.domain.repository.AuthToken
import ke.co.smartroundclinic.auth.domain.repository.TokenProvider
import ke.co.smartroundclinic.infra.EnvLoader
import java.util.*

class JwtTokenProvider : TokenProvider {

    private fun require(key: String): String =
        EnvLoader.get(key) ?: throw IllegalStateException("$key is required")

    val jwtAudience = require("JWT_AUDIENCE")
    val jwtDomain = require("JWT_DOMAIN")
    val jwtSecret = require("JWT_SECRET")
    private val refreshSecret = require("REFRESH_SECRET")

    override fun generateTokens(userId: String, role: String, permissions: List<String>): AuthToken {
        val accessToken = JWT.create()
            .withAudience(jwtAudience)
            .withIssuer(jwtDomain)
            .withClaim("userId", userId)
            .withClaim("role", role)
            .withClaim("permissions", permissions.joinToString(","))
            .withExpiresAt(Date(System.currentTimeMillis() + 3600000 * 24)) // 24 hours
            .sign(Algorithm.HMAC256(jwtSecret))

        val refreshToken = JWT.create()
            .withAudience(jwtAudience)
            .withIssuer(jwtDomain)
            .withClaim("userId", userId)
            .withExpiresAt(Date(System.currentTimeMillis() + 3600000L * 24 * 30)) // 30 days
            .sign(Algorithm.HMAC256(refreshSecret))

        return AuthToken(accessToken, refreshToken, role = role)
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
