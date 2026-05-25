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
    /** Creates a Cloudflare meeting and stores its ID atomically.
     *  If two requests race, both end up using the same winning meeting ID. */
    private suspend fun createAndStore(sessionId: String): Resource<String> {
        val newId = when (val r = client.createMeeting(title = "Consultation $sessionId")) {
            is Resource.Success -> r.data?.takeIf { it.isNotBlank() }
                ?: return Resource.Error("Cloudflare returned no meeting id")
            is Resource.Error -> return Resource.Error(r.message ?: "Failed to create meeting")
        }
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

        // Resolve the single shared Cloudflare meeting for this consultation.
        //
        // The key invariant: the client SDK (RealtimeKit) can only connect to a meeting
        // that Cloudflare considers LIVE.  A meeting becomes LIVE once the first participant
        // connects via WebRTC.  Once everyone leaves it becomes INACTIVE.  Calling
        // addParticipant on an INACTIVE meeting succeeds at the REST level but the SDK
        // will reject the returned token with "Resource NotFound: Participant not found".
        //
        // Strategy:
        //  1. If the stored meeting is LIVE → reuse it.
        //  2. If INACTIVE / missing / no meeting stored → create a fresh meeting.
        //     Use setVideoRoomIdIfAbsent so that two concurrent requests (doctor and
        //     patient both pressing "Join" at the same time) coordinate: only one meeting
        //     is created; the other request reuses the winner's meeting ID.
        //  3. If addParticipant still fails on the resolved meeting → safety-net retry
        //     with another fresh meeting (handles very unlikely race windows).
        var meetingId: String = when (val existingId = session.videoRoomId) {
            null -> {
                when (val r = createAndStore(session.id)) {
                    is Resource.Success -> r.data!!
                    is Resource.Error -> return DefaultResponse(
                        httpStatusCode = HttpStatusCode.BadGateway.value,
                        status = false,
                        message = r.message ?: "Failed to create meeting",
                        data = null,
                    )
                }
            }
            else -> {
                val status = client.getMeetingStatus(existingId)
                if (status == "LIVE") {
                    existingId
                } else {
                    // INACTIVE or gone — must create a fresh meeting.
                    // clearVideoRoomId first so setVideoRoomIdIfAbsent treats the slot as empty.
                    sessions.clearVideoRoomId(session.id)
                    when (val r = createAndStore(session.id)) {
                        is Resource.Success -> r.data!!
                        is Resource.Error -> return DefaultResponse(
                            httpStatusCode = HttpStatusCode.BadGateway.value,
                            status = false,
                            message = r.message ?: "Failed to recreate meeting",
                            data = null,
                        )
                    }
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

        // Safety-net retry: if addParticipant still fails (e.g. a race window between
        // the status check and the actual call), create one final fresh meeting.
        val resolvedParticipant = if (participant is Resource.Error) {
            sessions.clearVideoRoomId(session.id)
            when (val r = createAndStore(session.id)) {
                is Resource.Success -> meetingId = r.data!!
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
