package ke.co.smartroundclinic.payments.domain.usecase.admin

import io.ktor.http.HttpStatusCode
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.payments.data.entity.EarningsLedgerEntity
import ke.co.smartroundclinic.payments.domain.repository.EarningsLedgerRepository
import ke.co.smartroundclinic.payments.presentation.dto.response.CommissionLogItemRes
import ke.co.smartroundclinic.payments.presentation.dto.response.CommissionLogsPageRes
import kotlin.math.ceil

class GetCommissionLogsAdminUseCase(private val repository: EarningsLedgerRepository) {

    suspend fun getAll(page: Int, size: Int): DefaultResponse<CommissionLogsPageRes?> =
        when (val result = repository.getAll(page, size)) {
            is Resource.Success -> {
                val (items, total) = result.data ?: (emptyList<EarningsLedgerEntity>() to 0L)
                DefaultResponse(
                    httpStatusCode = HttpStatusCode.OK.value,
                    status = true,
                    message = "Commission logs fetched successfully",
                    data = CommissionLogsPageRes(
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
                message = result.message ?: "Failed to fetch commission logs",
                data = null,
            )
        }

    suspend fun getByDoctor(doctorId: String): DefaultResponse<CommissionLogsPageRes?> =
        when (val result = repository.getByDoctorId(doctorId)) {
            is Resource.Success -> {
                val items = result.data ?: emptyList()
                DefaultResponse(
                    httpStatusCode = HttpStatusCode.OK.value,
                    status = true,
                    message = "Doctor commission logs fetched successfully",
                    data = CommissionLogsPageRes(
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
                message = result.message ?: "Failed to fetch commission logs",
                data = null,
            )
        }

    private fun EarningsLedgerEntity.toRes() = CommissionLogItemRes(
        id = id,
        paymentId = paymentId,
        appointmentId = appointmentId,
        doctorId = doctorId,
        grossAmount = grossAmount,
        commissionRate = commissionRate,
        commissionAmount = commissionAmount,
        netAmount = netAmount,
        doctorCreditedAt = doctorCreditedAt,
        commissionCreditedAt = commissionCreditedAt,
        createdAt = createdAt,
    )
}
