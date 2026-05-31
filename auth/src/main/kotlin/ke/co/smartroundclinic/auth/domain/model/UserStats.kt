package ke.co.smartroundclinic.auth.domain.model

data class AuthUserStats(
    // Role breakdown
    val totalUsers: Long,
    val totalDoctors: Long,
    val totalPatients: Long,
    val totalAdmins: Long,
    val totalSuperAdmins: Long,
    // Account status
    val activeUsers: Long,
    val inactiveUsers: Long,
    val suspendedUsers: Long,
    // Verification status
    val verifiedUsers: Long,
    val unverifiedUsers: Long,
    val pendingApprovalUsers: Long,
    val rejectedUsers: Long,
    // Doctor-specific
    val pendingDoctorApprovals: Long,
    val verifiedDoctors: Long,
    // Growth
    val newToday: Long,
    val newThisWeek: Long,
    val newThisMonth: Long,
    val newThisYear: Long,
    // Gender
    val maleUsers: Long,
    val femaleUsers: Long,
    val nonBinaryUsers: Long,
    val otherGenderUsers: Long,
)
