package ke.co.smartroundclinic.doctor.domain.usecase.compliance

import io.ktor.http.HttpStatusCode
import ke.co.smartroundclinic.auth.data.entity.UserEntity
import ke.co.smartroundclinic.auth.domain.repository.UserRepository
import ke.co.smartroundclinic.common.AccountResolver
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.common.SubAccountCreator
import ke.co.smartroundclinic.doctor.domain.model.toModel
import ke.co.smartroundclinic.doctor.domain.repository.ComplianceRepository
import ke.co.smartroundclinic.doctor.domain.repository.PaymentDetailsRepository
import ke.co.smartroundclinic.doctor.presentation.dto.response.ComplianceRes
import ke.co.smartroundclinic.doctor.presentation.dto.response.toRes
import ke.co.smartroundclinic.common.NotificationChannel
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
    private val paymentDetailsRepository: PaymentDetailsRepository,
    private val subAccountCreator: SubAccountCreator? = null,
    private val notificationSender: NotificationSender? = null,
    private val accountResolver: AccountResolver? = null,
) {
    private val log = LoggerFactory.getLogger(ApproveComplianceUseCase::class.java)

    suspend operator fun invoke(id: String, adminId: String): DefaultResponse<ComplianceRes?> =
        withContext(Dispatchers.IO) {
            val approve = repository.approve(id, adminId)
            if (approve is Resource.Error) return@withContext approve.toDefaultResponse()
            val doctorId = approve.data?.doctorId ?: return@withContext approve.toDefaultResponse()
            userRepository.adminUpdateUser(doctorId, accountStatus = null, verificationStatus = UserEntity.VerificationStatus.VERIFIED)
            val user = userRepository.getUser(doctorId)
            if (user is Resource.Error) return@withContext user.toDefaultResponse()
            sendApplicationApprovedEmail(user.data?.fullName ?: "", user.data?.email ?: "")
            runCatching { createPaystackSubAccount(doctorId, user.data?.fullName) }
                .onFailure { log.error("Paystack subaccount step failed for doctorId=$doctorId — ${it.message}", it) }
            runCatching {
                notificationSender?.send(
                    title = "Application Approved",
                    message = "Congratulations! Your doctor application has been approved. You can now receive appointments.",
                    channel = NotificationChannel.PUSH_NOTIFICATION,
                    destination = NotificationDestination.DOCTOR,
                    recipientId = doctorId,
                )
            }
            approve.toDefaultResponse() { approve.data?.toModel()?.toRes() }
        }

    private suspend fun createPaystackSubAccount(doctorId: String, fullName: String?) {
        if (subAccountCreator == null) return
        val details = (paymentDetailsRepository.getPaymentDetails(doctorId) as? Resource.Success)?.data
        if (details == null) {
            log.warn("Cannot create Paystack subaccount — no payment details for doctorId=$doctorId")
            return
        }

        // Non-blocking account validation — log result but never stop approval flow
        runCatching {
            accountResolver?.validateAccount(
                accountNumber = details.accountNumber,
                bankCode = details.bankCode,
                accountName = details.accountName,
                accountType = "personal",
                countryCode = "KE",
                documentType = "identityNumber",
                documentNumber = null,
            )?.fold(
                onSuccess = { verified ->
                    if (verified) {
                        log.info("Account validation succeeded for doctorId=$doctorId")
                    } else {
                        log.warn("Account validation returned unverified for doctorId=$doctorId")
                    }
                },
                onFailure = { err ->
                    log.warn("Account validation failed for doctorId=$doctorId — ${err.message}")
                }
            )
        }.onFailure { err ->
            log.warn("Account validation step threw an exception for doctorId=$doctorId — ${err.message}")
        }

        subAccountCreator.createForDoctor(
            businessName = fullName?.takeIf { it.isNotBlank() } ?: details.accountName,
            settlementBank = details.bankCode,
            accountNumber = details.accountNumber,
        ).onSuccess { code ->
            paymentDetailsRepository.saveSubaccountCode(doctorId, code)
        }.onFailure { err ->
            log.warn("Paystack subaccount creation failed for doctorId=$doctorId — ${err.message}")
        }
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
