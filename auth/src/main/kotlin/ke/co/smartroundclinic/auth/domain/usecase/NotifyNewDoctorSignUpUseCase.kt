package ke.co.smartroundclinic.auth.domain.usecase

import ke.co.smartroundclinic.notification.config.EmailConfig
import ke.co.smartroundclinic.notification.domain.model.EmailWithTemplate
import ke.co.smartroundclinic.notification.domain.model.Template
import ke.co.smartroundclinic.notification.domain.repository.EmailRepository
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val NAIROBI = ZoneId.of("Africa/Nairobi")
private val SIGNUP_DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")

class NotifyNewDoctorSignUpUseCase(
    private val emailRepository: EmailRepository,
    private val emailConfig: EmailConfig,
) {
    private val logger = LoggerFactory.getLogger(NotifyNewDoctorSignUpUseCase::class.java)

    suspend operator fun invoke(fullName: String, email: String, doctorId: String, createdAt: String) {
        runCatching {
            val lastName = fullName.trim().split(" ").last()
            val signupDate = formatNairobiDate(createdAt)
            emailRepository.sendEmailWithTemplate(
                EmailWithTemplate(
                    from = emailConfig.accountVerifyEmail,
                    to = emailConfig.accountSupportMailboxDestination,
                    template = Template(
                        id = emailConfig.newDoctorSignUpTemplateId,
                        variables = mapOf(
                            "DOCTOR_LAST_NAME" to lastName,
                            "DOCTOR_FULL_NAME" to fullName,
                            "DOCTOR_EMAIL" to email,
                            "SPECIALTY" to "Not yet assigned",
                            "SIGNUP_DATE" to signupDate,
                            "DOCTOR_ID" to doctorId,
                            "SENDER_NAME" to "Smartround Clinic",
                            "SENDER_EMAIL" to emailConfig.accountVerifyEmail,
                        ),
                    ),
                )
            )
        }.onFailure { e ->
            logger.error(
                "Failed to send new doctor signup notification " +
                    "[templateId=${emailConfig.newDoctorSignUpTemplateId}, " +
                    "to=${emailConfig.accountSupportMailboxDestination}, event=NewDoctorSignUp]",
                e,
            )
        }
    }

    private fun formatNairobiDate(isoInstant: String): String = try {
        SIGNUP_DATE_FMT.format(Instant.parse(isoInstant).atZone(NAIROBI))
    } catch (_: Exception) {
        isoInstant
    }
}
