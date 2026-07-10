package ke.co.smartroundclinic.consultation.presentation.controller

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import ke.co.smartroundclinic.consultation.domain.usecase.chat.GetMergedConsultationHistoryUseCase
import ke.co.smartroundclinic.consultation.domain.usecase.chat.ListConversationThreadsUseCase
import ke.co.smartroundclinic.infra.plugins.MissingParametersException
import ke.co.smartroundclinic.infra.plugins.getUserId

/**
 * Doctor-patient conversation aggregation — one entry per (doctorId, patientId) pair, merging all
 * of that pair's consultations. Sits alongside [consultationChatController]/[consultationSessionController]
 * which remain session-scoped (sending, calls) and are untouched by this.
 */
fun Route.conversationThreadController(
    listThreadsUseCase: ListConversationThreadsUseCase,
    getMergedHistoryUseCase: GetMergedConsultationHistoryUseCase,
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
                val result = getMergedHistoryUseCase(doctorId, patientId, before, size)
                call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
            }
        }
    }
}
