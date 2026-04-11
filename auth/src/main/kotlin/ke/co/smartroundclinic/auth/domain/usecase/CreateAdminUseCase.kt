package ke.co.smartroundclinic.auth.domain.usecase

import ke.co.smartroundclinic.auth.domain.model.User
import ke.co.smartroundclinic.auth.domain.repository.UserRepository
import ke.co.smartroundclinic.auth.presentation.dto.response.UserRes
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.notification.config.EmailConfig
import ke.co.smartroundclinic.notification.domain.model.EmailWithTemplate
import ke.co.smartroundclinic.notification.domain.model.Template
import ke.co.smartroundclinic.notification.domain.repository.EmailRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory

class CreateAdminUseCase(
    private val userRepository: UserRepository,
    private val emailRepository: EmailRepository,
    private val emailConfig: EmailConfig
) {
    private val logger = LoggerFactory.getLogger(CreateAdminUseCase::class.java)

    suspend operator fun invoke(user: User): DefaultResponse<UserRes?> {
        return when (val createUser = userRepository.create(user.toEntity())) {
            is Resource.Success -> {
                val userRes = createUser.data?.toModel()?.toUserRes()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        sendEmailNotification(
                            fullName = user.fullName,
                            toEmail = user.email,
                            password = user.password
                        )
                    } catch (e: Exception) {
                        logger.error("Failed to send admin creation email", e)
                    }
                }
                DefaultResponse(
                    httpStatusCode = 201,
                    status = true,
                    message = createUser.message ?: "Admin account created successfully",
                    data = userRes
                )
            }

            is Resource.Error -> {
                DefaultResponse(
                    httpStatusCode = 400,
                    status = false,
                    message = createUser.message ?: "Failed to create Admin account"
                )
            }
        }
    }

    private suspend fun sendEmailNotification(fullName: String, toEmail: String, password: String) {
        emailRepository.sendEmailWithTemplate(
            emailWithTemplate = EmailWithTemplate(
                from = emailConfig.onboardingEmail,
                to = toEmail,
                subject = "Welcome to Smart Round Clinic",
                template = Template(
                    id = emailConfig.onboardingTemplateId,
                    variables = mapOf(
                        "ADMIN_NAME" to fullName,
                        "ADMIN_EMAIL" to toEmail,
                        "ADMIN_PASSWORD" to password
                    )
                )
            )
        )
    }
}
