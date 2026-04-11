package ke.co.smartroundclinic.auth.domain.usecase

import io.ktor.http.HttpStatusCode
import ke.co.smartroundclinic.auth.domain.repository.TokenProvider
import ke.co.smartroundclinic.auth.domain.repository.TokenRepository
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Date

class RevokeTokenUseCase(
    private val tokenProvider: TokenProvider,
    private val tokenRepository: TokenRepository,
) {
    suspend operator fun invoke(refreshToken: String): DefaultResponse<Nothing?> =
        withContext(Dispatchers.IO) {
            tokenProvider.verifyRefreshToken(refreshToken)
                ?: return@withContext Resource.Error<Nothing?>(message = "Invalid or expired refresh token")
                    .toDefaultResponse(failedStatusCode = HttpStatusCode.BadRequest.value) { null }

            // Refresh tokens expire in 30 days — store the same TTL so the blocklist self-prunes
            val expiresAt = System.currentTimeMillis() + 3600000L * 24 * 30

            tokenRepository.revokeToken(token = refreshToken, expiresAt = expiresAt)
                .toDefaultResponse(
                    successStatusCode = HttpStatusCode.OK.value,
                    successMessage = "Token revoked successfully",
                    failedStatusCode = HttpStatusCode.InternalServerError.value,
                    errorMessage = "Failed to revoke token"
                ) { null }
        }
}
