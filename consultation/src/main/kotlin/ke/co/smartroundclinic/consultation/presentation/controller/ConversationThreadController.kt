package ke.co.smartroundclinic.consultation.presentation.controller

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.consultation.domain.service.ConsultationSocketRegistry
import ke.co.smartroundclinic.consultation.domain.usecase.chat.GetMergedConsultationHistoryUseCase
import ke.co.smartroundclinic.consultation.domain.usecase.chat.HideConversationThreadUseCase
import ke.co.smartroundclinic.consultation.domain.usecase.chat.ListConversationThreadsUseCase
import ke.co.smartroundclinic.consultation.domain.usecase.chat.MarkThreadReadUseCase
import ke.co.smartroundclinic.consultation.presentation.dto.response.ConsultationReadReceiptEventRes
import ke.co.smartroundclinic.infra.plugins.MissingParametersException
import ke.co.smartroundclinic.infra.plugins.getUserId
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }

/**
 * Doctor-patient conversation aggregation — one entry per (doctorId, patientId) pair, merging all
 * of that pair's consultations. Sits alongside [consultationChatController]/[consultationSessionController]
 * which remain session-scoped (sending, calls) and are untouched by this.
 */
fun Route.conversationThreadController(
    listThreadsUseCase: ListConversationThreadsUseCase,
    getMergedHistoryUseCase: GetMergedConsultationHistoryUseCase,
    markThreadReadUseCase: MarkThreadReadUseCase,
    hideConversationThreadUseCase: HideConversationThreadUseCase,
    socketRegistry: ConsultationSocketRegistry,
) {
    authenticate("auth-jwt") {
        route("/consultation/threads") {

            // GET /consultation/threads
            // One row per doctor/patient pair the caller participates in — the chat list data source.
            get {
                val userId = call.getUserId() ?: return@get
                val role = call.principal<JWTPrincipal>()?.payload?.getClaim("role")?.asString() ?: ""
                val result = listThreadsUseCase(userId, role)
                call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
            }

            // GET /consultation/threads/{doctorId}/{patientId}/messages?before=<cursor>&size=50
            // Merged, cursor-paginated history across every consultation for this pair, newest first.
            get("{doctorId}/{patientId}/messages") {
                val doctorId = call.parameters["doctorId"]
                    ?: throw MissingParametersException("doctorId path parameter is required")
                val patientId = call.parameters["patientId"]
                    ?: throw MissingParametersException("patientId path parameter is required")
                val userId = call.getUserId() ?: return@get
                if (userId != doctorId && userId != patientId) {
                    return@get call.respond(HttpStatusCode.Forbidden, mapOf("message" to "Not a participant"))
                }

                val before = call.request.queryParameters["before"]
                val size = call.request.queryParameters["size"]?.toIntOrNull() ?: 50
                val result = getMergedHistoryUseCase(doctorId, patientId, before, size, requesterId = userId)

                // Only the initial (non-paginated) open of a conversation counts as "read" —
                // scrolling up to load older history must not re-trigger it.
                if (before == null) {
                    runCatching {
                        markThreadReadAndRelay(markThreadReadUseCase, socketRegistry, doctorId, patientId, userId)
                    }
                }

                call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
            }

            // DELETE /consultation/threads/{doctorId}/{patientId}
            // Hides the thread from the caller's own list only — reappears automatically if the
            // other participant sends a new message afterwards (see ListConversationThreadsUseCase).
            delete("{doctorId}/{patientId}") {
                val doctorId = call.parameters["doctorId"]
                    ?: throw MissingParametersException("doctorId path parameter is required")
                val patientId = call.parameters["patientId"]
                    ?: throw MissingParametersException("patientId path parameter is required")
                val userId = call.getUserId() ?: return@delete
                if (userId != doctorId && userId != patientId) {
                    return@delete call.respond(HttpStatusCode.Forbidden, mapOf("message" to "Not a participant"))
                }

                val result = hideConversationThreadUseCase(userId, doctorId, patientId)
                call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
            }
        }
    }
}

private suspend fun markThreadReadAndRelay(
    markThreadReadUseCase: MarkThreadReadUseCase,
    socketRegistry: ConsultationSocketRegistry,
    doctorId: String,
    patientId: String,
    readerId: String,
) {
    val readState = markThreadReadUseCase(doctorId, patientId, readerId)
    val lastReadAt = (readState as? Resource.Success)?.data?.let {
        if (readerId == doctorId) it.doctorLastReadAt else it.patientLastReadAt
    } ?: return

    val counterpartId = if (readerId == doctorId) patientId else doctorId
    val threadKey = ConsultationSocketRegistry.threadKey(doctorId, patientId)
    socketRegistry.sendToUser(
        threadKey,
        counterpartId,
        json.encodeToString(
            ConsultationReadReceiptEventRes(doctorId = doctorId, patientId = patientId, readerId = readerId, lastReadAt = lastReadAt)
        ),
    )
}
