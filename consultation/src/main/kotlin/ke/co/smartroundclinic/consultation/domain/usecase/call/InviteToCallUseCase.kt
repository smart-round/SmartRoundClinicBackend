package ke.co.smartroundclinic.consultation.domain.usecase.call

import io.ktor.http.HttpStatusCode
import ke.co.smartroundclinic.common.NotificationSender
import ke.co.smartroundclinic.common.PushNotificationEvents
import ke.co.smartroundclinic.common.RedisRepository
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.common.sortableNowIso
import ke.co.smartroundclinic.consultation.domain.model.CallInviteState
import ke.co.smartroundclinic.consultation.domain.repository.ConsultationMessageRepository
import ke.co.smartroundclinic.consultation.domain.repository.ConsultationThreadRepository
import ke.co.smartroundclinic.consultation.domain.service.ConsultationSocketRegistry
import ke.co.smartroundclinic.consultation.presentation.dto.response.ConsultationCallInviteEventRes
import ke.co.smartroundclinic.consultation.presentation.dto.response.InviteToCallRes
import ke.co.smartroundclinic.infra.redis.RedisKeys
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.bson.types.ObjectId
import ke.co.smartroundclinic.common.DefaultResponse

/**
 * Rings the other party of a permanent (doctorId, patientId) thread — the WhatsApp-style
 * "invite" step that happens before either side actually joins the Cloudflare RealtimeKit
 * meeting. Deliberately does not touch RealtimeKit at all: if the callee accepts, their
 * client calls the existing POST /chat/{otherUserId}/call/join, which lazily provisions/
 * reuses the meeting as it always has; [JoinThreadCallUseCase] then signals CALL_ANSWERED
 * back to the caller so the caller's own client can also call join and land in the same room.
 */
class InviteToCallUseCase(
    private val threads: ConsultationThreadRepository,
    private val messages: ConsultationMessageRepository,
    private val redis: RedisRepository,
    private val socketRegistry: ConsultationSocketRegistry,
    private val notificationSender: NotificationSender? = null,
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend operator fun invoke(
        doctorId: String,
        patientId: String,
        callerId: String,
        isVideo: Boolean,
    ): DefaultResponse<InviteToCallRes?> {
        if (callerId != doctorId && callerId != patientId) {
            return DefaultResponse(
                httpStatusCode = HttpStatusCode.Forbidden.value,
                status = false,
                message = "Not a participant of this thread",
                data = null,
            )
        }

        when (val r = threads.getOrCreate(doctorId, patientId)) {
            is Resource.Error -> return DefaultResponse(
                httpStatusCode = HttpStatusCode.InternalServerError.value,
                status = false,
                message = r.message ?: "Failed to load thread",
                data = null,
            )
            else -> Unit
        }

        val calleeId = if (callerId == doctorId) patientId else doctorId
        val threadActiveKey = RedisKeys.activeCallForThread(doctorId, patientId)
        if (redis.get(threadActiveKey) != null) {
            return DefaultResponse(
                httpStatusCode = HttpStatusCode.Conflict.value,
                status = false,
                message = "A call is already ringing on this thread",
                data = null,
            )
        }

        val callId = ObjectId().toString()
        val callerName = messages.getUserName(callerId)
        val invite = CallInviteState(
            callId = callId,
            callerId = callerId,
            calleeId = calleeId,
            doctorId = doctorId,
            patientId = patientId,
            isVideo = isVideo,
            createdAt = sortableNowIso(),
        )

        redis.set(RedisKeys.callInvite(callId), json.encodeToString(invite), RedisKeys.CALL_INVITE_TTL_SECONDS)
        val claimed = redis.setIfAbsent(threadActiveKey, callId, RedisKeys.CALL_INVITE_TTL_SECONDS)
        if (!claimed) {
            redis.delete(RedisKeys.callInvite(callId))
            return DefaultResponse(
                httpStatusCode = HttpStatusCode.Conflict.value,
                status = false,
                message = "A call is already ringing on this thread",
                data = null,
            )
        }

        val threadKey = ConsultationSocketRegistry.threadKey(doctorId, patientId)
        runCatching {
            socketRegistry.sendToUser(
                threadKey,
                calleeId,
                json.encodeToString(
                    ConsultationCallInviteEventRes(
                        callId = callId,
                        callerId = callerId,
                        callerName = callerName,
                        isVideo = isVideo,
                        ringTimeoutSeconds = RedisKeys.CALL_INVITE_TTL_SECONDS,
                    )
                ),
            )
        }
        runCatching {
            notificationSender?.sendCallSignal(
                event = PushNotificationEvents.CALL_INVITE,
                recipientId = calleeId,
                metadata = mapOf(
                    "callId" to callId,
                    "callerId" to callerId,
                    "callerName" to (callerName ?: ""),
                    "doctorId" to doctorId,
                    "patientId" to patientId,
                    "isVideo" to isVideo.toString(),
                    "ringTimeoutSeconds" to RedisKeys.CALL_INVITE_TTL_SECONDS.toString(),
                ),
            )
        }

        return DefaultResponse(
            httpStatusCode = HttpStatusCode.OK.value,
            status = true,
            message = "Ringing",
            data = InviteToCallRes(callId = callId, ringTimeoutSeconds = RedisKeys.CALL_INVITE_TTL_SECONDS),
        )
    }
}
