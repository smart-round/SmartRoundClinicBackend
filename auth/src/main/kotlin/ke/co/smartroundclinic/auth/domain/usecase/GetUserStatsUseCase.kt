package ke.co.smartroundclinic.auth.domain.usecase

import io.ktor.http.HttpStatusCode
import ke.co.smartroundclinic.auth.domain.repository.UserRepository
import ke.co.smartroundclinic.auth.presentation.dto.response.DoctorStats
import ke.co.smartroundclinic.auth.presentation.dto.response.UserAccountStatusStats
import ke.co.smartroundclinic.auth.presentation.dto.response.UserGenderStats
import ke.co.smartroundclinic.auth.presentation.dto.response.UserGrowthStats
import ke.co.smartroundclinic.auth.presentation.dto.response.UserRoleStats
import ke.co.smartroundclinic.auth.presentation.dto.response.UserStatsRes
import ke.co.smartroundclinic.auth.presentation.dto.response.UserSummaryRes
import ke.co.smartroundclinic.auth.presentation.dto.response.UserVerificationStatusStats
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.Resource

class GetUserStatsUseCase(private val repository: UserRepository) {

    suspend operator fun invoke(): DefaultResponse<UserStatsRes?> {
        val statsResult = repository.getAdminStats()
        if (statsResult !is Resource.Success) {
            return DefaultResponse(
                httpStatusCode = HttpStatusCode.InternalServerError.value,
                status = false,
                message = (statsResult as? Resource.Error)?.message ?: "Failed to fetch user stats",
                data = null,
            )
        }

        val s = statsResult.data ?: return DefaultResponse(
            httpStatusCode = HttpStatusCode.InternalServerError.value,
            status = false,
            message = "Failed to fetch user stats",
            data = null,
        )
        val recentUsers = (repository.getRecentUsers(10) as? Resource.Success)?.data ?: emptyList()

        val approvalRate = if (s.totalDoctors > 0)
            (s.verifiedDoctors.toDouble() / s.totalDoctors * 100).let {
                "%.1f".format(it).toDouble()
            }
        else 0.0

        return DefaultResponse(
            httpStatusCode = HttpStatusCode.OK.value,
            status = true,
            message = "User stats fetched successfully",
            data = UserStatsRes(
                roles = UserRoleStats(
                    total = s.totalUsers,
                    doctors = s.totalDoctors,
                    patients = s.totalPatients,
                    admins = s.totalAdmins,
                    superAdmins = s.totalSuperAdmins,
                ),
                accountStatus = UserAccountStatusStats(
                    active = s.activeUsers,
                    inactive = s.inactiveUsers,
                    suspended = s.suspendedUsers,
                ),
                verificationStatus = UserVerificationStatusStats(
                    verified = s.verifiedUsers,
                    unverified = s.unverifiedUsers,
                    pendingApproval = s.pendingApprovalUsers,
                    rejected = s.rejectedUsers,
                ),
                doctors = DoctorStats(
                    total = s.totalDoctors,
                    verified = s.verifiedDoctors,
                    pendingApproval = s.pendingDoctorApprovals,
                    approvalRate = approvalRate,
                ),
                growth = UserGrowthStats(
                    today = s.newToday,
                    thisWeek = s.newThisWeek,
                    thisMonth = s.newThisMonth,
                    thisYear = s.newThisYear,
                ),
                gender = UserGenderStats(
                    male = s.maleUsers,
                    female = s.femaleUsers,
                    nonBinary = s.nonBinaryUsers,
                    other = s.otherGenderUsers,
                ),
                recentUsers = recentUsers.map { user ->
                    UserSummaryRes(
                        id = user.id,
                        fullName = user.fullName,
                        email = user.email,
                        role = user.role.name,
                        accountStatus = user.accountStatus.name,
                        verificationStatus = user.verificationStatus.name,
                        createdAt = user.createdAt,
                    )
                },
            )
        )
    }
}
