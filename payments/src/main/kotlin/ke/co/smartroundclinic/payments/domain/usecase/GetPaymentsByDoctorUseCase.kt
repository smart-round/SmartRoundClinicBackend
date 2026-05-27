package ke.co.smartroundclinic.payments.domain.usecase

import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.payments.domain.model.toRes
import ke.co.smartroundclinic.payments.domain.repository.PaymentRepository
import ke.co.smartroundclinic.payments.presentation.dto.response.PaymentsPageRes
import kotlin.math.ceil

class GetPaymentsByDoctorUseCase(private val repository: PaymentRepository) {
    suspend operator fun invoke(doctorId: String, page: Int, size: Int): DefaultResponse<PaymentsPageRes?> =
        repository.getByDoctorId(doctorId, page, size).toDefaultResponse { pair ->
            pair?.let { (items, total) ->
                PaymentsPageRes(
                    items = items.map { it.toModel().toRes() },
                    total = total,
                    page = page,
                    size = size,
                    pages = ceil(total.toDouble() / size).toLong(),
                )
            }
        }
}
