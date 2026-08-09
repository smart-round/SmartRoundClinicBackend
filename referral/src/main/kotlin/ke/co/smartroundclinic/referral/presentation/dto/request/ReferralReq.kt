package ke.co.smartroundclinic.referral.presentation.dto.request

import kotlinx.serialization.Serializable

@Serializable
data class CreateReferralReq(
    val appointmentId: String,
    val receivingDoctorId: String,
    // Defaulted so a client that omits it still deserializes. The doctor app stopped collecting a
    // reason; the stored column stays non-null and simply holds an empty string.
    val reason: String = "",
)
