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

        // Provision the Cloudflare meeting on first join
        val meetingId = session.videoRoomId ?: run {
            val created = client.createMeeting(title = "Consultation ${session.id}")
            when (created) {
                is Resource.Success -> {
                    val id = created.data ?: return DefaultResponse(
                        httpStatusCode = HttpStatusCode.BadGateway.value,
                        status = false,
                        message = "Failed to create meeting",
                        data = null,
                    )
                    sessions.setVideoRoomId(session.id, id)
                    id
                }
                is Resource.Error -> return DefaultResponse(
                    httpStatusCode = HttpStatusCode.BadGateway.value,
                    status = false,
                    message = created.message ?: "Failed to create meeting",
                    data = null,
                )
            }
        }

        val isDoctor = userId == session.doctorId
        val preset = if (isDoctor) AppConfig.realtimeKit.doctorPreset else AppConfig.realtimeKit.patientPreset
        val displayName = messages.getUserName(userId)

        return when (val participant = client.addParticipant(
            meetingId = meetingId,
            customParticipantId = userId,
            presetName = preset,
            name = displayName,
            picture = null,
        )) {
            is Resource.Success -> {
                val p = participant.data!!
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
                message = participant.message ?: "Failed to add participant",
                data = null,
            )
        }
    }
}
