package ke.co.smartroundclinic.support.domain.usecase.ticket

import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.notification.config.EmailConfig
import ke.co.smartroundclinic.notification.domain.model.EmailWithTemplate
import ke.co.smartroundclinic.notification.domain.repository.EmailRepository
import ke.co.smartroundclinic.support.data.entity.IssueCategoryEntity
import ke.co.smartroundclinic.support.data.entity.TicketEntity
import ke.co.smartroundclinic.support.data.entity.TicketStatus
import ke.co.smartroundclinic.support.domain.repository.IssueCategoryRepository
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class SendSupportTicketEmailUseCaseTest {

    // ── Test doubles ─────────────────────────────────────────────────────────

    private class FakeEmailRepository : EmailRepository {
        val sent = mutableListOf<EmailWithTemplate>()
        override suspend fun sendEmailWithTemplate(emailWithTemplate: EmailWithTemplate) {
            sent += emailWithTemplate
        }
    }

    private class FakeIssueCategoryRepository(private val categoryName: String) : IssueCategoryRepository {
        override suspend fun getById(id: String): Resource<IssueCategoryEntity?> =
            Resource.Success(data = IssueCategoryEntity(id = id, name = categoryName), message = "ok")

        override suspend fun create(entity: IssueCategoryEntity): Resource<IssueCategoryEntity?> =
            Resource.Success(data = entity, message = "ok")

        override suspend fun getAll(page: Int, size: Int): Resource<Pair<List<IssueCategoryEntity>, Long>> =
            Resource.Success(data = emptyList<IssueCategoryEntity>() to 0L, message = "ok")

        override suspend fun update(id: String, name: String?, description: String?, status: Boolean?): Resource<IssueCategoryEntity?> =
            Resource.Success(data = null, message = "ok")

        override suspend fun delete(id: String): Resource<IssueCategoryEntity?> =
            Resource.Success(data = null, message = "ok")
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

    private fun ticket(status: TicketStatus = TicketStatus.OPEN) = TicketEntity(
        id = "ticket-123",
        ticketNumber = "#1234567ABC",
        issueCategoryId = "cat-456",
        title = "App crashes on login",
        description = "Every time I try to log in, the app crashes.",
        complainantName = "Jane Doe",
        complainantEmail = "jane@example.com",
        status = status,
    )

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    fun `onCreated sends to correct from, to, and template ID`(): Unit = runBlocking {
        val repo = FakeEmailRepository()
        val useCase = SendSupportTicketEmailUseCase(repo, fakeConfig(), FakeIssueCategoryRepository("General"))

        useCase.onCreated(ticket())

        assertEquals(1, repo.sent.size)
        val email = repo.sent.first()
        assertEquals("support@smartroundclinic.co.ke", email.from)
        assertEquals("info@smartroundclinic.co.ke", email.to)
        assertEquals("aad664d8-0886-438a-a585-a350897d0878", email.template.id)
    }

    @Test
    fun `onCreated passes correct event variables for new ticket`(): Unit = runBlocking {
        val repo = FakeEmailRepository()
        val useCase = SendSupportTicketEmailUseCase(repo, fakeConfig(), FakeIssueCategoryRepository("Billing"))

        useCase.onCreated(ticket())

        val vars = repo.sent.first().template.variables
        assertEquals("New Support Ticket", vars["EVENT_TITLE"])
        assertEquals("A new support ticket has been submitted and requires triage.", vars["EVENT_MESSAGE"])
        assertEquals("OPEN", vars["STATUS"])
        assertEquals("Billing", vars["ISSUE_CATEGORY_NAME"])
        assertEquals("#1234567ABC", vars["TICKET_NUMBER"])
        assertEquals("ticket-123", vars["TICKET_ID"])
        assertEquals("App crashes on login", vars["TITLE"])
        assertEquals("Jane Doe", vars["COMPLAINANT_NAME"])
        assertEquals("jane@example.com", vars["COMPLAINANT_EMAIL"])
        assertNotNull(vars["DESCRIPTION"])
        assertEquals("Every time I try to log in, the app crashes.", vars["DESCRIPTION"])
    }

    @Test
    fun `onStatusChanged sends to correct from, to, and template ID`(): Unit = runBlocking {
        val repo = FakeEmailRepository()
        val useCase = SendSupportTicketEmailUseCase(repo, fakeConfig(), FakeIssueCategoryRepository("General"))

        useCase.onStatusChanged(ticket(TicketStatus.IN_PROGRESS))

        assertEquals(1, repo.sent.size)
        val email = repo.sent.first()
        assertEquals("support@smartroundclinic.co.ke", email.from)
        assertEquals("info@smartroundclinic.co.ke", email.to)
        assertEquals("aad664d8-0886-438a-a585-a350897d0878", email.template.id)
    }

    @Test
    fun `onStatusChanged passes correct event variables for status update`(): Unit = runBlocking {
        val repo = FakeEmailRepository()
        val useCase = SendSupportTicketEmailUseCase(repo, fakeConfig(), FakeIssueCategoryRepository("Technical"))

        useCase.onStatusChanged(ticket(TicketStatus.CLOSED))

        val vars = repo.sent.first().template.variables
        assertEquals("Ticket Status Updated", vars["EVENT_TITLE"])
        assertEquals("A support ticket status has been updated and may need attention.", vars["EVENT_MESSAGE"])
        assertEquals("CLOSED", vars["STATUS"])
        assertEquals("Technical", vars["ISSUE_CATEGORY_NAME"])
        assertEquals("#1234567ABC", vars["TICKET_NUMBER"])
        assertEquals("ticket-123", vars["TICKET_ID"])
        assertEquals("App crashes on login", vars["TITLE"])
        assertEquals("Jane Doe", vars["COMPLAINANT_NAME"])
        assertEquals("jane@example.com", vars["COMPLAINANT_EMAIL"])
    }

    @Test
    fun `onCreated resolves category name from repository`(): Unit = runBlocking {
        val repo = FakeEmailRepository()
        val useCase = SendSupportTicketEmailUseCase(repo, fakeConfig(), FakeIssueCategoryRepository("Payments"))

        useCase.onCreated(ticket())

        assertEquals("Payments", repo.sent.first().template.variables["ISSUE_CATEGORY_NAME"])
    }

    @Test
    fun `category lookup failure falls back to Unknown`(): Unit = runBlocking {
        val failingCategoryRepo = object : IssueCategoryRepository {
            override suspend fun getById(id: String): Resource<IssueCategoryEntity?> =
                Resource.Error("not found")
            override suspend fun create(entity: IssueCategoryEntity): Resource<IssueCategoryEntity?> =
                Resource.Success(data = entity, message = "ok")
            override suspend fun getAll(page: Int, size: Int): Resource<Pair<List<IssueCategoryEntity>, Long>> =
                Resource.Success(data = emptyList<IssueCategoryEntity>() to 0L, message = "ok")
            override suspend fun update(id: String, name: String?, description: String?, status: Boolean?): Resource<IssueCategoryEntity?> =
                Resource.Success(data = null, message = "ok")
            override suspend fun delete(id: String): Resource<IssueCategoryEntity?> =
                Resource.Success(data = null, message = "ok")
        }
        val repo = FakeEmailRepository()
        val useCase = SendSupportTicketEmailUseCase(repo, fakeConfig(), failingCategoryRepo)

        useCase.onCreated(ticket())

        assertEquals("Unknown", repo.sent.first().template.variables["ISSUE_CATEGORY_NAME"])
    }
}
