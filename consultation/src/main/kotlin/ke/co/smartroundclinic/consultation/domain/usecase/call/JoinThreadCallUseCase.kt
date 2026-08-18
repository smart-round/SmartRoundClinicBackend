package ke.co.smartroundclinic.consultation.domain.usecase.call

import io.ktor.http.HttpStatusCode
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.NotificationChannel
import ke.co.smartroundclinic.common.PushNotificationEvents
import ke.co.smartroundclinic.common.NotificationDestination
import ke.co.smartroundclinic.common.NotificationSender
import ke.co.smartroundclinic.common.RedisRepository
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.consultation.domain.model.CallInviteState
import ke.co.smartroundclinic.consultation.domain.repository.ConsultationMessageRepository
import ke.co.smartroundclinic.consultation.domain.repository.ConsultationThreadRepository
import ke.co.smartroundclinic.consultation.domain.service.ConsultationSocketRegistry
import ke.co.smartroundclinic.consultation.presentation.dto.response.ConsultationCallAnsweredEventRes
import ke.co.smartroundclinic.consultation.presentation.dto.response.JoinCallRes
import ke.co.smartroundclinic.infra.AppConfig
import ke.co.smartroundclinic.infra.realtime.RealtimeKitClient
import ke.co.smartroundclinic.infra.redis.RedisKeys
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

/**
 * Resolves (or lazily provisions) the Cloudflare RealtimeKit meeting tied to a
 * permanent (doctorId, patientId) thread and returns this user's participant join token.
 *
 * Flow:
 *   1. If the thread has no `videoRoomId`, create a meeting on Cloudflare and persist the id
 *   2. Call Cloudflare's add-participant API with the role-appropriate preset
 *   3. Return `{ meetingId, participantId, authToken, presetName }`
 */
