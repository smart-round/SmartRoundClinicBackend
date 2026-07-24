package ke.co.smartroundclinic.payments.domain.usecase.admin

import io.ktor.http.HttpStatusCode
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.payments.domain.repository.EarningsLedgerRepository
import ke.co.smartroundclinic.payments.presentation.dto.response.RevenueBreakdownRes
import java.time.Instant

/**
 * Composition of confirmed revenue within `range`: how much of what was collected went to
 * commission vs. the doctor. Sourced entirely from the earnings ledger so the numbers are
 * internally consistent components of the same underlying rows (unlike the platform overview's
 * `collected`, which is a cumulative all-time figure, not scoped to a range).
 */
class GetRevenueBreakdownUseCase(private val repository: EarningsLedgerRepository) {

    suspend operator fun invoke(rangeParam: String?): DefaultResponse<RevenueBreakdownRes?> {
        val range = ChartRange.parse(rangeParam)
        val now = Instant.now()
        val from = range.windowStart(now)

        val logs = (repository.getAllForAdmin() as? Resource.Success)?.data
            ?.filter { it.commissionCreditedAt != null }
            ?.filter { entry ->
                val creditedAt = runCatching { Instant.parse(entry.commissionCreditedAt) }.getOrNull()
                creditedAt != null && !creditedAt.isBefore(from) && !creditedAt.isAfter(now)
            } ?: emptyList()

        val collected = logs.sumOf { it.grossAmount }
        val commission = logs.sumOf { it.commissionAmount }
        val netToDoctor = logs.filter { it.doctorCreditedAt != null }.sumOf { it.netAmount }
        val doctorPayoutPending = logs.filter { it.doctorCreditedAt == null }.sumOf { it.netAmount }

        return DefaultResponse(
            httpStatusCode = HttpStatusCode.OK.value,
            status = true,
            message = "Revenue breakdown fetched successfully",
            data = RevenueBreakdownRes(
                range = range.name.lowercase(),
                collected = collected,
                commission = commission,
                netToDoctor = netToDoctor,
                doctorPayoutPending = doctorPayoutPending,
                transactionCount = logs.size,
            )
        )
    }
}
