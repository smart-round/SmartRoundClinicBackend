package ke.co.smartroundclinic.referral.presentation.dto.request

import kotlinx.serialization.Serializable

@Serializable
data class CreateReferralReq(
    val appointmentId: String,
    val receivingDoctorId: String,
    val reason: String,
)
