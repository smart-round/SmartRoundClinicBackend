package ke.co.smartroundclinic.consultation.domain.usecase.call

import io.ktor.http.HttpStatusCode
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.consultation.domain.repository.ConsultationMessageRepository
import ke.co.smartroundclinic.consultation.domain.repository.ConsultationSessionRepository
import ke.co.smartroundclinic.consultation.presentation.dto.response.JoinCallRes
import ke.co.smartroundclinic.infra.AppConfig
import ke.co.smartroundclinic.infra.realtime.RealtimeKitClient

/**
 * Resolves (or lazily provisions) the Cloudflare RealtimeKit meeting tied to a
 * consultation session and returns this user's participant join token.
 *
 * Flow:
 *   1. Verify the caller is the doctor or patient on the session
 *   2. If the session has no `videoRoomId`, create a meeting on Cloudflare and persist the id
 *   3. Call Cloudflare's add-participant API with the role-appropriate preset
 *   4. Return `{ meetingId, participantId, authToken, presetName }`
 */
class JoinConsultationCallUseCase(
    private val sessions: ConsultationSessionRepository,
    private val messages: ConsultationMessageRepository,
    private val client: RealtimeKitClient,
) {
    private suspend fun createMeeting(sessionId: String): String? =
        when (val r = client.createMeeting(title = "Consultation $sessionId")) {
            is Resource.Success -> r.data?.takeIf { it.isNotBlank() }
            is Resource.Error -> null
        }

    suspend operator fun invoke(consultationId: String, userId: String): DefaultResponse<JoinCallRes?> {
        val session = when (val r = sessions.getById(consultationId)) {
            is Resource.Success -> r.data
            is Resource.Error -> return DefaultResponse(
                httpStatusCode = HttpStatusCode.InternalServerError.value,
                status = false,
                message = r.message ?: "Failed to load consultation",
                data = null,
            )
        } ?: return DefaultResponse(
            httpStatusCode = HttpStatusCode.NotFound.value,
            status = false,
            message = "Consultation not found",
            data = null,
        )

        if (userId != session.doctorId && userId != session.patientId) {
            return DefaultResponse(
                httpStatusCode = HttpStatusCode.Forbidden.value,
                status = false,
                message = "Not a participant of this consultation",
                data = null,
            )
        }

        val isDoctor = userId == session.doctorId
        val preset = if (isDoctor) AppConfig.realtimeKit.doctorPreset else AppConfig.realtimeKit.patientPreset
        val displayName = messages.getUserName(userId)

        // Resolve or provision the Cloudflare meeting.
        // If a videoRoomId is stored, check its status first:
        //   - LIVE      → reuse it
        //   - INACTIVE or not found → clear the stale ID and create a fresh meeting
        // If no videoRoomId → create a new meeting.
        var meetingId: String = when (val existingId = session.videoRoomId) {
            null -> {
                val id = createMeeting(session.id) ?: return DefaultResponse(
                    httpStatusCode = HttpStatusCode.BadGateway.value,
                    status = false,
                    message = "Failed to create meeting",
                    data = null,
                )
                sessions.setVideoRoomId(session.id, id)
                id
            }
            else -> {
                val status = client.getMeetingStatus(existingId)
                if (status == "LIVE") {
                    existingId
                } else {
                    // Meeting is INACTIVE or no longer exists — start fresh
                    sessions.clearVideoRoomId(session.id)
                    val id = createMeeting(session.id) ?: return DefaultResponse(
                        httpStatusCode = HttpStatusCode.BadGateway.value,
                        status = false,
                        message = "Failed to create meeting",
                        data = null,
                    )
                    sessions.setVideoRoomId(session.id, id)
                    id
                }
            }
        }

        val participant = client.addParticipant(
            meetingId = meetingId,
            customParticipantId = userId,
            presetName = preset,
            name = displayName,
            picture = null,
        )

        // Last-resort recovery: if addParticipant still fails (e.g. race condition),
        // create one more fresh meeting and retry.
        val resolvedParticipant = if (participant is Resource.Error) {
            sessions.clearVideoRoomId(session.id)
            val freshId = createMeeting(session.id) ?: return DefaultResponse(
                httpStatusCode = HttpStatusCode.BadGateway.value,
                status = false,
                message = "Failed to recreate meeting",
                data = null,
            )
            sessions.setVideoRoomId(session.id, freshId)
            meetingId = freshId
            client.addParticipant(
                meetingId = freshId,
                customParticipantId = userId,
                presetName = preset,
                name = displayName,
                picture = null,
            )
        } else participant

        return when (resolvedParticipant) {
            is Resource.Success -> {
                val p = resolvedParticipant.data!!
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
