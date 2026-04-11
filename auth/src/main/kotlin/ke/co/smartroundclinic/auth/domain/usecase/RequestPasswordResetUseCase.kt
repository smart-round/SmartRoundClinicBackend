package ke.co.smartroundclinic.auth.domain.usecase

import io.ktor.http.HttpStatusCode
import ke.co.smartroundclinic.auth.domain.repository.CredentialsHasher
import ke.co.smartroundclinic.auth.domain.repository.UserRepository
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.OtpCodeGenerator
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.notification.config.EmailConfig
import ke.co.smartroundclinic.notification.domain.model.EmailWithTemplate
import ke.co.smartroundclinic.notification.domain.model.Template
import ke.co.smartroundclinic.notification.domain.repository.EmailRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes

class RequestPasswordResetUseCase(
    private val userRepository: UserRepository,
    private val credentialsHasher: CredentialsHasher,
    private val emailRepository: EmailRepository,
    private val emailConfig: EmailConfig
) {
    suspend operator fun invoke(email: String): DefaultResponse<Nothing?> = withContext(Dispatchers.IO) {
        supervisorScope {
            val result = userRepository.getUserByEmail(email)
            if (result is Resource.Error) return@supervisorScope result.toDefaultResponse(
                failedStatusCode = HttpStatusCode.NotFound.value,
                errorMessage = "No account found with that email address"
            ) { null }

            val user = result.data ?: return@supervisorScope result.toDefaultResponse(
                failedStatusCode = HttpStatusCode.NotFound.value,
                errorMessage = "No account found with that email address"
            ) { null }

            val otpCode = OtpCodeGenerator().generateOtpCode()
            val hashedOtpCode = credentialsHasher.hash(otpCode)
            val otpExpiresAt = Clock.System.now().plus(15.minutes).toEpochMilliseconds()

            val updateResult = userRepository.updateOtp(
                userId = user.id,
                otpCode = hashedOtpCode,
                otpExpiresAt = otpExpiresAt
            )

            if (updateResult is Resource.Error) return@supervisorScope updateResult.toDefaultResponse(
                failedStatusCode = HttpStatusCode.InternalServerError.value,
                errorMessage = "Failed to initiate password reset"
            ) { null }

            launch {
                emailRepository.sendEmailWithTemplate(
                    emailWithTemplate = EmailWithTemplate(
                        from = emailConfig.passwordResetTemplateId,
                        to = email,
                        subject = "Password Reset Request - SmartRound Clinic",
                        template = Template(
                            id = emailConfig.passwordResetRequestTemplateId,
                            variables = mapOf(
                                "NAME" to user.fullName,
                                "OTP_CODE" to otpCode,
                            )
                        )
                    )
                )
            }

            updateResult.toDefaultResponse(
                successStatusCode = HttpStatusCode.OK.value,
                successMessage = "Password reset OTP sent to $email"
            ) { null }
        }
    }
}