package ke.co.smartroundclinic.doctorchat.presentation.controller

import kotlinx.serialization.Serializable
import ke.co.smartroundclinic.common.DefaultResponse
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.websocket.webSocket
import io.ktor.utils.io.toByteArray
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.doctorchat.domain.repository.DoctorChatThreadRepository
import ke.co.smartroundclinic.doctorchat.domain.service.DoctorChatService
import ke.co.smartroundclinic.doctorchat.domain.service.DoctorChatSocketRegistry
import ke.co.smartroundclinic.doctorchat.domain.usecase.GetDoctorChatHistoryUseCase
import ke.co.smartroundclinic.doctorchat.domain.usecase.GetMyDoctorChatThreadsUseCase
import ke.co.smartroundclinic.doctorchat.domain.usecase.InitiateDoctorChatUseCase
import ke.co.smartroundclinic.doctorchat.domain.usecase.call.CancelDoctorCallInviteUseCase
import ke.co.smartroundclinic.doctorchat.domain.usecase.call.DeclineDoctorCallInviteUseCase
import ke.co.smartroundclinic.doctorchat.domain.usecase.call.EndDoctorCallUseCase
import ke.co.smartroundclinic.doctorchat.domain.usecase.call.InviteToDoctorCallUseCase
import ke.co.smartroundclinic.doctorchat.domain.usecase.call.JoinDoctorCallUseCase
import ke.co.smartroundclinic.doctorchat.presentation.dto.request.DoctorCallActionReq
import ke.co.smartroundclinic.doctorchat.presentation.dto.request.InitiateDoctorChatReq
import ke.co.smartroundclinic.doctorchat.presentation.dto.request.InviteToDoctorCallReq
import ke.co.smartroundclinic.doctorchat.presentation.dto.response.DoctorPresenceEventRes
import ke.co.smartroundclinic.doctorchat.presentation.dto.response.toRes
import ke.co.smartroundclinic.infra.plugins.MissingParametersException
import ke.co.smartroundclinic.infra.plugins.getUserId
import ke.co.smartroundclinic.infra.plugins.requireRole
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val DOCTOR = "DOCTOR"
private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

