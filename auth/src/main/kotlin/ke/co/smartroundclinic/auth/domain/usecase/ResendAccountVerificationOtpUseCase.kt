package ke.co.smartroundclinic.auth.domain.usecase

import io.ktor.http.HttpStatusCode
import ke.co.smartroundclinic.auth.domain.repository.CredentialsHasher
import ke.co.smartroundclinic.auth.domain.repository.UserRepository
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.OtpCodeGenerator
import ke.co.smartroundclinic.common.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes

class ResendAccountVerificationOtpUseCase(
    private val userRepository: UserRepository,
    private val credentialsHasher: CredentialsHasher,
    private val sendAccountVerificationOtpUseCase: SendAccountVerificationOtpUseCase
) {
    suspend operator fun invoke(email: String): DefaultResponse<Nothing?> = withContext(Dispatchers.IO) {
        supervisorScope {
            val result = userRepository.getUserByEmail(email)
            if (result is Resource.Error) return@supervisorScope result.toDefaultResponse(
                failedStatusCode = HttpStatusCode.NotFound.value, errorMessage = "User not found"
            ) { null }

            val user = result.data ?: return@supervisorScope result.toDefaultResponse(
                failedStatusCode = HttpStatusCode.NotFound.value, errorMessage = "User not found"
            ) { null }

            if (user.otpExpiresAt == null) return@supervisorScope result.toDefaultResponse(
                failedStatusCode = HttpStatusCode.NotFound.value, errorMessage = "User not found"
            )

            if (Clock.System.now()
                    .toEpochMilliseconds() < (user.otpExpiresAt)
            ) return@supervisorScope result.toDefaultResponse(
                failedStatusCode = HttpStatusCode.OK.value, errorMessage = "OTP has not expired yet. Please try again"
            ) { null }

            val otpCode = OtpCodeGenerator().generateOtpCode()
            val hashedOtpCode = credentialsHasher.hash(otpCode)
            val otpExpiresAt = Clock.System.now().plus(2.minutes).toEpochMilliseconds()

            userRepository.updateOtp(
                userId = user.id, otpCode = hashedOtpCode, otpExpiresAt = otpExpiresAt
            ).let { updateResult ->

                if (updateResult is Resource.Error) return@let updateResult.toDefaultResponse(
                    failedStatusCode = HttpStatusCode.InternalServerError.value,
                    errorMessage = "Failed to send otp"
                ) { null }

                launch {
                    sendAccountVerificationOtpUseCase(
                        fullName = user.fullName,
                        toEmail = user.email,
                        otpCode = otpCode
                    )
                }

                return@supervisorScope updateResult.toDefaultResponse(
                    successStatusCode = HttpStatusCode.OK.value,
                    successMessage = "OTP sent successfully"
                ) { null }

            }
        }
    }
}