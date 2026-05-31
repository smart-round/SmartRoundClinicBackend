package ke.co.smartroundclinic.payments.domain.usecase.admin

import io.ktor.http.HttpStatusCode
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.payments.data.entity.PlatformCommissionLogEntity
import ke.co.smartroundclinic.payments.domain.repository.PlatformCommissionLogRepository
import ke.co.smartroundclinic.payments.presentation.dto.response.CommissionPeriodStats
import ke.co.smartroundclinic.payments.presentation.dto.response.CommissionTimeSummaryRes
import java.time.Instant
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

class GetCommissionTimeSummaryUseCase(private val repository: PlatformCommissionLogRepository) {

    suspend operator fun invoke(): DefaultResponse<CommissionTimeSummaryRes?> {
        val logs = (repository.getAllForAdmin() as? Resource.Success)?.data ?: emptyList()

        val now = Instant.now()
        val startOfToday = now.atOffset(ZoneOffset.UTC).truncatedTo(ChronoUnit.DAYS).toInstant()
        val startOfWeek = startOfToday.minus(7, ChronoUnit.DAYS)
        val startOfMonth = now.atOffset(ZoneOffset.UTC)
            .withDayOfMonth(1)
            .truncatedTo(ChronoUnit.DAYS)
            .toInstant()

        return DefaultResponse(
            httpStatusCode = HttpStatusCode.OK.value,
            status = true,
            message = "Commission time summary fetched successfully",
            data = CommissionTimeSummaryRes(
                total = logs.toStats(),
                monthly = logs.filter { it.createdAfter(startOfMonth) }.toStats(),
                weekly = logs.filter { it.createdAfter(startOfWeek) }.toStats(),
                daily = logs.filter { it.createdAfter(startOfToday) }.toStats(),
            )
        )
    }

    private fun PlatformCommissionLogEntity.createdAfter(boundary: Instant): Boolean =
        runCatching { Instant.parse(createdAt).isAfter(boundary) }.getOrDefault(false)

    private fun List<PlatformCommissionLogEntity>.toStats() = CommissionPeriodStats(
        totalCommission = sumOf { it.totalCommission },
        totalGross = sumOf { it.totalGross },
        totalDisbursed = sumOf { it.withdrawalAmount },
        transactionCount = size,
    )
}
