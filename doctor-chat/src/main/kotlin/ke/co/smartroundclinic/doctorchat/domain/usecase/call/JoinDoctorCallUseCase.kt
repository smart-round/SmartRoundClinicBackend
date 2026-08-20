package ke.co.smartroundclinic.doctorchat.domain.usecase.call

import io.ktor.http.HttpStatusCode
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.NotificationChannel
import ke.co.smartroundclinic.common.NotificationDestination
import ke.co.smartroundclinic.common.NotificationSender
import ke.co.smartroundclinic.common.PushNotificationEvents
import ke.co.smartroundclinic.common.RedisRepository
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.doctorchat.domain.model.DoctorCallInviteState
import ke.co.smartroundclinic.doctorchat.domain.repository.DoctorChatMessageRepository
import ke.co.smartroundclinic.doctorchat.domain.repository.DoctorChatThreadRepository
import ke.co.smartroundclinic.doctorchat.domain.service.DoctorChatSocketRegistry
import ke.co.smartroundclinic.doctorchat.presentation.dto.response.DoctorCallAnsweredEventRes
import ke.co.smartroundclinic.doctorchat.presentation.dto.response.JoinDoctorCallRes
import ke.co.smartroundclinic.infra.AppConfig
import ke.co.smartroundclinic.infra.realtime.RealtimeKitClient
import ke.co.smartroundclinic.infra.redis.RedisKeys
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

/**
 * Resolves (or lazily provisions) the Cloudflare RealtimeKit meeting for a doctor-chat thread and
 * returns this doctor's join token — mirrors [ke.co.smartroundclinic.consultation.domain.usecase.call.JoinThreadCallUseCase],
 * with both sides always using the same "doctor" preset (there's no patient side here). The meeting
 * title is a per-thread "doctors-lounge" room (private to the two doctors on that thread, never
 * shared with other doctor pairs), prefixed per environment via AppConfig.realtimeKit.roomPrefix
 * (e.g. "dev-doctors-lounge-{threadId}" on sandbox) so rooms are identifiable in the Cloudflare
 * dashboard without leaking across environments.
 */
