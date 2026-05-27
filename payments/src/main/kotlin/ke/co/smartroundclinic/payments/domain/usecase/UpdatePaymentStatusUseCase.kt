package ke.co.smartroundclinic.payments.domain.usecase

import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.payments.data.entity.PaymentEntity
import ke.co.smartroundclinic.payments.domain.model.toRes
import ke.co.smartroundclinic.payments.domain.repository.PaymentRepository
import ke.co.smartroundclinic.payments.presentation.dto.response.PaymentRes

class UpdatePaymentStatusUseCase(private val repository: PaymentRepository) {
    suspend operator fun invoke(id: String, status: String, transactionRef: String?): DefaultResponse<PaymentRes?> {
        val validStatuses = PaymentEntity.PaymentStatus.entries.map { it.name }
        if (status.uppercase() !in validStatuses) {
            return ke.co.smartroundclinic.common.Resource.Error<PaymentEntity?>(
                "Invalid status. Must be one of: ${validStatuses.joinToString()}"
            ).toDefaultResponse { null }
        }
        return repository.updateStatus(id, status, transactionRef)
            .toDefaultResponse { it?.toModel()?.toRes() }
    }
}
