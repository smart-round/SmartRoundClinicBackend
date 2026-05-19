package ke.co.smartroundclinic.doctor.domain.usecase.compliance

import io.ktor.http.HttpStatusCode
import ke.co.smartroundclinic.auth.data.entity.UserEntity
import ke.co.smartroundclinic.auth.domain.repository.UserRepository
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.doctor.domain.model.toModel
import ke.co.smartroundclinic.doctor.domain.repository.ComplianceRepository
import ke.co.smartroundclinic.doctor.presentation.dto.response.ComplianceRes
import ke.co.smartroundclinic.doctor.presentation.dto.response.toRes
import ke.co.smartroundclinic.infra.AppConfig
import ke.co.smartroundclinic.notification.config.EmailConfig
import ke.co.smartroundclinic.notification.domain.model.EmailWithTemplate
import ke.co.smartroundclinic.notification.domain.model.Template
import ke.co.smartroundclinic.notification.domain.repository.EmailRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SubmitComplianceUseCase(
    private val repository: ComplianceRepository,
    private val complianceCheckUseCase: ComplianceCheckUseCase,
    private val emailRepository: EmailRepository,
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(doctorId: String): DefaultResponse<ComplianceRes?> = withContext(Dispatchers.IO) {
        val check = complianceCheckUseCase(doctorId)
        if (check is Resource.Error) return@withContext check.toDefaultResponse()
        val user = userRepository.getUser(doctorId)
        if (user is Resource.Error) return@withContext user.toDefaultResponse()
        userRepository.adminUpdateUser(doctorId, accountStatus = null, verificationStatus = UserEntity.VerificationStatus.PENDING_APPROVAL)
        sendApplicationReceivedEmail(user.data?.fullName ?: "",user.data?.email ?: "",doctorId)
        repository.submit(doctorId).toDefaultResponse(
            successStatusCode = HttpStatusCode.Created.value,
            failedStatusCode = HttpStatusCode.Conflict.value,
            successMessage = "Submitted for review successfully",
        ) { it?.toModel()?.toRes() }
    }

    private suspend fun sendApplicationReceivedEmail(name:String,email:String,doctorId: String) {
        emailRepository.sendEmailWithTemplate(
            emailWithTemplate = EmailWithTemplate(
                from = EmailConfig().accountVerificationEmail,
                to = email,
                subject = "Practitioner Application Request Received",
                template = Template(
                    id = EmailConfig().resendDoctorApplicationRequestTemplateId,
                    variables = mapOf(
                        "NAME" to name,
                    )
                )
            )
        )
    }

}
