package ke.co.smartroundclinic.doctor.domain.usecase.compliance

import io.ktor.http.HttpStatusCode
import ke.co.smartroundclinic.auth.domain.repository.UserRepository
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.doctor.domain.model.toModel
import ke.co.smartroundclinic.doctor.domain.repository.ComplianceRepository
import ke.co.smartroundclinic.doctor.presentation.dto.response.ComplianceRes
import ke.co.smartroundclinic.doctor.presentation.dto.response.toRes
import ke.co.smartroundclinic.notification.config.EmailConfig
import ke.co.smartroundclinic.notification.domain.model.EmailWithTemplate
import ke.co.smartroundclinic.notification.domain.model.Template
import ke.co.smartroundclinic.notification.domain.repository.EmailRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ApproveComplianceUseCase(
    private val repository: ComplianceRepository,
    private val emailRepository: EmailRepository,
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(id: String, adminId: String): DefaultResponse<ComplianceRes?> =
        withContext(Dispatchers.IO) {
            val approve = repository.approve(id, adminId)
            if (approve is Resource.Error) return@withContext approve.toDefaultResponse()
            val user = approve.data?.doctorId?.let { userRepository.getUser(it) }
            if (user is Resource.Error) return@withContext user.toDefaultResponse()
            sendApplicationApprovedEmail(user?.data?.fullName ?: "",user?.data?.email ?: "")
            approve.toDefaultResponse(){ approve.data?.toModel()?.toRes() }
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
