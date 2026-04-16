package ke.co.smartroundclinic.doctor.domain.usecase.payment

import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.doctor.domain.repository.PaymentDetailsRepository
import ke.co.smartroundclinic.doctor.presentation.dto.response.PaymentDetailsRes
import ke.co.smartroundclinic.doctor.presentation.dto.response.toRes

class GetPaymentDetailsByIdUseCase(private val repository: PaymentDetailsRepository) {
    suspend operator fun invoke(id: String): DefaultResponse<PaymentDetailsRes?> =
        repository.getPaymentDetailsById(id).toDefaultResponse { it?.toModel()?.toRes() }
}