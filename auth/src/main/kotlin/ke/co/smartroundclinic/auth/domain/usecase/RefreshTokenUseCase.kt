package ke.co.smartroundclinic.auth.domain.usecase

import io.ktor.http.HttpStatusCode
import ke.co.smartroundclinic.auth.domain.repository.AuthToken
import ke.co.smartroundclinic.auth.domain.repository.TokenProvider
import ke.co.smartroundclinic.auth.domain.repository.TokenRepository
import ke.co.smartroundclinic.auth.domain.repository.UserRepository
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RefreshTokenUseCase(
    private val tokenProvider: TokenProvider,
    private val tokenRepository: TokenRepository,
    private val userRepository: UserRepository,
) {
    suspend operator fun invoke(refreshToken: String): DefaultResponse<AuthToken?> =
        withContext(Dispatchers.IO) {
            val userId = tokenProvider.verifyRefreshToken(refreshToken)
                ?: return@withContext Resource.Error<AuthToken?>(message = "Invalid or expired refresh token")
                    .toDefaultResponse(failedStatusCode = HttpStatusCode.Unauthorized.value) { null }

            if (tokenRepository.isRevoked(refreshToken)) {
                return@withContext Resource.Error<AuthToken?>(message = "Token has been revoked")
                    .toDefaultResponse(failedStatusCode = HttpStatusCode.Unauthorized.value) { null }
            }

            val userResult = userRepository.getUser(userId)
            val user = userResult.data
                ?: return@withContext Resource.Error<AuthToken?>(message = "User not found")
                    .toDefaultResponse(failedStatusCode = HttpStatusCode.Unauthorized.value) { null }

            val newTokens = tokenProvider.generateTokens(userId = user.id, role = user.role.name)
                .copy(
                    accountStatus = user.accountStatus,
                    verificationStatus = user.verificationStatus,
                )
            Resource.Success(data = newTokens, message = "Token refreshed successfully")
                .toDefaultResponse(successStatusCode = HttpStatusCode.OK.value) { it }
        }
}