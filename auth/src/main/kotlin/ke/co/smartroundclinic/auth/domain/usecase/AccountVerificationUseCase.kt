package ke.co.smartroundclinic.auth.domain.usecase

import io.ktor.http.HttpStatusCode
import ke.co.smartroundclinic.auth.data.entity.UserEntity
import ke.co.smartroundclinic.auth.domain.repository.UserRepository
import ke.co.smartroundclinic.auth.presentation.dto.response.UserRes
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

class AccountVerificationUseCase(
    private val userRepository: UserRepository,
    private val verifyAccountOtpUseCase: VerifyAccountOtpUseCase,
    private val emailRepository: EmailRepository,
    private val emailConfig: EmailConfig
) {
    suspend operator fun invoke(email: String, otpCode: String): DefaultResponse<UserRes?>  = withContext(Dispatchers.IO) {
        supervisorScope {
            when (val otpVerification = verifyAccountOtpUseCase(email, otpCode)) {
                is Resource.Error -> otpVerification.toDefaultResponse(
                    failedStatusCode = HttpStatusCode.OK.value,
                ) { null }

                is Resource.Success -> {
                    userRepository.updateAccountVerificationStatus(email).toDefaultResponse(
                        successStatusCode = HttpStatusCode.OK.value,
                        successMessage = "Account verification successful"
                    ) {userEntity ->
                        userEntity?.toModel()?.let { user ->
                            launch {
                                sendAccountVerificationEmail(
                                    role = user.role,
                                    fullName = user.fullName,
                                    toEmail = user.email
                                )
                            }
                            user.toUserRes()
                        }
                    }
                }
            }
        }
    }

    private suspend fun sendAccountVerificationEmail(role: UserEntity.Role, fullName: String, toEmail: String) {
        when(role){
            UserEntity.Role.DOCTOR -> {
                emailRepository.sendEmailWithTemplate(
                    emailWithTemplate = EmailWithTemplate(
                        from = emailConfig.accountVerificationEmail,
                        to = toEmail,
                        subject = "Account Pending Approval",
                        template = Template(
                            id = emailConfig.doctorAccountVerificationSuccessTemplateId,
                            variables = mapOf(
                                "NAME" to fullName,
                            )
                        )
                    )
                )
            }
            UserEntity.Role.PATIENT -> {
                emailRepository.sendEmailWithTemplate(
                    emailWithTemplate = EmailWithTemplate(
                        from = emailConfig.accountVerificationEmail,
                        to = toEmail,
                        subject = "Welcome to SmartRound Clinic",
                        template = Template(
                            id = emailConfig.patientAccountVerificationSuccessTemplateId,
                            variables = mapOf(
                                "NAME" to fullName,
                            )
                        )
                    )
                )
            }
            else -> {}
        }

    }
}