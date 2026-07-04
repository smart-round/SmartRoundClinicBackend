package ke.co.smartroundclinic.auth.domain.usecase

import ke.co.smartroundclinic.notification.config.EmailConfig
import ke.co.smartroundclinic.notification.domain.model.EmailWithTemplate
import ke.co.smartroundclinic.notification.domain.repository.EmailRepository
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class NotifyNewDoctorSignUpUseCaseTest {

    // ── Test doubles ─────────────────────────────────────────────────────────

    private class FakeEmailRepository : EmailRepository {
        val sent = mutableListOf<EmailWithTemplate>()
        override suspend fun sendEmailWithTemplate(emailWithTemplate: EmailWithTemplate) {
            sent += emailWithTemplate
        }
    }

    private fun fakeConfig() = EmailConfig(
        apiKey = "test-api-key",
        baseUrl = "https://api.resend.com",
        onboardingEmail = "onboarding@test.com",
        onboardingTemplateId = "onboarding-tpl",
        accountVerificationEmail = "verify@test.com",
        accountVerificationTemplateId = "verify-tpl",
        doctorAccountVerificationSuccessTemplateId = "doc-verify-tpl",
        patientAccountVerificationSuccessTemplateId = "pat-verify-tpl",
        passwordResetTemplateId = "reset@test.com",
        passwordResetRequestTemplateId = "reset-req-tpl",
        passwordResetConfirmationTemplateId = "reset-conf-tpl",
        resendDoctorApplicationRequestTemplateId = "doc-app-tpl",
        resendDoctorApplicationRequestApprovedTemplateId = "doc-app-approved-tpl",
        resendDoctorApplicationRequestRejectedTemplateId = "doc-app-rejected-tpl",
        suspendUserEmail = "suspend@test.com",
        suspendUserTemplateId = "suspend-tpl",
        reinstatedUserEmail = "reinstate@test.com",
        reinstatedUserTemplateId = "reinstate-tpl",
        accountVerifyEmail = "verify@smartroundclinic.co.ke",
        accountSupportEmail = "support@smartroundclinic.co.ke",
        accountSupportTemplateId = "aad664d8-0886-438a-a585-a350897d0878",
        newDoctorSignUpTemplateId = "d214300f-0707-4658-a19b-11b019c7d1b9",
        accountSupportMailboxDestination = "info@smartroundclinic.co.ke",
    )

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    fun `sends to correct from, to, and template ID`(): Unit = runBlocking {
        val repo = FakeEmailRepository()
        val useCase = NotifyNewDoctorSignUpUseCase(repo, fakeConfig())

        useCase("Dr. Jane Smith", "jane@example.com", "doc-999", "2026-07-04T11:32:00Z")

        assertEquals(1, repo.sent.size)
        val email = repo.sent.first()
        assertEquals("verify@smartroundclinic.co.ke", email.from)
        assertEquals("info@smartroundclinic.co.ke", email.to)
        assertEquals("d214300f-0707-4658-a19b-11b019c7d1b9", email.template.id)
    }

    @Test
    fun `passes correct template variables`(): Unit = runBlocking {
        val repo = FakeEmailRepository()
        val useCase = NotifyNewDoctorSignUpUseCase(repo, fakeConfig())

        useCase("Dr. Jane Smith", "jane@example.com", "doc-999", "2026-07-04T11:32:00Z")

        val vars = repo.sent.first().template.variables
        assertEquals("Smith", vars["DOCTOR_LAST_NAME"])
        assertEquals("Dr. Jane Smith", vars["DOCTOR_FULL_NAME"])
        assertEquals("jane@example.com", vars["DOCTOR_EMAIL"])
        assertEquals("doc-999", vars["DOCTOR_ID"])
        assertEquals("Smartround Clinic", vars["SENDER_NAME"])
        assertEquals("verify@smartroundclinic.co.ke", vars["SENDER_EMAIL"])
        assertEquals("Not yet assigned", vars["SPECIALTY"])
        assertEquals("04 Jul 2026, 14:32", vars["SIGNUP_DATE"])
    }

    @Test
    fun `formats signup date in Nairobi time`(): Unit = runBlocking {
        val repo = FakeEmailRepository()
        val useCase = NotifyNewDoctorSignUpUseCase(repo, fakeConfig())

        // 2026-07-04T08:00:00Z → 11:00 Nairobi (UTC+3)
        useCase("John Doe", "john@example.com", "doc-1", "2026-07-04T08:00:00Z")

        assertEquals("04 Jul 2026, 11:00", repo.sent.first().template.variables["SIGNUP_DATE"])
    }

    @Test
    fun `extracts last name from full name`(): Unit = runBlocking {
        val repo = FakeEmailRepository()
        val useCase = NotifyNewDoctorSignUpUseCase(repo, fakeConfig())

        useCase("Alice Wanjiku Kamau", "alice@example.com", "doc-2", "2026-07-04T08:00:00Z")

        assertEquals("Kamau", repo.sent.first().template.variables["DOCTOR_LAST_NAME"])
    }
}
