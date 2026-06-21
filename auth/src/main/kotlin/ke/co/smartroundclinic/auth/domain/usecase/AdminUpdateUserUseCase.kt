package ke.co.smartroundclinic.auth.domain.usecase

import io.ktor.http.HttpStatusCode
import ke.co.smartroundclinic.auth.data.entity.UserEntity
import ke.co.smartroundclinic.auth.domain.repository.UserRepository
import ke.co.smartroundclinic.auth.presentation.dto.response.UserRes
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.NotificationChannel
import ke.co.smartroundclinic.common.NotificationDestination
import ke.co.smartroundclinic.common.NotificationSender
import ke.co.smartroundclinic.common.PushNotificationEvents
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.notification.config.EmailConfig
import ke.co.smartroundclinic.notification.domain.model.EmailWithTemplate
import ke.co.smartroundclinic.notification.domain.model.Template
import ke.co.smartroundclinic.notification.domain.repository.EmailRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

class AdminUpdateUserUseCase(
    private val repository: UserRepository,
    private val emailRepository: EmailRepository,
    private val emailConfig: EmailConfig,
    private val notificationSender: NotificationSender? = null,
) {
    suspend operator fun invoke(
        userId: String,
        accountStatus: UserEntity.AccountStatus?,
        verificationStatus: UserEntity.VerificationStatus?,
    ): DefaultResponse<UserRes?> = supervisorScope {
        val result = repository.adminUpdateUser(userId, accountStatus, verificationStatus)
        if (result is Resource.Success && accountStatus == UserEntity.AccountStatus.SUSPENDED) {
            result.data?.let { user ->
                launch {
                    runCatching {
                        notificationSender?.send(
                            title = PushNotificationEvents.ACCOUNT_SUSPENDED,
                            message = "Your account has been suspended. Please contact support.",
                            channel = NotificationChannel.PUSH_NOTIFICATION,
                            destination = user.role.toNotificationDestination(),
                            recipientId = user.id,
                            metadata = mapOf("event" to PushNotificationEvents.ACCOUNT_SUSPENDED),
                        )
                    }
                    runCatching {
                        emailRepository.sendEmailWithTemplate(
                            EmailWithTemplate(
                                from = emailConfig.suspendUserEmail,
                                to = user.email,
                                template = Template(
                                    id = emailConfig.suspendUserTemplateId,
                                    variables = mapOf("NAME" to user.fullName),
                                )
                            )
                        )
                    }
                }
            }
        }
        result.toDefaultResponse(failedStatusCode = HttpStatusCode.NotFound.value) { it?.toModel()?.toUserRes() }
    }
}

private fun UserEntity.Role.toNotificationDestination() = when (this) {
    UserEntity.Role.PATIENT -> NotificationDestination.PATIENT
    UserEntity.Role.DOCTOR -> NotificationDestination.DOCTOR
    UserEntity.Role.ADMIN, UserEntity.Role.SUPER_ADMIN -> NotificationDestination.ADMIN
}
