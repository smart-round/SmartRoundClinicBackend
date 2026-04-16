package ke.co.smartroundclinic.doctor.domain.usecase.payment

import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.doctor.domain.repository.PaymentDetailsRepository
import ke.co.smartroundclinic.doctor.presentation.dto.response.PaymentDetailsPageResult
import ke.co.smartroundclinic.doctor.presentation.dto.response.toRes
import kotlin.math.ceil

class GetAllPaymentDetailsUseCase(private val repository: PaymentDetailsRepository) {
    suspend operator fun invoke(page: Int, size: Int): DefaultResponse<PaymentDetailsPageResult?> =
        repository.getAllPaymentDetails(page, size).toDefaultResponse { pair ->
            pair?.let { (items, total) ->
                PaymentDetailsPageResult(
                    items = items.map { it.toModel().toRes() },
                    total = total,
                    page = page,
                    size = size,
                    pages = ceil(total.toDouble() / size).toLong(),
                )
            }
        }
}