fun Route.doctorChatController(
    initiateUseCase: InitiateDoctorChatUseCase,
    listThreadsUseCase: GetMyDoctorChatThreadsUseCase,
    getHistoryUseCase: GetDoctorChatHistoryUseCase,
    chatService: DoctorChatService,
    threadRepository: DoctorChatThreadRepository,
    socketRegistry: DoctorChatSocketRegistry,
    joinCallUseCase: JoinDoctorCallUseCase,
    inviteToCallUseCase: InviteToDoctorCallUseCase,
    declineCallInviteUseCase: DeclineDoctorCallInviteUseCase,
    cancelCallInviteUseCase: CancelDoctorCallInviteUseCase,
    endCallUseCase: EndDoctorCallUseCase,
) {
    authenticate("auth-jwt") {
        route("/doctor-chat/threads") {

            // POST /doctor-chat/threads — "Connect": find-or-create the thread with another doctor.
            post {
                call.requireRole(DOCTOR) {
                    val callerId = call.getUserId() ?: return@requireRole
                    val body = call.receive<InitiateDoctorChatReq>()
                    val result = initiateUseCase(callerId, body.otherDoctorId)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            // GET /doctor-chat/threads — this doctor's thread list, for the Consultations sub-tab.
            get {
                call.requireRole(DOCTOR) {
                    val callerId = call.getUserId() ?: return@requireRole
                    val result = listThreadsUseCase(callerId)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            // GET /doctor-chat/threads/{threadId}/messages?before=<cursor>&size=50
            get("{threadId}/messages") {
                call.requireRole(DOCTOR) {
                    val threadId = call.parameters["threadId"] ?: throw MissingParametersException("threadId path parameter is required")
                    val callerId = call.getUserId() ?: return@requireRole
                    if (!isParticipant(threadRepository, threadId, callerId)) {
                        return@requireRole call.respond(HttpStatusCode.Forbidden, mapOf("message" to "Not a participant of this thread"))
                    }
                    val before = call.request.queryParameters["before"]
                    val size = call.request.queryParameters["size"]?.toIntOrNull() ?: 50
                    val result = getHistoryUseCase(threadId, before, size)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            // Step 1 of the direct-to-R2 upload: mint a pre-signed PUT URL. Preferred over the
            // multipart route below, which proxies the whole file through this process and holds
            // it in heap before forwarding it to R2.
            post("{threadId}/files/presign") {
                call.requireRole(DOCTOR) {
                    val threadId = call.parameters["threadId"] ?: throw MissingParametersException("threadId path parameter is required")
                    val callerId = call.getUserId() ?: return@requireRole
                    requireParticipantThread(threadRepository, threadId, callerId) ?: run {
                        call.respond(HttpStatusCode.Forbidden, mapOf("message" to "Not a participant of this thread"))
                        return@requireRole
                    }

                    val req = call.receive<DoctorChatPresignUploadReq>()
                    if (req.sizeBytes <= 0) {
                        return@requireRole call.respond(HttpStatusCode.BadRequest, mapOf("message" to "sizeBytes must be positive"))
                    }
                    if (req.sizeBytes > MAX_DOCTOR_CHAT_FILE_BYTES) {
                        return@requireRole call.respond(
                            HttpStatusCode.PayloadTooLarge,
                            mapOf("message" to "Unable to send file as it is too large. Please try again"),
                        )
                    }

                    when (val result = chatService.presignFileUpload(threadId, req.fileName, req.contentType)) {
                        is Resource.Success -> {
                            val data = result.data!!
                            call.respond(
                                HttpStatusCode.OK,
                                DefaultResponse(
                                    httpStatusCode = HttpStatusCode.OK.value,
                                    message = "Upload URL generated",
                                    status = true,
                                    data = DoctorChatPresignUploadRes(
                                        messageId = data.messageId,
                                        key = data.key,
                                        uploadUrl = data.uploadUrl,
                                        contentType = data.contentType,
                                    ),
                                ),
                            )
                        }
                        is Resource.Error -> call.respond(
                            HttpStatusCode.InternalServerError,
                            mapOf("message" to (result.message ?: "Failed to prepare upload")),
                        )
                    }
                }
            }

            // Step 2: record the message once the client's PUT to R2 has succeeded.
            post("{threadId}/files/complete") {
                call.requireRole(DOCTOR) {
                    val threadId = call.parameters["threadId"] ?: throw MissingParametersException("threadId path parameter is required")
                    val callerId = call.getUserId() ?: return@requireRole
                    val thread = requireParticipantThread(threadRepository, threadId, callerId) ?: run {
                        call.respond(HttpStatusCode.Forbidden, mapOf("message" to "Not a participant of this thread"))
                        return@requireRole
                    }
                    val otherDoctorId = if (callerId == thread.doctorAId) thread.doctorBId else thread.doctorAId

                    val req = call.receive<DoctorChatCompleteUploadReq>()
                    val senderName = chatService.getUserName(callerId) ?: "Unknown"
                    val result = chatService.completeFileUpload(
                        threadId = threadId,
                        senderId = callerId,
                        senderName = senderName,
                        recipientId = otherDoctorId,
                        messageId = req.messageId,
                        key = req.key,
                        fileName = req.fileName,
                        contentType = req.contentType,
                        sizeBytes = req.sizeBytes,
                    )
                    val response = result.toDefaultResponse(
                        successStatusCode = HttpStatusCode.Created.value,
                        failedStatusCode = HttpStatusCode.BadRequest.value,
                    ) { it?.toModel()?.toRes() }
                    call.respond(HttpStatusCode.fromValue(response.httpStatusCode), response)
                }
            }

            // POST /doctor-chat/threads/{threadId}/files — multipart upload, broadcast via change stream.
            post("{threadId}/files") {
                call.requireRole(DOCTOR) {
                    val threadId = call.parameters["threadId"] ?: throw MissingParametersException("threadId path parameter is required")
                    val callerId = call.getUserId() ?: return@requireRole
                    val thread = requireParticipantThread(threadRepository, threadId, callerId) ?: run {
                        call.respond(HttpStatusCode.Forbidden, mapOf("message" to "Not a participant of this thread"))
                        return@requireRole
                    }
                    val otherDoctorId = if (callerId == thread.doctorAId) thread.doctorBId else thread.doctorAId

                    var fileBytes: ByteArray? = null
                    var fileName = "file"
                    var contentType = "application/octet-stream"
                    call.receiveMultipart().forEachPart { part ->
                        when (part) {
                            is PartData.FileItem -> {
                                fileName = part.originalFileName?.ifBlank { null } ?: fileName
                                contentType = part.contentType?.toString()?.ifBlank { null } ?: contentType
                                fileBytes = part.provider().toByteArray()
                            }
                            is PartData.FormItem -> when (part.name) {
                                "fileName" -> fileName = part.value.ifBlank { fileName }
                                "contentType" -> contentType = part.value.ifBlank { contentType }
                            }
                            else -> Unit
                        }
                        part.dispose()
                    }

                    val bytes = fileBytes
                    if (bytes == null || bytes.isEmpty()) {
                        return@requireRole call.respond(HttpStatusCode.BadRequest, mapOf("message" to "file part is required"))
                    }

                    val senderName = chatService.getUserName(callerId) ?: "Unknown"
                    val result = chatService.uploadFile(threadId, callerId, senderName, otherDoctorId, fileName, contentType, bytes)
                    val response = result.toDefaultResponse(
                        successStatusCode = HttpStatusCode.Created.value,
                        failedStatusCode = HttpStatusCode.InternalServerError.value,
                    ) { it?.toModel()?.toRes() }
                    call.respond(HttpStatusCode.fromValue(response.httpStatusCode), response)
                }
            }

            // POST /doctor-chat/threads/{threadId}/call/join
            post("{threadId}/call/join") {
                call.requireRole(DOCTOR) {
                    val threadId = call.parameters["threadId"] ?: throw MissingParametersException("threadId path parameter is required")
                    val callerId = call.getUserId() ?: return@requireRole
                    val req = call.receive<DoctorCallActionReq>()
                    val result = joinCallUseCase(threadId, callerId, req.callId)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            // POST /doctor-chat/threads/{threadId}/call/invite
            post("{threadId}/call/invite") {
                call.requireRole(DOCTOR) {
                    val threadId = call.parameters["threadId"] ?: throw MissingParametersException("threadId path parameter is required")
                    val callerId = call.getUserId() ?: return@requireRole
                    val req = runCatching { call.receive<InviteToDoctorCallReq>() }.getOrDefault(InviteToDoctorCallReq())
                    val result = inviteToCallUseCase(threadId, callerId, req.isVideo)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            // POST /doctor-chat/threads/{threadId}/call/decline  (callee only)
            post("{threadId}/call/decline") {
                call.requireRole(DOCTOR) {
                    val callerId = call.getUserId() ?: return@requireRole
                    val req = call.receive<DoctorCallActionReq>()
                    val result = declineCallInviteUseCase(req.callId, callerId)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            // POST /doctor-chat/threads/{threadId}/call/cancel  (caller only)
            post("{threadId}/call/cancel") {
                call.requireRole(DOCTOR) {
                    val callerId = call.getUserId() ?: return@requireRole
                    val req = call.receive<DoctorCallActionReq>()
                    val result = cancelCallInviteUseCase(req.callId, callerId)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            // POST /doctor-chat/threads/{threadId}/call/end  (either participant)
            post("{threadId}/call/end") {
                call.requireRole(DOCTOR) {
                    val threadId = call.parameters["threadId"] ?: throw MissingParametersException("threadId path parameter is required")
                    val callerId = call.getUserId() ?: return@requireRole
                    val result = endCallUseCase(threadId, callerId)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }
        }

        // WS /doctor-chat/threads/{threadId} — real-time text chat. Send {"type":"TEXT","message":"..."}.
        // Files go through POST .../files above.
        webSocket("/doctor-chat/threads/{threadId}") {
            val threadId = call.parameters["threadId"] ?: run {
                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Missing threadId"))
                return@webSocket
            }
            val principal = call.principal<JWTPrincipal>() ?: run {
                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Unauthorized"))
                return@webSocket
            }
            val userId = principal.payload.getClaim("userId")?.asString() ?: run {
                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Invalid token"))
                return@webSocket
            }
            val thread = requireParticipantThread(threadRepository, threadId, userId) ?: run {
                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Not a participant of this thread"))
                return@webSocket
            }
            val otherDoctorId = if (userId == thread.doctorAId) thread.doctorBId else thread.doctorAId
            val senderName = chatService.getUserName(userId) ?: "Unknown"

            socketRegistry.register(threadId, userId, this)
            var watchJob: Job? = null
            var presenceJob: Job? = null
            try {
                watchJob = launch {
                    try {
                        chatService.watchMessagesForThread(threadId).collect { msg ->
                            val resolved = chatService.resolveEntityFiles(msg)
                            send(Frame.Text(json.encodeToString(resolved.toModel().toRes())))
                        }
                    } catch (_: Exception) {
                        // change stream interrupted — client reconnects
                    }
                }

                // Poll the counterpart's global presence and relay changes over this socket — presence
                // itself is tracked by the separate /auth/user/presence heartbeat, this just surfaces it here.
                presenceJob = launch {
                    var lastKnownOnline: Boolean? = null
                    while (isActive) {
                        try {
                            val online = chatService.isOnline(otherDoctorId)
                            if (online != lastKnownOnline) {
                                lastKnownOnline = online
                                val lastSeenAt = if (!online) chatService.getLastSeenAt(otherDoctorId) else null
                                send(Frame.Text(json.encodeToString(
                                    DoctorPresenceEventRes(userId = otherDoctorId, isOnline = online, lastSeenAt = lastSeenAt)
                                )))
                            }
                        } catch (_: Exception) {
                            // transient Redis/Mongo hiccup — try again next tick
                        }
                        delay(9_000L)
                    }
                }

                for (frame in incoming) {
                    if (frame is Frame.Text) {
                        try {
                            chatService.handleIncomingMessage(
                                threadId = threadId,
                                senderId = userId,
                                senderName = senderName,
                                rawJson = frame.readText(),
                                recipientId = otherDoctorId,
                            )
                        } catch (_: Exception) {
                            send(Frame.Text("""{"error":"Failed to process message"}"""))
                        }
                    }
                }
            } finally {
                watchJob?.cancel()
                presenceJob?.cancel()
                socketRegistry.unregister(threadId, userId, this)
            }
        }
    }
}

private suspend fun isParticipant(threadRepository: DoctorChatThreadRepository, threadId: String, userId: String): Boolean =
    requireParticipantThreadInternal(threadRepository, threadId, userId) != null

private suspend fun requireParticipantThread(threadRepository: DoctorChatThreadRepository, threadId: String, userId: String) =
    requireParticipantThreadInternal(threadRepository, threadId, userId)

private suspend fun requireParticipantThreadInternal(threadRepository: DoctorChatThreadRepository, threadId: String, userId: String) =
    (threadRepository.getById(threadId) as? Resource.Success)?.data?.takeIf { it.doctorAId == userId || it.doctorBId == userId }

/** Mirrors the consultation module's ceiling — see MAX_CHAT_FILE_BYTES there. */
private const val MAX_DOCTOR_CHAT_FILE_BYTES = 300L * 1_000_000

@Serializable
private data class DoctorChatPresignUploadReq(
    val fileName: String,
    val contentType: String,
    val sizeBytes: Long,
)

@Serializable
private data class DoctorChatPresignUploadRes(
    val messageId: String,
    val key: String,
    val uploadUrl: String,
    val contentType: String,
)

@Serializable
private data class DoctorChatCompleteUploadReq(
    val messageId: String,
    val key: String,
    val fileName: String,
    val contentType: String,
    val sizeBytes: Long,
)
