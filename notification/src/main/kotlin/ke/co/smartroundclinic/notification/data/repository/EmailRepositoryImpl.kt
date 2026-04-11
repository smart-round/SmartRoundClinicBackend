package ke.co.smartroundclinic.notification.data.repository

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import ke.co.smartroundclinic.infra.logResponse
import ke.co.smartroundclinic.notification.config.EmailConfig
import ke.co.smartroundclinic.notification.domain.model.EmailWithTemplate
import ke.co.smartroundclinic.notification.domain.repository.EmailRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonElement
import org.slf4j.LoggerFactory

class EmailRepositoryImpl(
    private val http: HttpClient,
    private val config: EmailConfig
): EmailRepository {
    private val logger = LoggerFactory.getLogger(EmailRepositoryImpl::class.java)

    override suspend fun sendEmailWithTemplate(emailWithTemplate: EmailWithTemplate) = withContext(Dispatchers.IO) {
        try {
            val response = http.post("${config.baseUrl}/emails") {
                logger.info("Url: ${config.baseUrl}/emails")
                header(HttpHeaders.Authorization, "Bearer ${config.apiKey}")
                contentType(ContentType.Application.Json)
                setBody(emailWithTemplate)
            }
            logger.info(response.logResponse())
        } catch (e: Exception) {
            logger.error("Failed to send email", e)
        }
    }
}