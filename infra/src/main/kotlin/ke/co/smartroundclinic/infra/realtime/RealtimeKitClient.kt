package ke.co.smartroundclinic.infra.realtime

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.infra.AppConfig
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Thin wrapper over Cloudflare's RealtimeKit REST API.
 *
 * Docs:
 *  - https://developers.cloudflare.com/api/resources/realtime_kit/subresources/meetings
 *  - https://developers.cloudflare.com/api/resources/realtime_kit/subresources/meetings/subresources/participants
 */
class RealtimeKitClient(private val http: HttpClient) {

    private val baseMeetingsPath: String
        get() = "${AppConfig.realtimeKit.baseUrl.trimEnd('/')}/accounts/${AppConfig.realtimeKit.accountId}/realtime/kit/${AppConfig.realtimeKit.appId}/meetings"

    /** Creates a new meeting. Returns the meeting id (`data.id`). */
    suspend fun createMeeting(title: String, preferredRegion: String? = null): Resource<String> = try {
        val res: HttpResponse = http.post(baseMeetingsPath) {
            bearerAuth(AppConfig.realtimeKit.apiToken)
            contentType(ContentType.Application.Json)
            headers { append(HttpHeaders.Accept, "application/json") }
            setBody(CreateMeetingReq(title = title, preferredRegion = preferredRegion))
        }
        if (!res.status.isSuccess()) {
            Resource.Error("Cloudflare RealtimeKit createMeeting failed: ${res.status.value}")
        } else {
            val body: CloudflareEnvelope<MeetingData> = res.body()
            val id = body.data?.id
            if (body.success && !id.isNullOrBlank()) Resource.Success(id) else Resource.Error("Cloudflare RealtimeKit returned no meeting id")
        }
    } catch (e: Exception) {
        Resource.Error(e.message ?: "Failed to create RealtimeKit meeting")
    }

    /** Adds a participant to the given meeting and returns the join auth token. */
    suspend fun addParticipant(
        meetingId: String,
        customParticipantId: String,
        presetName: String,
        name: String?,
        picture: String?,
    ): Resource<ParticipantData> = try {
        val res: HttpResponse = http.post("$baseMeetingsPath/$meetingId/participants") {
            bearerAuth(AppConfig.realtimeKit.apiToken)
            contentType(ContentType.Application.Json)
            headers { append(HttpHeaders.Accept, "application/json") }
            setBody(AddParticipantReq(
                customParticipantId = customParticipantId,
                presetName = presetName,
                name = name,
                picture = picture,
            ))
        }
        if (!res.status.isSuccess()) {
            Resource.Error("Cloudflare RealtimeKit addParticipant failed: ${res.status.value}")
        } else {
            val body: CloudflareEnvelope<ParticipantData> = res.body()
            val data = body.data
            if (body.success && data != null && data.token.isNotBlank()) Resource.Success(data)
            else Resource.Error("Cloudflare RealtimeKit returned no participant token")
        }
    } catch (e: Exception) {
        Resource.Error(e.message ?: "Failed to add RealtimeKit participant")
    }

    private fun HttpStatusCode.isSuccess() = value in 200..299
}

@Serializable
internal data class CloudflareEnvelope<T>(
    val success: Boolean = false,
    val data: T? = null,
    val errors: List<CloudflareError> = emptyList(),
    val messages: List<CloudflareMessage> = emptyList(),
)

@Serializable
internal data class CloudflareError(val code: Int? = null, val message: String? = null)

@Serializable
internal data class CloudflareMessage(val code: Int? = null, val message: String? = null)

@Serializable
internal data class CreateMeetingReq(
    val title: String,
    @SerialName("preferred_region") val preferredRegion: String? = null,
)

@Serializable
internal data class MeetingData(val id: String)

@Serializable
internal data class AddParticipantReq(
    @SerialName("custom_participant_id") val customParticipantId: String,
    @SerialName("preset_name") val presetName: String,
    val name: String? = null,
    val picture: String? = null,
)

@Serializable
data class ParticipantData(
    val id: String,
    val token: String,
    @SerialName("custom_participant_id") val customParticipantId: String,
    @SerialName("preset_name") val presetName: String,
    val name: String? = null,
    val picture: String? = null,
)
