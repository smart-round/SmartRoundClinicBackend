package ke.co.smartroundclinic.auth.domain.usecase

import io.ktor.http.HttpStatusCode
import ke.co.smartroundclinic.auth.domain.repository.UserRepository
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.notification.config.EmailConfig
import ke.co.smartroundclinic.notification.domain.model.EmailWithTemplate
import ke.co.smartroundclinic.notification.domain.model.Template
import ke.co.smartroundclinic.notification.domain.repository.EmailRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext

class ResetPasswordUseCase(
    private val userRepository: UserRepository,
    private val verifyAccountOtpUseCase: VerifyAccountOtpUseCase,
    private val emailRepository: EmailRepository,
    private val emailConfig: EmailConfig
) {
    suspend operator fun invoke(email: String, otpCode: String, newPassword: String): DefaultResponse<Nothing?> =
        withContext(Dispatchers.IO) {
            supervisorScope {

                val userResult = userRepository.getUserByEmail(email)
                if (userResult is Resource.Error) return@supervisorScope userResult.toDefaultResponse(
                    failedStatusCode = HttpStatusCode.NotFound.value,
                    errorMessage = "No account found with that email address"
                ) { null }


                val user = userResult.data ?: return@supervisorScope userResult.toDefaultResponse(
                    failedStatusCode = HttpStatusCode.NotFound.value,
                    errorMessage = "No account found with that email address"
                ) { null }

                when (val otpVerification = verifyAccountOtpUseCase(email, otpCode)) {
                    is Resource.Error -> otpVerification.toDefaultResponse(
                        failedStatusCode = HttpStatusCode.OK.value,
                    ) { null }

                    is Resource.Success -> {
                        val updateResult = userRepository.updatePassword(
                            userId = user.id,
                            newPassword = newPassword
                        )

                        if (updateResult is Resource.Error) return@supervisorScope updateResult.toDefaultResponse(
                            failedStatusCode = HttpStatusCode.InternalServerError.value,
                            errorMessage = "Failed to reset password"
                        ) { null }

                        launch {
                            emailRepository.sendEmailWithTemplate(
                                emailWithTemplate = EmailWithTemplate(
                                    from = emailConfig.passwordResetTemplateId,
                                    to = email,
                                    subject = "Password Reset Successful - SmartRound Clinic",
                                    template = Template(
                                        id = emailConfig.passwordResetConfirmationTemplateId,
                                        variables = mapOf("NAME" to user.fullName)
                                    )
                                )
                            )
                        }

                        updateResult.toDefaultResponse(
                            successStatusCode = HttpStatusCode.OK.value,
                            successMessage = "Password reset successfully"
                        ) { null }
                    }
                }
            }
        }
}