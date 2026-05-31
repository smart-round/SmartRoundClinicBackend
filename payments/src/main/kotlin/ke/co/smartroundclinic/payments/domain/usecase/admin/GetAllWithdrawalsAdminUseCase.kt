package ke.co.smartroundclinic.payments.domain.usecase.admin

import io.ktor.http.HttpStatusCode
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.payments.data.entity.WithdrawalEntity
import ke.co.smartroundclinic.payments.domain.repository.WithdrawalRepository
import ke.co.smartroundclinic.payments.presentation.dto.response.WithdrawalItemRes
import ke.co.smartroundclinic.payments.presentation.dto.response.WithdrawalsPageRes
import kotlin.math.ceil

class GetAllWithdrawalsAdminUseCase(private val repository: WithdrawalRepository) {

    suspend fun getAll(page: Int, size: Int, status: String?): DefaultResponse<WithdrawalsPageRes?> =
        when (val result = repository.getAll(page, size, status)) {
            is Resource.Success -> {
                val (items, total) = result.data ?: (emptyList<WithdrawalEntity>() to 0L)
                DefaultResponse(
                    httpStatusCode = HttpStatusCode.OK.value,
                    status = true,
                    message = "Withdrawals fetched successfully",
                    data = WithdrawalsPageRes(
                        items = items.map { it.toRes() },
                        total = total,
                        page = page,
                        size = size,
                        pages = ceil(total.toDouble() / size).toLong(),
                    )
                )
            }
            is Resource.Error -> DefaultResponse(
                httpStatusCode = HttpStatusCode.InternalServerError.value,
                status = false,
                message = result.message ?: "Failed to fetch withdrawals",
                data = null,
            )
        }

    suspend fun getByDoctor(doctorId: String): DefaultResponse<WithdrawalsPageRes?> =
        when (val result = repository.getByDoctorId(doctorId)) {
            is Resource.Success -> {
                val items = result.data ?: emptyList()
                DefaultResponse(
                    httpStatusCode = HttpStatusCode.OK.value,
                    status = true,
                    message = "Doctor withdrawals fetched successfully",
                    data = WithdrawalsPageRes(
                        items = items.map { it.toRes() },
                        total = items.size.toLong(),
                        page = 1,
                        size = items.size,
                        pages = 1,
                    )
                )
            }
            is Resource.Error -> DefaultResponse(
                httpStatusCode = HttpStatusCode.InternalServerError.value,
                status = false,
                message = result.message ?: "Failed to fetch withdrawals",
                data = null,
            )
        }

    private fun WithdrawalEntity.toRes() = WithdrawalItemRes(
        id = id,
        doctorId = doctorId,
        amount = amount,
        currency = currency,
        trackingId = trackingId,
        status = status,
        provider = provider,
        platformCommission = platformCommission,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}
