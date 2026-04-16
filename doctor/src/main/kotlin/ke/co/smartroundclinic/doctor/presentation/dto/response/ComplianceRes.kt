package ke.co.smartroundclinic.doctor.presentation.dto.response

import ke.co.smartroundclinic.doctor.domain.model.Compliance
import kotlinx.serialization.Serializable

@Serializable
data class ComplianceRes(
    val id: String,
    val doctorId: String,
    val status: String,
    val isApproved: Boolean,
    val approvedAt: Long?,
    val approvedBy: String?,
    val failedApprovalReason: String?,
    val createdAt: String,
    val updatedAt: String?,
)

@Serializable
data class CompliancePageResult(
    val items: List<ComplianceRes>,
    val total: Long,
    val page: Int,
    val size: Int,
    val pages: Long,
)

fun Compliance.toRes() = ComplianceRes(
    id = id,
    doctorId = doctorId,
    status = status,
    isApproved = isApproved,
    approvedAt = approvedAt,
    approvedBy = approvedBy,
    failedApprovalReason = failedApprovalReason,
    createdAt = createdAt,
    updatedAt = updatedAt,
)