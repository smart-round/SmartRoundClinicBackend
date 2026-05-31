package ke.co.smartroundclinic.payments.domain.usecase.withdrawal

import io.ktor.http.HttpStatusCode
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.payments.domain.repository.WithdrawalRepository
import ke.co.smartroundclinic.payments.presentation.dto.response.WithdrawalItemRes

class GetWithdrawalByIdUseCase(
    private val withdrawalRepository: WithdrawalRepository,
) {
    suspend operator fun invoke(id: String, doctorId: String): DefaultResponse<WithdrawalItemRes?> {
        val result = withdrawalRepository.getById(id)

        if (result is Resource.Error) {
            return DefaultResponse(
                httpStatusCode = HttpStatusCode.InternalServerError.value,
                status = false,
                message = result.message ?: "Failed to fetch withdrawal",
                data = null,
            )
        }

        val entity = (result as Resource.Success).data
            ?: return DefaultResponse(
                httpStatusCode = HttpStatusCode.NotFound.value,
                status = false,
                message = "Withdrawal not found",
                data = null,
            )

        if (entity.doctorId != doctorId) {
            return DefaultResponse(
                httpStatusCode = HttpStatusCode.Forbidden.value,
                status = false,
                message = "You do not have access to this withdrawal",
                data = null,
            )
        }

        return DefaultResponse(
            httpStatusCode = HttpStatusCode.OK.value,
            status = true,
            message = "Withdrawal fetched successfully",
            data = WithdrawalItemRes(
                id = entity.id,
                doctorId = entity.doctorId,
                amount = entity.amount,
                currency = entity.currency,
                trackingId = entity.trackingId,
                status = entity.status,
                provider = entity.provider,
                platformCommission = entity.platformCommission,
                createdAt = entity.createdAt,
                updatedAt = entity.updatedAt,
            ),
        )
    }
}
