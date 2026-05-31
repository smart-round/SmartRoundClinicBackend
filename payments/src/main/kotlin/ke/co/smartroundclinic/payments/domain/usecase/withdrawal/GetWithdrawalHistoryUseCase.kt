package ke.co.smartroundclinic.payments.domain.usecase.withdrawal

import io.ktor.http.HttpStatusCode
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.payments.domain.repository.WithdrawalRepository
import ke.co.smartroundclinic.payments.presentation.dto.response.WithdrawalItemRes
import ke.co.smartroundclinic.payments.presentation.dto.response.WithdrawalsPageRes
import kotlin.math.ceil

class GetWithdrawalHistoryUseCase(
    private val withdrawalRepository: WithdrawalRepository,
) {
    suspend operator fun invoke(doctorId: String, page: Int, size: Int): DefaultResponse<WithdrawalsPageRes?> {
        val safePage = maxOf(1, page)
        val safeSize = minOf(maxOf(1, size), 100)

        return when (val result = withdrawalRepository.getByDoctorIdPaginated(doctorId, safePage, safeSize)) {
            is Resource.Success -> {
                val (items, total) = result.data!!
                DefaultResponse(
                    httpStatusCode = HttpStatusCode.OK.value,
                    status = true,
                    message = "Withdrawal history fetched successfully",
                    data = WithdrawalsPageRes(
                        items = items.map { w ->
                            WithdrawalItemRes(
                                id = w.id,
                                doctorId = w.doctorId,
                                amount = w.amount,
                                currency = w.currency,
                                trackingId = w.trackingId,
                                status = w.status,
                                provider = w.provider,
                                platformCommission = w.platformCommission,
                                createdAt = w.createdAt,
                                updatedAt = w.updatedAt,
                            )
                        },
                        total = total,
                        page = safePage,
                        size = safeSize,
                        pages = ceil(total.toDouble() / safeSize).toLong(),
                    ),
                )
            }
            is Resource.Error -> DefaultResponse(
                httpStatusCode = HttpStatusCode.InternalServerError.value,
                status = false,
                message = result.message ?: "Failed to fetch withdrawal history",
                data = null,
            )
        }
    }
}
