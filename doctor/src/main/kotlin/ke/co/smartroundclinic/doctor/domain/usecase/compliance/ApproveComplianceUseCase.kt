package ke.co.smartroundclinic.doctor.domain.usecase.compliance

import ke.co.smartroundclinic.auth.data.entity.UserEntity
import ke.co.smartroundclinic.auth.domain.repository.UserRepository
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.doctor.domain.model.toModel
import ke.co.smartroundclinic.doctor.domain.repository.ComplianceCorrectionRepository
import ke.co.smartroundclinic.doctor.domain.repository.ComplianceRepository
import ke.co.smartroundclinic.doctor.presentation.dto.response.ComplianceRes
import ke.co.smartroundclinic.doctor.presentation.dto.response.toRes
import ke.co.smartroundclinic.common.NotificationChannel
import ke.co.smartroundclinic.common.PushNotificationEvents
import ke.co.smartroundclinic.common.NotificationDestination
import ke.co.smartroundclinic.common.NotificationSender
import ke.co.smartroundclinic.notification.config.EmailConfig
import ke.co.smartroundclinic.notification.domain.model.EmailWithTemplate
import ke.co.smartroundclinic.notification.domain.model.Template
import ke.co.smartroundclinic.notification.domain.repository.EmailRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory

class ApproveComplianceUseCase(
    private val repository: ComplianceRepository,
    private val emailRepository: EmailRepository,
    private val userRepository: UserRepository,
    private val correctionRepository: ComplianceCorrectionRepository,
    private val notificationSender: NotificationSender? = null,
) {
    private val log = LoggerFactory.getLogger(ApproveComplianceUseCase::class.java)

    suspend operator fun invoke(id: String, adminId: String): DefaultResponse<ComplianceRes?> =
        withContext(Dispatchers.IO) {
            val approve = repository.approve(id, adminId)
            if (approve is Resource.Error) return@withContext approve.toDefaultResponse()
            val doctorId = approve.data?.doctorId ?: return@withContext approve.toDefaultResponse()
            userRepository.adminUpdateUser(doctorId, accountStatus = null, verificationStatus = UserEntity.VerificationStatus.VERIFIED)
            runCatching { correctionRepository.resolvePending(doctorId, "COMPLETED", adminId) }
            val user = userRepository.getUser(doctorId)
            if (user is Resource.Error) return@withContext user.toDefaultResponse()
            sendApplicationApprovedEmail(user.data?.fullName ?: "", user.data?.email ?: "")
            runCatching {
                notificationSender?.send(
                    title = PushNotificationEvents.DOCTOR_APPLICATION_APPROVED,
                    message = "Congratulations! Your doctor application has been approved. You can now receive appointments.",
                    channel = NotificationChannel.PUSH_NOTIFICATION,
                    destination = NotificationDestination.DOCTOR,
                    recipientId = doctorId,
                    metadata = mapOf("event" to PushNotificationEvents.DOCTOR_APPLICATION_APPROVED, "complianceId" to id),
                )
            }
            approve.toDefaultResponse() { approve.data?.toModel()?.toRes() }
        }

    private suspend fun sendApplicationApprovedEmail(name: String, email: String) {
        emailRepository.sendEmailWithTemplate(
            emailWithTemplate = EmailWithTemplate(
                from = EmailConfig().accountVerificationEmail,
                to = email,
                subject = "Account Approved ",
                template = Template(
                    id = EmailConfig().resendDoctorApplicationRequestApprovedTemplateId,
                    variables = mapOf(
                        "NAME" to name,
                    )
                )
            )
        )
    }
}
