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

class RejectComplianceUseCase(
    private val repository: ComplianceRepository,
    private val emailRepository: EmailRepository,
    private val userRepository: UserRepository,
    private val correctionRepository: ComplianceCorrectionRepository,
    private val notificationSender: NotificationSender? = null,
) {
    suspend operator fun invoke(id: String, adminId: String, reason: String): DefaultResponse<ComplianceRes?> = withContext(
        Dispatchers.IO) {
        val rejected = repository.reject(id, adminId, reason)
        if (rejected is Resource.Error) return@withContext rejected.toDefaultResponse()
        val doctorId = rejected.data?.doctorId ?: return@withContext rejected.toDefaultResponse()
        userRepository.adminUpdateUser(doctorId, accountStatus = null, verificationStatus = UserEntity.VerificationStatus.REJECTED)
        runCatching { correctionRepository.resolvePending(doctorId, "REJECTED", adminId) }
        val user = userRepository.getUser(doctorId)
        if (user is Resource.Error) return@withContext user.toDefaultResponse()
        sendApplicationRejectEmail(user.data?.fullName ?: "", user.data?.email ?: "", reason)
        runCatching {
            notificationSender?.send(
                title = PushNotificationEvents.DOCTOR_APPLICATION_REJECTED,
                message = "Your doctor application was not approved. Please review the feedback and resubmit.",
                channel = NotificationChannel.PUSH_NOTIFICATION,
                destination = NotificationDestination.DOCTOR,
                recipientId = doctorId,
                metadata = mapOf("event" to PushNotificationEvents.DOCTOR_APPLICATION_REJECTED, "complianceId" to id),
            )
        }
        rejected.toDefaultResponse(
            successStatusCode = 200,
            successMessage = "Rejected successfully",
        ){rejected.data?.toModel()?.toRes()}
    }

    private suspend fun sendApplicationRejectEmail(name: String, email: String, reason: String) {
        emailRepository.sendEmailWithTemplate(
            emailWithTemplate = EmailWithTemplate(
                from = EmailConfig().accountVerificationEmail,
                to = email,
                subject = "Application Unsuccessful",
                template = Template(
                    id = EmailConfig().resendDoctorApplicationRequestRejectedTemplateId,
                    variables = mapOf(
                        "NAME" to name,
                        "REJECTION_REASON" to reason
                    )
                )
            )
        )
    }

}