package ke.co.smartroundclinic.referral.presentation.dto.response

import ke.co.smartroundclinic.referral.domain.model.Referral
import kotlinx.serialization.Serializable

@Serializable
data class ReferralRes(
    val id: String,
    val sourceAppointmentId: String,
    val referringDoctorId: String,
    val referringDoctorName: String?,
    val referringDoctorPicture: String?,
    val patientId: String,
    val patientName: String? = null,
    val patientProfilePicture: String? = null,
    val receivingDoctorId: String,
    val receivingDoctorName: String? = null,
    val receivingDoctorPicture: String? = null,
    val reason: String,
    val status: String,
    val resultingAppointmentId: String?,
    val createdAt: String,
    val respondedAt: String?,
)

fun Referral.toRes(
    patientName: String? = null,
    patientProfilePicture: String? = null,
    receivingDoctorName: String? = null,
    receivingDoctorPicture: String? = null,
) = ReferralRes(
    id = id,
    sourceAppointmentId = sourceAppointmentId,
    referringDoctorId = referringDoctorId,
    referringDoctorName = referringDoctorName,
    referringDoctorPicture = referringDoctorPicture,
    patientId = patientId,
    patientName = patientName,
    patientProfilePicture = patientProfilePicture,
    receivingDoctorId = receivingDoctorId,
    receivingDoctorName = receivingDoctorName,
    receivingDoctorPicture = receivingDoctorPicture,
    reason = reason,
    status = status,
    resultingAppointmentId = resultingAppointmentId,
    createdAt = createdAt,
    respondedAt = respondedAt,
)

@Serializable
data class ReferralEligibilityRes(
    val eligible: Boolean,
    val reasons: List<String>,
)

@Serializable
data class AdminReferralsPageRes(
    val items: List<ReferralRes>,
    val total: Long,
    val page: Int,
    val size: Int,
    val pages: Long,
)

@Serializable
data class ReferralStatsRes(
    val total: Int,
    val pending: Int,
    val accepted: Int,
    val declined: Int,
    val acceptanceRate: Double,
)
