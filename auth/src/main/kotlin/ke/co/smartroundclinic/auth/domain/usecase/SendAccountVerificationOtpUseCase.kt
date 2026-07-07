package ke.co.smartroundclinic.auth.domain.usecase

import ke.co.smartroundclinic.notification.config.EmailConfig
import ke.co.smartroundclinic.notification.domain.model.EmailWithTemplate
import ke.co.smartroundclinic.notification.domain.model.Template
import ke.co.smartroundclinic.notification.domain.repository.EmailRepository

class SendAccountVerificationOtpUseCase(
    private val emailRepository: EmailRepository,
    private val emailConfig: EmailConfig
) {
    suspend  operator fun invoke(fullName: String, toEmail: String, otpCode: String) {
        emailRepository.sendEmailWithTemplate(
            emailWithTemplate = EmailWithTemplate(
                from = emailConfig.accountVerificationEmail,
                to = toEmail,
                template = Template(
                    id = emailConfig.accountVerificationTemplateId,
                    variables = mapOf(
                        "NAME" to fullName,
                        "OTP_CODE" to otpCode,
                    )
                )
            )
        )
    }
}