class JoinThreadCallUseCase(
    private val threads: ConsultationThreadRepository,
    private val messages: ConsultationMessageRepository,
    private val client: RealtimeKitClient,
    private val notificationSender: NotificationSender? = null,
    private val redis: RedisRepository? = null,
    private val socketRegistry: ConsultationSocketRegistry? = null,
) {
    // encodeDefaults = true — the "type" discriminator on the CALL_* event DTOs defaults to a
    // fixed value, so without this it's always equal to its default and gets omitted entirely
    // from the wire payload, leaving clients with no "type" field to dispatch on (see 80a189f).
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val logger = LoggerFactory.getLogger(JoinThreadCallUseCase::class.java)

    private fun meetingTitle(doctorId: String, patientId: String) = "Thread $doctorId:$patientId"

    /**
     * If this join is the callee accepting a ringing invite (see [InviteToCallUseCase]), tells the
     * caller's client CALL_ANSWERED so it stops ringing and calls join itself. No-ops otherwise —
     * including when the *caller's own* client calls join in response to that signal, since by then
     * the invite has already been cleared below.
     */
    private suspend fun signalAnsweredIfRinging(doctorId: String, patientId: String, joiningUserId: String) {
        if (redis == null || socketRegistry == null) return
        val callId = redis.get(RedisKeys.activeCallForThread(doctorId, patientId)) ?: return
        val raw = redis.get(RedisKeys.callInvite(callId)) ?: return
        val invite = json.decodeFromString<CallInviteState>(raw)
        if (invite.calleeId != joiningUserId) return

        redis.delete(RedisKeys.callInvite(callId))
        redis.delete(RedisKeys.activeCallForThread(doctorId, patientId))
        val threadKey = ConsultationSocketRegistry.threadKey(doctorId, patientId)
        socketRegistry.sendToUser(threadKey, invite.callerId, json.encodeToString(ConsultationCallAnsweredEventRes(callId = callId)))
        runCatching {
            notificationSender?.sendCallSignal(
                event = PushNotificationEvents.CALL_ANSWERED,
                recipientId = invite.callerId,
                metadata = mapOf("callId" to callId, "doctorId" to doctorId, "patientId" to patientId),
            )
        }.onFailure { e -> logger.error("signalAnsweredIfRinging: sendCallSignal threw for callId=$callId callerId=${invite.callerId}", e) }
    }

    private suspend fun resolveMeetingId(doctorId: String, patientId: String, currentStoredId: String?): Resource<String> {
        val title = meetingTitle(doctorId, patientId)

        if (currentStoredId != null) {
            val status = client.getMeetingStatus(currentStoredId)
            if (status != null && status != "INACTIVE") return Resource.Success(currentStoredId)
        }

        val liveId = client.findActiveMeetingByTitle(title)
        if (liveId != null) {
            threads.setVideoRoomId(doctorId, patientId, liveId)
            return Resource.Success(liveId)
        }

        if (currentStoredId != null) threads.clearVideoRoomId(doctorId, patientId)
        val newId = when (val r = client.createMeeting(title)) {
            is Resource.Success -> r.data?.takeIf { it.isNotBlank() }
                ?: return Resource.Error("Cloudflare returned no meeting id")
            is Resource.Error -> return Resource.Error(r.message ?: "Failed to create meeting")
        }
        return threads.setVideoRoomIdIfAbsent(doctorId, patientId, newId)
    }

    suspend operator fun invoke(doctorId: String, patientId: String, userId: String): DefaultResponse<JoinCallRes?> {
        if (userId != doctorId && userId != patientId) {
            return DefaultResponse(
                httpStatusCode = HttpStatusCode.Forbidden.value,
                status = false,
                message = "Not a participant of this thread",
                data = null,
            )
        }

        val thread = when (val r = threads.getOrCreate(doctorId, patientId)) {
            is Resource.Success -> r.data ?: return DefaultResponse(
                httpStatusCode = HttpStatusCode.InternalServerError.value,
                status = false,
                message = "Failed to load thread",
                data = null,
            )
            is Resource.Error -> return DefaultResponse(
                httpStatusCode = HttpStatusCode.InternalServerError.value,
                status = false,
                message = r.message ?: "Failed to load thread",
                data = null,
            )
        }

        val isDoctor = userId == doctorId
        val preset = if (isDoctor) AppConfig.realtimeKit.doctorPreset else AppConfig.realtimeKit.patientPreset
        val displayName = messages.getUserName(userId)

        var meetingId = when (val r = resolveMeetingId(doctorId, patientId, thread.videoRoomId)) {
            is Resource.Success -> r.data!!
            is Resource.Error -> return DefaultResponse(
                httpStatusCode = HttpStatusCode.BadGateway.value,
                status = false,
                message = r.message ?: "Failed to resolve meeting",
                data = null,
            )
        }

        val participant = client.addParticipant(
            meetingId = meetingId,
            customParticipantId = userId,
            presetName = preset,
            name = displayName,
            picture = null,
        )

        val resolvedParticipant = if (participant is Resource.Error) {
            threads.clearVideoRoomId(doctorId, patientId)
            meetingId = when (val r = resolveMeetingId(doctorId, patientId, null)) {
                is Resource.Success -> r.data!!
                is Resource.Error -> return DefaultResponse(
                    httpStatusCode = HttpStatusCode.BadGateway.value,
                    status = false,
                    message = r.message ?: "Failed to recreate meeting",
                    data = null,
                )
            }
            client.addParticipant(
                meetingId = meetingId,
                customParticipantId = userId,
                presetName = preset,
                name = displayName,
                picture = null,
            )
        } else participant

        return when (resolvedParticipant) {
            is Resource.Success -> {
                val p = resolvedParticipant.data!!
                runCatching { signalAnsweredIfRinging(doctorId, patientId, userId) }
                    .onFailure { e -> logger.error("signalAnsweredIfRinging threw for doctorId=$doctorId patientId=$patientId userId=$userId", e) }
                runCatching {
                    notificationSender?.send(
                        title = if (isDoctor) PushNotificationEvents.CALL_DOCTOR_JOINED else PushNotificationEvents.CALL_PATIENT_JOINED,
                        message = if (isDoctor) "Your doctor has joined the video call"
                                  else "Your patient has joined the video call",
                        channel = NotificationChannel.PUSH_NOTIFICATION,
                        destination = if (isDoctor) NotificationDestination.PATIENT else NotificationDestination.DOCTOR,
                        recipientId = if (isDoctor) patientId else doctorId,
                        metadata = mapOf(
                            "event" to if (isDoctor) PushNotificationEvents.CALL_DOCTOR_JOINED else PushNotificationEvents.CALL_PATIENT_JOINED,
                            "doctorId" to doctorId,
                            "patientId" to patientId,
                        ),
                    )
                }.onFailure { e -> logger.error("Call-joined push notification threw for doctorId=$doctorId patientId=$patientId isDoctor=$isDoctor", e) }
                DefaultResponse(
                    httpStatusCode = HttpStatusCode.OK.value,
                    status = true,
                    message = "Ready to join",
                    data = JoinCallRes(
                        meetingId = meetingId,
                        participantId = p.id,
                        authToken = p.token,
                        presetName = preset,
                    ),
                )
            }
            is Resource.Error -> DefaultResponse(
                httpStatusCode = HttpStatusCode.BadGateway.value,
                status = false,
                message = resolvedParticipant.message ?: "Failed to add participant",
                data = null,
            )
        }
    }
}
