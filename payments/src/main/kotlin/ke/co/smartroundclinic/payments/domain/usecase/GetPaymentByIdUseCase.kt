package ke.co.smartroundclinic.payments.domain.usecase

import io.ktor.http.HttpStatusCode
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.payments.domain.model.toRes
import ke.co.smartroundclinic.payments.domain.repository.PaymentRepository
import ke.co.smartroundclinic.payments.presentation.dto.response.PaymentRes

class GetPaymentByIdUseCase(private val repository: PaymentRepository) {
    suspend operator fun invoke(id: String): DefaultResponse<PaymentRes?> =
        repository.getById(id)
            .toDefaultResponse(failedStatusCode = HttpStatusCode.InternalServerError.value) { entity ->
                entity?.toModel()?.toRes()
            }.let { response ->
                if (response.status && response.data == null)
                    response.copy(httpStatusCode = HttpStatusCode.NotFound.value, status = false, message = "Payment not found")
                else response
            }
}
