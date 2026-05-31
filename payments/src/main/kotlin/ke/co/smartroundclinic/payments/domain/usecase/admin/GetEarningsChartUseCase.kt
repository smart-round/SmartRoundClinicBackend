package ke.co.smartroundclinic.payments.domain.usecase.admin

import io.ktor.http.HttpStatusCode
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.payments.data.entity.PlatformCommissionLogEntity
import ke.co.smartroundclinic.payments.domain.repository.PlatformCommissionLogRepository
import ke.co.smartroundclinic.payments.presentation.dto.response.CommissionPeriodStats
import ke.co.smartroundclinic.payments.presentation.dto.response.EarningsChartRes
import ke.co.smartroundclinic.payments.presentation.dto.response.EarningsDataPoint
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class GetEarningsChartUseCase(private val repository: PlatformCommissionLogRepository) {

    // date format accepted/returned: yyyy-MM-dd
    private val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    suspend operator fun invoke(fromParam: String?, toParam: String?): DefaultResponse<EarningsChartRes?> {
        val toDate = toParam?.let { runCatching { LocalDate.parse(it, fmt) }.getOrNull() }
            ?: LocalDate.now(ZoneOffset.UTC)
        val fromDate = fromParam?.let { runCatching { LocalDate.parse(it, fmt) }.getOrNull() }
            ?: toDate.minusMonths(1)

        if (fromDate.isAfter(toDate)) {
            return DefaultResponse(
                httpStatusCode = HttpStatusCode.BadRequest.value,
                status = false,
                message = "from must be before or equal to to",
                data = null,
            )
        }

        val logs = (repository.getAllForAdmin() as? Resource.Success)?.data ?: emptyList()

        // Build a map keyed by date string for fast lookup
        val byDate: Map<String, List<PlatformCommissionLogEntity>> = logs
            .mapNotNull { log ->
                val date = runCatching {
                    Instant.parse(log.createdAt)
                        .atOffset(ZoneOffset.UTC)
                        .toLocalDate()
                }.getOrNull() ?: return@mapNotNull null
                date to log
            }
            .filter { (date, _) -> !date.isBefore(fromDate) && !date.isAfter(toDate) }
            .groupBy({ (date, _) -> date.format(fmt) }, { (_, log) -> log })

        // Generate one data point per calendar day in the range
        val points = generateSequence(fromDate) { it.plusDays(1) }
            .takeWhile { !it.isAfter(toDate) }
            .map { day ->
                val key = day.format(fmt)
                val entries = byDate[key] ?: emptyList()
                EarningsDataPoint(
                    date = key,
                    commission = entries.sumOf { it.totalCommission },
                    gross = entries.sumOf { it.totalGross },
                    disbursed = entries.sumOf { it.withdrawalAmount },
                    transactionCount = entries.size,
                )
            }
            .toList()

        val inRange = byDate.values.flatten()
        val totals = CommissionPeriodStats(
            totalCommission = inRange.sumOf { it.totalCommission },
            totalGross = inRange.sumOf { it.totalGross },
            totalDisbursed = inRange.sumOf { it.withdrawalAmount },
            transactionCount = inRange.size,
        )

        return DefaultResponse(
            httpStatusCode = HttpStatusCode.OK.value,
            status = true,
            message = "Earnings chart fetched successfully",
            data = EarningsChartRes(
                from = fromDate.format(fmt),
                to = toDate.format(fmt),
                points = points,
                totals = totals,
            )
        )
    }
}
