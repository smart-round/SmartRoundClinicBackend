package ke.co.smartroundclinic.doctorchat.domain.usecase.call

import io.ktor.http.HttpStatusCode
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.NotificationSender
import ke.co.smartroundclinic.common.PushNotificationEvents
import ke.co.smartroundclinic.common.RedisRepository
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.common.sortableNowIso
import ke.co.smartroundclinic.doctorchat.domain.model.DoctorCallInviteState
import ke.co.smartroundclinic.doctorchat.domain.repository.DoctorChatMessageRepository
import ke.co.smartroundclinic.doctorchat.domain.repository.DoctorChatThreadRepository
import ke.co.smartroundclinic.doctorchat.domain.service.DoctorChatSocketRegistry
import ke.co.smartroundclinic.doctorchat.presentation.dto.response.DoctorCallInviteEventRes
import ke.co.smartroundclinic.doctorchat.presentation.dto.response.InviteToDoctorCallRes
import ke.co.smartroundclinic.infra.redis.RedisKeys
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.bson.types.ObjectId
import org.slf4j.LoggerFactory

/** Rings the other doctor on the thread — mirrors consultation's InviteToCallUseCase. */
class InviteToDoctorCallUseCase(
    private val threads: DoctorChatThreadRepository,
    private val messages: DoctorChatMessageRepository,
    private val redis: RedisRepository,
    private val socketRegistry: DoctorChatSocketRegistry,
    private val notificationSender: NotificationSender? = null,
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val logger = LoggerFactory.getLogger(InviteToDoctorCallUseCase::class.java)

    suspend operator fun invoke(threadId: String, callerId: String, isVideo: Boolean): DefaultResponse<InviteToDoctorCallRes?> {
        val thread = when (val r = threads.getById(threadId)) {
            is Resource.Success -> r.data ?: return DefaultResponse(httpStatusCode = HttpStatusCode.NotFound.value, status = false, message = "Thread not found", data = null)
            is Resource.Error -> return DefaultResponse(httpStatusCode = HttpStatusCode.InternalServerError.value, status = false, message = r.message ?: "Failed to load thread", data = null)
        }
        if (callerId != thread.doctorAId && callerId != thread.doctorBId) {
            return DefaultResponse(httpStatusCode = HttpStatusCode.Forbidden.value, status = false, message = "Not a participant of this thread", data = null)
        }
        val calleeId = if (callerId == thread.doctorAId) thread.doctorBId else thread.doctorAId

        val threadActiveKey = RedisKeys.activeCallForDoctorChatThread(threadId)
        if (redis.get(threadActiveKey) != null) {
            return DefaultResponse(httpStatusCode = HttpStatusCode.Conflict.value, status = false, message = "A call is already ringing on this thread", data = null)
        }

        val callId = ObjectId().toString()
        val callerName = messages.getUserName(callerId)
        val invite = DoctorCallInviteState(
            callId = callId, callerId = callerId, calleeId = calleeId, threadId = threadId, isVideo = isVideo, createdAt = sortableNowIso(),
        )

        redis.set(RedisKeys.callInvite(callId), json.encodeToString(invite), RedisKeys.CALL_INVITE_TTL_SECONDS)
        val claimed = redis.setIfAbsent(threadActiveKey, callId, RedisKeys.CALL_INVITE_TTL_SECONDS)
        if (!claimed) {
            redis.delete(RedisKeys.callInvite(callId))
            return DefaultResponse(httpStatusCode = HttpStatusCode.Conflict.value, status = false, message = "A call is already ringing on this thread", data = null)
        }

        runCatching {
            socketRegistry.sendToUser(
                threadId, calleeId,
                json.encodeToString(DoctorCallInviteEventRes(callId = callId, callerId = callerId, callerName = callerName, isVideo = isVideo, ringTimeoutSeconds = RedisKeys.CALL_INVITE_TTL_SECONDS)),
            )
        }.onFailure { logger.error("InviteToDoctorCallUseCase: socket send threw for callId=$callId calleeId=$calleeId", it) }

        runCatching {
            notificationSender?.sendCallSignal(
                event = PushNotificationEvents.DOCTOR_CALL_INVITE,
                recipientId = calleeId,
                metadata = mapOf(
                    "callId" to callId, "callerId" to callerId, "callerName" to (callerName ?: ""),
                    "threadId" to threadId, "isVideo" to isVideo.toString(),
                    "ringTimeoutSeconds" to RedisKeys.CALL_INVITE_TTL_SECONDS.toString(),
                ),
            )
        }.onFailure { logger.error("InviteToDoctorCallUseCase: sendCallSignal threw for callId=$callId calleeId=$calleeId", it) }

        return DefaultResponse(
            httpStatusCode = HttpStatusCode.OK.value, status = true, message = "Ringing",
            data = InviteToDoctorCallRes(callId = callId, ringTimeoutSeconds = RedisKeys.CALL_INVITE_TTL_SECONDS),
        )
    }
}
