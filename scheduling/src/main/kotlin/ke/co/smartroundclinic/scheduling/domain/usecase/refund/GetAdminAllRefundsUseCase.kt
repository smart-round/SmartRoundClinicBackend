package ke.co.smartroundclinic.scheduling.domain.usecase.refund

import io.ktor.http.HttpStatusCode
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.scheduling.data.lookup.AppointmentAdminLookup
import ke.co.smartroundclinic.scheduling.data.lookup.RefundLookup
import ke.co.smartroundclinic.scheduling.presentation.dto.response.RefundsPageRes
import ke.co.smartroundclinic.scheduling.presentation.dto.response.toRes
import kotlin.math.ceil

/** Admin queue — refund records created on appointment cancellation, newest first. */
class GetAdminAllRefundsUseCase(
    private val refundLookup: RefundLookup,
    private val nameLookup: AppointmentAdminLookup,
) {
    suspend operator fun invoke(status: String?, page: Int, size: Int): DefaultResponse<RefundsPageRes?> {
        val safePage = maxOf(1, page)
        val safeSize = minOf(maxOf(1, size), 100)
        val (items, total) = refundLookup.getAll(status, safePage, safeSize)

        val userIds = items.flatMap { listOf(it.doctorId, it.patientId) }.distinct()
        val names = if (userIds.isNotEmpty()) nameLookup.getUserNames(userIds) else emptyMap()
        val paymentIds = items.map { it.paymentId }.distinct()
        val payments = if (paymentIds.isNotEmpty()) nameLookup.getPaymentsByIds(paymentIds) else emptyMap()

        return DefaultResponse(
            httpStatusCode = HttpStatusCode.OK.value,
            status = true,
            message = "Refunds fetched successfully",
            data = RefundsPageRes(
                items = items.map { it.toRes(names, payments[it.paymentId]) },
                total = total,
                page = safePage,
                size = safeSize,
                pages = if (total == 0L) 0L else ceil(total.toDouble() / safeSize).toLong(),
            ),
        )
    }
}
