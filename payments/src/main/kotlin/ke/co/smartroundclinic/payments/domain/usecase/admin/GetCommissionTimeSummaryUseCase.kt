package ke.co.smartroundclinic.payments.domain.usecase.admin

import io.ktor.http.HttpStatusCode
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.payments.data.entity.EarningsLedgerEntity
import ke.co.smartroundclinic.payments.domain.repository.EarningsLedgerRepository
import ke.co.smartroundclinic.payments.presentation.dto.response.CommissionPeriodStats
import ke.co.smartroundclinic.payments.presentation.dto.response.CommissionTimeSummaryRes
import java.time.Instant
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

/** Buckets commission entries by [EarningsLedgerEntity.commissionCreditedAt] — the real date the
 *  commission was confirmed credited to the platform's IntaSend wallet, not when the row was created. */
class GetCommissionTimeSummaryUseCase(private val repository: EarningsLedgerRepository) {

    suspend operator fun invoke(): DefaultResponse<CommissionTimeSummaryRes?> {
        val logs = (repository.getAllForAdmin() as? Resource.Success)?.data
            ?.filter { it.commissionCreditedAt != null } ?: emptyList()

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
                monthly = logs.filter { it.creditedAfter(startOfMonth) }.toStats(),
                weekly = logs.filter { it.creditedAfter(startOfWeek) }.toStats(),
                daily = logs.filter { it.creditedAfter(startOfToday) }.toStats(),
            )
        )
    }

    private fun EarningsLedgerEntity.creditedAfter(boundary: Instant): Boolean =
        runCatching { Instant.parse(commissionCreditedAt).isAfter(boundary) }.getOrDefault(false)

    private fun List<EarningsLedgerEntity>.toStats() = CommissionPeriodStats(
        totalCommission = sumOf { it.commissionAmount },
        totalGross = sumOf { it.grossAmount },
        totalDisbursed = filter { it.doctorCreditedAt != null }.sumOf { it.netAmount },
        transactionCount = size,
    )
}
