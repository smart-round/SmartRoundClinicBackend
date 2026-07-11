package ke.co.smartroundclinic.doctor.domain.model

import ke.co.smartroundclinic.doctor.data.entity.ComplianceCorrectionEntity

data class ComplianceCorrection(
    val id: String,
    val doctorId: String,
    val complianceId: String,
    val rejectionReason: String,
    val status: String,
    val submittedAt: String,
    val reviewedAt: String?,
    val reviewedBy: String?,
)

fun ComplianceCorrectionEntity.toModel() = ComplianceCorrection(
    id = id,
    doctorId = doctorId,
    complianceId = complianceId,
    rejectionReason = rejectionReason,
    status = status,
    submittedAt = submittedAt,
    reviewedAt = reviewedAt,
    reviewedBy = reviewedBy,
)
