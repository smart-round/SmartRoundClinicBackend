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

        // Resolve or provision the single shared Cloudflare meeting for this consultation.
        //
        // Both doctor and patient call this endpoint — the consultation's videoRoomId
        // is the coordination point.  Rules:
        //   1. If videoRoomId already exists → try to use it directly (don't recreate).
        //   2. If videoRoomId is null → create a new meeting and store it atomically
        //      (setVideoRoomIdIfAbsent) so a concurrent call from the other participant
        //      gets the same meeting ID instead of creating a second one.
        //   3. If addParticipant fails (meeting deleted/inactive in Cloudflare) →
        //      clear the stale ID, create a fresh meeting using the same atomic write,
        //      and retry once.
        var meetingId: String = when (val existingId = session.videoRoomId) {
            null -> {
                val newId = createMeeting(session.id) ?: return DefaultResponse(
                    httpStatusCode = HttpStatusCode.BadGateway.value,
                    status = false,
                    message = "Failed to create meeting",
                    data = null,
                )
                when (val r = sessions.setVideoRoomIdIfAbsent(session.id, newId)) {
                    is Resource.Success -> r.data!!  // either newId or the winner's id
                    is Resource.Error -> return DefaultResponse(
                        httpStatusCode = HttpStatusCode.InternalServerError.value,
                        status = false,
                        message = r.message ?: "Failed to persist meeting",
                        data = null,
                    )
                }
            }
            else -> existingId
        }

        val participant = client.addParticipant(
            meetingId = meetingId,
            customParticipantId = userId,
            presetName = preset,
            name = displayName,
            picture = null,
        )

        // Meeting was deleted or went inactive in Cloudflare — clear the stale ID,
        // create a fresh meeting, and retry once.
        val resolvedParticipant = if (participant is Resource.Error) {
            sessions.clearVideoRoomId(session.id)
            val freshId = createMeeting(session.id) ?: return DefaultResponse(
                httpStatusCode = HttpStatusCode.BadGateway.value,
                status = false,
                message = "Failed to recreate meeting",
                data = null,
            )
            when (val r = sessions.setVideoRoomIdIfAbsent(session.id, freshId)) {
                is Resource.Success -> meetingId = r.data!!
                is Resource.Error -> return DefaultResponse(
                    httpStatusCode = HttpStatusCode.InternalServerError.value,
                    status = false,
                    message = r.message ?: "Failed to persist meeting",
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
