package ke.co.smartroundclinic.consultation.domain.usecase.call

import io.ktor.http.HttpStatusCode
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.NotificationSender
import ke.co.smartroundclinic.common.PushNotificationEvents
import ke.co.smartroundclinic.common.RedisRepository
import ke.co.smartroundclinic.consultation.domain.model.CallInviteState
import ke.co.smartroundclinic.consultation.domain.service.ConsultationSocketRegistry
import ke.co.smartroundclinic.consultation.presentation.dto.response.ConsultationCallCancelledEventRes
import ke.co.smartroundclinic.infra.redis.RedisKeys
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

/** Caller hung up (or the client-side ring timer expired) before the callee answered. */
class CancelCallInviteUseCase(
    private val redis: RedisRepository,
    private val socketRegistry: ConsultationSocketRegistry,
    private val notificationSender: NotificationSender? = null,
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val logger = LoggerFactory.getLogger(CancelCallInviteUseCase::class.java)

    suspend operator fun invoke(callId: String, callerId: String): DefaultResponse<Unit?> {
        val raw = redis.get(RedisKeys.callInvite(callId))
            ?: return DefaultResponse(httpStatusCode = HttpStatusCode.NotFound.value, status = false, message = "Call invite not found or already resolved", data = null)
        val invite = json.decodeFromString<CallInviteState>(raw)

        if (invite.callerId != callerId) {
            return DefaultResponse(httpStatusCode = HttpStatusCode.Forbidden.value, status = false, message = "Not the caller of this call", data = null)
        }

        redis.delete(RedisKeys.callInvite(callId))
        redis.delete(RedisKeys.activeCallForThread(invite.doctorId, invite.patientId))

        val threadKey = ConsultationSocketRegistry.threadKey(invite.doctorId, invite.patientId)
        runCatching {
            socketRegistry.sendToUser(threadKey, invite.calleeId, json.encodeToString(ConsultationCallCancelledEventRes(callId = callId)))
        }.onFailure { e -> logger.error("CancelCallInviteUseCase: socket send threw for callId=$callId calleeId=${invite.calleeId}", e) }
        runCatching {
            notificationSender?.sendCallSignal(
                event = PushNotificationEvents.CALL_CANCELLED,
                recipientId = invite.calleeId,
                metadata = mapOf(
                    "callId" to callId,
                    "doctorId" to invite.doctorId,
                    "patientId" to invite.patientId,
                ),
            )
        }.onFailure { e -> logger.error("CancelCallInviteUseCase: sendCallSignal threw for callId=$callId calleeId=${invite.calleeId}", e) }

        return DefaultResponse(httpStatusCode = HttpStatusCode.OK.value, status = true, message = "Cancelled", data = null)
    }
}
