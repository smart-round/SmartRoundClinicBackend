package ke.co.smartroundclinic.payments.domain.usecase

import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.payments.domain.model.toEntity
import ke.co.smartroundclinic.payments.domain.model.toModel
import ke.co.smartroundclinic.payments.domain.model.toRes
import ke.co.smartroundclinic.payments.domain.repository.PaymentRepository
import ke.co.smartroundclinic.payments.presentation.dto.request.InitiatePaymentReq
import ke.co.smartroundclinic.payments.presentation.dto.response.PaymentRes

class InitiatePaymentUseCase(private val repository: PaymentRepository) {
    suspend operator fun invoke(req: InitiatePaymentReq, patientId: String): DefaultResponse<PaymentRes?> =
        repository.save(req.toModel(patientId).toEntity())
            .toDefaultResponse { it?.toModel()?.toRes() }
}
