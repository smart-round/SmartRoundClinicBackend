package ke.co.smartroundclinic.payments.domain.usecase.admin

import io.ktor.http.HttpStatusCode
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.payments.data.entity.EarningsLedgerEntity
import ke.co.smartroundclinic.payments.domain.repository.EarningsLedgerRepository
import ke.co.smartroundclinic.payments.presentation.dto.response.CommissionPeriodStats
import ke.co.smartroundclinic.payments.presentation.dto.response.EarningsChartRes
import ke.co.smartroundclinic.payments.presentation.dto.response.EarningsDataPoint
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * Revenue-vs-commission time series for admin charts (line or bar), bucketed by
 * [EarningsLedgerEntity.commissionCreditedAt] — the date commission was confirmed credited, i.e.
 * real confirmed activity, not payment-initiation time. `range` sets both the lookback window and
 * the bucket granularity: day -> hourly points, week/month -> daily points, year -> monthly points.
 * Every bucket in the window is present in the output, zero-filled, so charts render without gaps.
 */
class GetEarningsChartUseCase(private val repository: EarningsLedgerRepository) {

    private val hourFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:00").withZone(ZoneOffset.UTC)
    private val dayFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneOffset.UTC)
    private val monthFmt = DateTimeFormatter.ofPattern("yyyy-MM")

    suspend operator fun invoke(rangeParam: String?): DefaultResponse<EarningsChartRes?> {
        val range = ChartRange.parse(rangeParam)
        val now = Instant.now()
        val from = range.windowStart(now)

        val datedLogs: List<Pair<Instant, EarningsLedgerEntity>> = (repository.getAllForAdmin() as? Resource.Success)?.data
            ?.filter { it.commissionCreditedAt != null }
            ?.mapNotNull { entry ->
                val creditedAt = runCatching { Instant.parse(entry.commissionCreditedAt) }.getOrNull()
                    ?: return@mapNotNull null
                if (creditedAt.isBefore(from) || creditedAt.isAfter(now)) null else creditedAt to entry
            } ?: emptyList()

        val byBucket: Map<String, List<EarningsLedgerEntity>> = datedLogs
            .groupBy({ (instant, _) -> bucketKey(range, instant) }, { (_, entry) -> entry })

        val points = bucketSequence(range, from, now).map { key ->
            val entries = byBucket[key] ?: emptyList()
            EarningsDataPoint(
                label = key,
                commission = entries.sumOf { it.commissionAmount },
                revenue = entries.sumOf { it.grossAmount },
                disbursed = entries.filter { it.doctorCreditedAt != null }.sumOf { it.netAmount },
                transactionCount = entries.size,
            )
        }

        val logs = datedLogs.map { it.second }
        val totals = CommissionPeriodStats(
            totalCommission = logs.sumOf { it.commissionAmount },
            totalRevenue = logs.sumOf { it.grossAmount },
            totalDisbursed = logs.filter { it.doctorCreditedAt != null }.sumOf { it.netAmount },
            transactionCount = logs.size,
        )

        return DefaultResponse(
            httpStatusCode = HttpStatusCode.OK.value,
            status = true,
            message = "Earnings chart fetched successfully",
            data = EarningsChartRes(
                range = range.name.lowercase(),
                from = dayFmt.format(from),
                to = dayFmt.format(now),
                points = points,
                totals = totals,
            )
        )
    }

    private fun bucketKey(range: ChartRange, instant: Instant): String = when (range) {
        ChartRange.DAY -> hourFmt.format(instant)
        ChartRange.WEEK, ChartRange.MONTH -> dayFmt.format(instant)
        ChartRange.YEAR -> monthFmt.format(YearMonth.from(instant.atOffset(ZoneOffset.UTC)))
    }

    private fun bucketSequence(range: ChartRange, from: Instant, to: Instant): List<String> = when (range) {
        ChartRange.DAY -> generateSequence(from.truncatedTo(ChronoUnit.HOURS)) { it.plus(1, ChronoUnit.HOURS) }
            .takeWhile { !it.isAfter(to) }.map(hourFmt::format).toList()
        ChartRange.WEEK, ChartRange.MONTH -> generateSequence(from.truncatedTo(ChronoUnit.DAYS)) { it.plus(1, ChronoUnit.DAYS) }
            .takeWhile { !it.isAfter(to) }.map(dayFmt::format).toList()
        ChartRange.YEAR -> generateSequence(YearMonth.from(from.atOffset(ZoneOffset.UTC))) { it.plusMonths(1) }
            .takeWhile { !it.isAfter(YearMonth.from(to.atOffset(ZoneOffset.UTC))) }.map(monthFmt::format).toList()
    }
}
