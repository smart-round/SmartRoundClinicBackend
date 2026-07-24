package ke.co.smartroundclinic.payments.domain.usecase.admin

import java.time.Instant
import java.time.temporal.ChronoUnit

/** Shorthand lookback windows for admin revenue/commission charts. */
enum class ChartRange {
    DAY, WEEK, MONTH, YEAR;

    companion object {
        fun parse(value: String?): ChartRange =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: WEEK
    }
}

fun ChartRange.windowStart(now: Instant): Instant = when (this) {
    ChartRange.DAY -> now.minus(24, ChronoUnit.HOURS)
    ChartRange.WEEK -> now.minus(7, ChronoUnit.DAYS)
    ChartRange.MONTH -> now.minus(30, ChronoUnit.DAYS)
    ChartRange.YEAR -> now.minus(365, ChronoUnit.DAYS)
}
