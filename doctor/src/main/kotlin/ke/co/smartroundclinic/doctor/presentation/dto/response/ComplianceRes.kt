package ke.co.smartroundclinic.doctor.presentation.dto.response

import ke.co.smartroundclinic.doctor.domain.model.Compliance

data class DoctorProfileInfo(
    val fullName: String,
    val email: String,
    val phoneNumber: String?,
    val profilePicture: String?,
    val title: String?,
    val facilityName: String?,
    val kmpdcRegNumber: String?,
)

data class ComplianceRes(
    val id: String,
    val doctorId: String,
    val status: String,
    val isApproved: Boolean,
    val isMonetized: Boolean,
    val approvedAt: String?,
    val approvedBy: String?,
    val failedApprovalReason: String?,
    val createdAt: String,
    val updatedAt: String?,
    val doctorProfile: DoctorProfileInfo?,
    val specializations: List<SpecializationWithNamesRes>,
)

data class CompliancePageResult(
    val items: List<ComplianceRes>,
    val total: Long,
    val page: Int,
    val size: Int,
    val pages: Long,
)

fun Compliance.toRes(
    doctorProfile: DoctorProfileInfo? = null,
    specializations: List<SpecializationWithNamesRes> = emptyList(),
) = ComplianceRes(
    id = id,
    doctorId = doctorId,
    status = status,
    isApproved = isApproved,
    isMonetized = isMonetized,
    approvedAt = approvedAt,
    approvedBy = approvedBy,
    failedApprovalReason = failedApprovalReason,
    createdAt = createdAt,
    updatedAt = updatedAt,
    doctorProfile = doctorProfile,
    specializations = specializations,
)