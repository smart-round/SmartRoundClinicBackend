package ke.co.smartroundclinic.doctor.presentation.dto.response

import ke.co.smartroundclinic.doctor.domain.model.ComplianceCorrection

data class ComplianceCorrectionRes(
    val id: String,
    val doctorId: String,
    val complianceId: String,
    val rejectionReason: String,
    val status: String,
    val submittedAt: String,
    val reviewedAt: String?,
    val reviewedBy: String?,
    val doctorProfile: DoctorProfileInfo?,
)

data class ComplianceCorrectionsPageRes(
    val items: List<ComplianceCorrectionRes>,
    val total: Long,
    val page: Int,
    val size: Int,
    val pages: Long,
)

fun ComplianceCorrection.toRes(doctorProfile: DoctorProfileInfo? = null) = ComplianceCorrectionRes(
    id = id,
    doctorId = doctorId,
    complianceId = complianceId,
    rejectionReason = rejectionReason,
    status = status,
    submittedAt = submittedAt,
    reviewedAt = reviewedAt,
    reviewedBy = reviewedBy,
    doctorProfile = doctorProfile,
)
