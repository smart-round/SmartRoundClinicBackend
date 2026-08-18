package ke.co.smartroundclinic.doctorchat.domain.usecase.call

import io.ktor.http.HttpStatusCode
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.NotificationSender
import ke.co.smartroundclinic.common.PushNotificationEvents
import ke.co.smartroundclinic.common.RedisRepository
import ke.co.smartroundclinic.doctorchat.domain.model.DoctorCallInviteState
import ke.co.smartroundclinic.doctorchat.domain.service.DoctorChatSocketRegistry
import ke.co.smartroundclinic.doctorchat.presentation.dto.response.DoctorCallCancelledEventRes
import ke.co.smartroundclinic.infra.redis.RedisKeys
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

/** Caller hung up (or the client-side ring timer expired) before the callee answered. */
class CancelDoctorCallInviteUseCase(
    private val redis: RedisRepository,
    private val socketRegistry: DoctorChatSocketRegistry,
    private val notificationSender: NotificationSender? = null,
) {
    // encodeDefaults = true — the "type" discriminator on the CALL_* event DTOs defaults to a
    // fixed value, so without this it's always equal to its default and gets omitted entirely
    // from the wire payload, leaving clients with no "type" field to dispatch on (see 80a189f).
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val logger = LoggerFactory.getLogger(CancelDoctorCallInviteUseCase::class.java)

    suspend operator fun invoke(callId: String, callerId: String): DefaultResponse<Unit?> {
        val raw = redis.get(RedisKeys.callInvite(callId))
            ?: return DefaultResponse(httpStatusCode = HttpStatusCode.NotFound.value, status = false, message = "Call invite not found or already resolved", data = null)
        val invite = json.decodeFromString<DoctorCallInviteState>(raw)

        if (invite.callerId != callerId) {
            return DefaultResponse(httpStatusCode = HttpStatusCode.Forbidden.value, status = false, message = "Not the caller of this call", data = null)
        }

        redis.delete(RedisKeys.callInvite(callId))
        redis.delete(RedisKeys.activeCallForDoctorChatThread(invite.threadId))

        runCatching {
            socketRegistry.sendToUser(invite.threadId, invite.calleeId, json.encodeToString(DoctorCallCancelledEventRes(callId = callId)))
        }.onFailure { e -> logger.error("CancelDoctorCallInviteUseCase: socket send threw for callId=$callId calleeId=${invite.calleeId}", e) }
        runCatching {
            notificationSender?.sendCallSignal(
                event = PushNotificationEvents.DOCTOR_CALL_CANCELLED,
                recipientId = invite.calleeId,
                metadata = mapOf("callId" to callId, "threadId" to invite.threadId),
            )
        }.onFailure { e -> logger.error("CancelDoctorCallInviteUseCase: sendCallSignal threw for callId=$callId calleeId=${invite.calleeId}", e) }

        return DefaultResponse(httpStatusCode = HttpStatusCode.OK.value, status = true, message = "Cancelled", data = null)
    }
}
