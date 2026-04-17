package ke.co.smartroundclinic.doctor.domain.model

import ke.co.smartroundclinic.doctor.data.entity.ComplianceEntity

data class Compliance(
    val id: String,
    val doctorId: String,
    val isApproved: Boolean,
    val approvedAt: String?,
    val approvedBy: String?,
    val failedApprovalReason: String?,
    val createdAt: String,
    val updatedAt: String?,
) {
    val status: String get() = when {
        isApproved -> "APPROVED"
        approvedBy != null -> "REJECTED"
        else -> "PENDING"
    }
}

fun ComplianceEntity.toModel() = Compliance(
    id = id,
    doctorId = doctorId,
    isApproved = isApproved,
    approvedAt = approvedAt,
    approvedBy = approvedBy,
    failedApprovalReason = failedApprovalReason,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
