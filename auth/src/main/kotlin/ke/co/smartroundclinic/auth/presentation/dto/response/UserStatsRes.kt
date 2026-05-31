package ke.co.smartroundclinic.auth.presentation.dto.response

import kotlinx.serialization.Serializable

@Serializable
data class UserStatsRes(
    val roles: UserRoleStats,
    val accountStatus: UserAccountStatusStats,
    val verificationStatus: UserVerificationStatusStats,
    val doctors: DoctorStats,
    val growth: UserGrowthStats,
    val gender: UserGenderStats,
    val recentUsers: List<UserSummaryRes>,
)

@Serializable
data class UserRoleStats(
    val total: Long,
    val doctors: Long,
    val patients: Long,
    val admins: Long,
    val superAdmins: Long,
)

@Serializable
data class UserAccountStatusStats(
    val active: Long,
    val inactive: Long,
    val suspended: Long,
)

@Serializable
data class UserVerificationStatusStats(
    val verified: Long,
    val unverified: Long,
    val pendingApproval: Long,
    val rejected: Long,
)

@Serializable
data class DoctorStats(
    val total: Long,
    val verified: Long,
    val pendingApproval: Long,
    val approvalRate: Double,
)

@Serializable
data class UserGrowthStats(
    val today: Long,
    val thisWeek: Long,
    val thisMonth: Long,
    val thisYear: Long,
)

@Serializable
data class UserGenderStats(
    val male: Long,
    val female: Long,
    val nonBinary: Long,
    val other: Long,
)

@Serializable
data class UserSummaryRes(
    val id: String,
    val fullName: String,
    val email: String,
    val role: String,
    val accountStatus: String,
    val verificationStatus: String,
    val createdAt: String,
)
