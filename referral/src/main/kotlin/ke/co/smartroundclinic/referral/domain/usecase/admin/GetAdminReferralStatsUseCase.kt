package ke.co.smartroundclinic.referral.domain.usecase.admin

import io.ktor.http.HttpStatusCode
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.referral.data.entity.ReferralStatus
import ke.co.smartroundclinic.referral.domain.repository.ReferralRepository
import ke.co.smartroundclinic.referral.presentation.dto.response.ReferralStatsRes

class GetAdminReferralStatsUseCase(private val repository: ReferralRepository) {
    suspend operator fun invoke(): DefaultResponse<ReferralStatsRes?> {
        val referrals = repository.getAll().data ?: emptyList()
        val byStatus = referrals.groupBy { it.status }
        val total = referrals.size
        val accepted = byStatus[ReferralStatus.ACCEPTED]?.size ?: 0

        return DefaultResponse(
            httpStatusCode = HttpStatusCode.OK.value,
            status = true,
            message = "Referral stats fetched successfully",
            data = ReferralStatsRes(
                total = total,
                pending = byStatus[ReferralStatus.PENDING]?.size ?: 0,
                accepted = accepted,
                declined = byStatus[ReferralStatus.DECLINED]?.size ?: 0,
                acceptanceRate = if (total == 0) 0.0 else accepted.toDouble() / total,
            ),
        )
    }
}
