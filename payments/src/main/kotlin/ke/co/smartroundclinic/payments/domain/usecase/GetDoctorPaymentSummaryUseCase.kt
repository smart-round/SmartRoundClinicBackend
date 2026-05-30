package ke.co.smartroundclinic.payments.domain.usecase

import io.ktor.http.HttpStatusCode
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.payments.data.entity.PaymentEntity
import ke.co.smartroundclinic.payments.domain.repository.PaymentRepository
import ke.co.smartroundclinic.payments.presentation.dto.response.DoctorPaymentSummaryRes

class GetDoctorPaymentSummaryUseCase(
    private val repository: PaymentRepository,
) {
    suspend operator fun invoke(doctorId: String): DefaultResponse<DoctorPaymentSummaryRes?> {
        return when (val result = repository.getAllByDoctorId(doctorId)) {
            is Resource.Success -> {
                val payments = result.data ?: emptyList()
                val completed = payments.filter { it.status == PaymentEntity.PaymentStatus.COMPLETED }
                val pending = payments.filter { it.status == PaymentEntity.PaymentStatus.PENDING }
                DefaultResponse(
                    httpStatusCode = HttpStatusCode.OK.value,
                    status = true,
                    message = "Payment summary fetched successfully",
                    data = DoctorPaymentSummaryRes(
                        totalEarned = completed.sumOf { it.amount },
                        totalPending = pending.sumOf { it.amount },
                        completedCount = completed.size,
                        pendingCount = pending.size,
                        totalTransactions = payments.size,
                    )
                )
            }
            is Resource.Error -> DefaultResponse(
                httpStatusCode = HttpStatusCode.InternalServerError.value,
                status = false,
                message = result.message ?: "Failed to fetch payment summary",
                data = null,
            )
        }
    }
}