class JoinDoctorCallUseCase(
    private val threads: DoctorChatThreadRepository,
    private val messages: DoctorChatMessageRepository,
    private val client: RealtimeKitClient,
    private val notificationSender: NotificationSender? = null,
    private val redis: RedisRepository,
    private val socketRegistry: DoctorChatSocketRegistry,
) {
    // encodeDefaults = true — the "type" discriminator on the CALL_* event DTOs defaults to a
    // fixed value, so without this it's always equal to its default and gets omitted entirely
    // from the wire payload, leaving clients with no "type" field to dispatch on (see 80a189f).
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val logger = LoggerFactory.getLogger(JoinDoctorCallUseCase::class.java)

    private fun meetingTitle(threadId: String): String {
        val prefix = AppConfig.realtimeKit.roomPrefix
        val base = if (prefix.isNullOrBlank()) "doctors-lounge" else "$prefix-doctors-lounge"
        return "$base-$threadId"
    }

    /** Mirrors JoinThreadCallUseCase.requireLiveInvite — see its doc comment for the atomicity rationale. */
    private suspend fun requireLiveInvite(callId: String, threadId: String, userId: String): Resource<DoctorCallInviteState> {
        val raw = redis.get(RedisKeys.callInvite(callId))
            ?: return Resource.Error("Call is no longer active")
        val invite = json.decodeFromString<DoctorCallInviteState>(raw)
        if (invite.threadId != threadId) return Resource.Error("Call invite does not match this thread")
        if (invite.callerId != userId && invite.calleeId != userId) return Resource.Error("Not a participant of this call")
        return Resource.Success(invite)
    }

    /** Mirrors JoinThreadCallUseCase.recordJoinAndMaybeCleanUp — see its doc comment for the atomicity rationale. */
    private suspend fun recordJoinAndMaybeCleanUp(invite: DoctorCallInviteState, joiningUserId: String) {
        if (joiningUserId == invite.calleeId) {
            runCatching {
                socketRegistry.sendToUser(invite.threadId, invite.callerId, json.encodeToString(DoctorCallAnsweredEventRes(callId = invite.callId)))
            }.onFailure { e -> logger.error("recordJoinAndMaybeCleanUp: socket send threw for callId=${invite.callId} callerId=${invite.callerId}", e) }
            runCatching {
                notificationSender?.sendCallSignal(
                    event = PushNotificationEvents.DOCTOR_CALL_ANSWERED,
                    recipientId = invite.callerId,
                    metadata = mapOf("callId" to invite.callId, "threadId" to invite.threadId),
                    ttlSeconds = RedisKeys.CALL_INVITE_TTL_SECONDS,
                )
            }.onFailure { e -> logger.error("recordJoinAndMaybeCleanUp: sendCallSignal threw for callId=${invite.callId} callerId=${invite.callerId}", e) }
        }

        val joinCount = redis.increment(RedisKeys.callJoinCount(invite.callId))
        if (joinCount <= 1) {
            redis.expire(RedisKeys.callInvite(invite.callId), RedisKeys.CALL_JOIN_GRACE_SECONDS)
        } else {
            redis.delete(RedisKeys.callInvite(invite.callId))
            redis.delete(RedisKeys.activeCallForDoctorChatThread(invite.threadId))
            redis.delete(RedisKeys.callJoinCount(invite.callId))
        }
    }

    private suspend fun resolveMeetingId(threadId: String, currentStoredId: String?): Resource<String> {
        val title = meetingTitle(threadId)

        if (currentStoredId != null) {
            val status = client.getMeetingStatus(currentStoredId)
            if (status != null && status != "INACTIVE") return Resource.Success(currentStoredId)
        }

        val liveId = client.findActiveMeetingByTitle(title)
        if (liveId != null) {
            threads.setVideoRoomId(threadId, liveId)
            return Resource.Success(liveId)
        }

        if (currentStoredId != null) threads.clearVideoRoomId(threadId)
        val newId = when (val r = client.createMeeting(title)) {
            is Resource.Success -> r.data?.takeIf { it.isNotBlank() } ?: return Resource.Error("Cloudflare returned no meeting id")
            is Resource.Error -> return Resource.Error(r.message ?: "Failed to create meeting")
        }
        return threads.setVideoRoomIdIfAbsent(threadId, newId)
    }

    suspend operator fun invoke(threadId: String, userId: String, callId: String): DefaultResponse<JoinDoctorCallRes?> {
        val thread = when (val r = threads.getById(threadId)) {
            is Resource.Success -> r.data ?: return DefaultResponse(httpStatusCode = HttpStatusCode.NotFound.value, status = false, message = "Thread not found", data = null)
            is Resource.Error -> return DefaultResponse(httpStatusCode = HttpStatusCode.InternalServerError.value, status = false, message = r.message ?: "Failed to load thread", data = null)
        }
        if (userId != thread.doctorAId && userId != thread.doctorBId) {
            return DefaultResponse(httpStatusCode = HttpStatusCode.Forbidden.value, status = false, message = "Not a participant of this thread", data = null)
        }

        val invite = when (val r = requireLiveInvite(callId, threadId, userId)) {
            is Resource.Success -> r.data!!
            is Resource.Error -> return DefaultResponse(httpStatusCode = HttpStatusCode.Gone.value, status = false, message = r.message ?: "Call is no longer active", data = null)
        }

        val preset = AppConfig.realtimeKit.doctorPreset
        val displayName = messages.getUserName(userId)
        val otherDoctorId = if (userId == thread.doctorAId) thread.doctorBId else thread.doctorAId

        var meetingId = when (val r = resolveMeetingId(threadId, thread.videoRoomId)) {
            is Resource.Success -> r.data!!
            is Resource.Error -> return DefaultResponse(httpStatusCode = HttpStatusCode.BadGateway.value, status = false, message = r.message ?: "Failed to resolve meeting", data = null)
        }

        val participant = client.addParticipant(meetingId, userId, preset, displayName, null)

        val resolvedParticipant = if (participant is Resource.Error) {
            threads.clearVideoRoomId(threadId)
            meetingId = when (val r = resolveMeetingId(threadId, null)) {
                is Resource.Success -> r.data!!
                is Resource.Error -> return DefaultResponse(httpStatusCode = HttpStatusCode.BadGateway.value, status = false, message = r.message ?: "Failed to recreate meeting", data = null)
            }
            client.addParticipant(meetingId, userId, preset, displayName, null)
        } else participant

        return when (resolvedParticipant) {
            is Resource.Success -> {
                val p = resolvedParticipant.data!!
                runCatching { recordJoinAndMaybeCleanUp(invite, userId) }
                    .onFailure { e -> logger.error("recordJoinAndMaybeCleanUp threw for callId=$callId threadId=$threadId userId=$userId", e) }
                runCatching {
                    notificationSender?.send(
                        title = PushNotificationEvents.CALL_DOCTOR_JOINED,
                        message = "Dr. ${displayName ?: "a colleague"} has joined the call",
                        channel = NotificationChannel.PUSH_NOTIFICATION,
                        destination = NotificationDestination.DOCTOR,
                        recipientId = otherDoctorId,
                        metadata = mapOf("event" to PushNotificationEvents.CALL_DOCTOR_JOINED, "threadId" to threadId),
                    )
                }.onFailure { e -> logger.error("Call-joined push notification threw for threadId=$threadId otherDoctorId=$otherDoctorId", e) }
                DefaultResponse(
                    httpStatusCode = HttpStatusCode.OK.value,
                    status = true,
                    message = "Ready to join",
                    data = JoinDoctorCallRes(meetingId = meetingId, participantId = p.id, authToken = p.token, presetName = preset),
                )
            }
            is Resource.Error -> DefaultResponse(httpStatusCode = HttpStatusCode.BadGateway.value, status = false, message = resolvedParticipant.message ?: "Failed to add participant", data = null)
        }
    }
}
