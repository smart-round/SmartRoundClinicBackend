package ke.co.smartroundclinic.infra

import com.google.gson.Gson
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.request
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

fun provideHttpClient() = HttpClient(CIO) {
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
        })
    }
}

suspend fun HttpResponse.logResponse(): String {
    return try {
        val requestTime = this.requestTime.timestamp
        val responseTime = this.responseTime.timestamp
        val durationMs = responseTime - requestTime

        val bodyText = try {
            this.bodyAsText()
        } catch (e: Exception) {
            "Unable to read body: ${e.message}"
        }

        val headersMap = this.headers.entries().associate { it.key to it.value }
        Gson().toJson(
            mapOf(
                "success" to (status.value in 200..299),
                "url" to this.request.url.toString(),
                "status" to this.status.value,
                "durationMs" to durationMs,
                "requestTime" to requestTime,
                "responseTime" to responseTime,
                "headers" to headersMap,
                "body" to bodyText
            )
        )
    } catch (e: Exception) {
        "Failed to log response: ${e.message}"
    }
}