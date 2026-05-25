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
    // Meeting title is derived from the consultation id — unique and stable.
    private fun meetingTitle(sessionId: String) = "Consultation $sessionId"

    /**
     * Resolves the meeting id to use for this consultation:
     *
     *  1. If DB has a videoRoomId AND Cloudflare reports it as LIVE → reuse it.
     *  2. Otherwise ask Cloudflare for any LIVE meeting whose title matches
     *     this consultation's title — if one exists another request already
     *     created it, just join that one.
     *  3. If still nothing → create a new meeting and store it atomically
     *     (setVideoRoomIdIfAbsent) so concurrent requests all land on the same id.
     */
    private suspend fun resolveMeetingId(sessionId: String, currentStoredId: String?): Resource<String> {
        val title = meetingTitle(sessionId)

        // Step 1 — fast path: stored id is still LIVE
        if (currentStoredId != null && client.getMeetingStatus(currentStoredId) == "LIVE") {
            return Resource.Success(currentStoredId)
        }

        // Step 2 — ask Cloudflare: is there already a LIVE meeting for this consultation?
        val liveId = client.findLiveMeetingByTitle(title)
        if (liveId != null) {
            sessions.setVideoRoomId(sessionId, liveId)   // keep DB in sync
            return Resource.Success(liveId)
        }

        // Step 3 — nothing live: create a fresh meeting
        if (currentStoredId != null) sessions.clearVideoRoomId(sessionId)
        val newId = when (val r = client.createMeeting(title)) {
            is Resource.Success -> r.data?.takeIf { it.isNotBlank() }
                ?: return Resource.Error("Cloudflare returned no meeting id")
            is Resource.Error -> return Resource.Error(r.message ?: "Failed to create meeting")
        }
        // Atomic store — if another request already stored a meeting, use that one instead
        return sessions.setVideoRoomIdIfAbsent(sessionId, newId)
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

        var meetingId = when (val r = resolveMeetingId(session.id, session.videoRoomId)) {
            is Resource.Success -> r.data!!
            is Resource.Error -> return DefaultResponse(
                httpStatusCode = HttpStatusCode.BadGateway.value,
                status = false,
                message = r.message ?: "Failed to resolve meeting",
                data = null,
            )
        }

        val participant = client.addParticipant(
            meetingId = meetingId,
            customParticipantId = userId,
            presetName = preset,
            name = displayName,
            picture = null,
        )

        // Safety-net: addParticipant still failed — resolve once more and retry
        val resolvedParticipant = if (participant is Resource.Error) {
            sessions.clearVideoRoomId(session.id)
            meetingId = when (val r = resolveMeetingId(session.id, null)) {
                is Resource.Success -> r.data!!
                is Resource.Error -> return DefaultResponse(
                    httpStatusCode = HttpStatusCode.BadGateway.value,
                    status = false,
                    message = r.message ?: "Failed to recreate meeting",
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
