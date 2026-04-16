package ke.co.smartroundclinic.doctor.domain.usecase.payment

import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.doctor.domain.repository.PaymentDetailsRepository
import ke.co.smartroundclinic.doctor.presentation.dto.response.PaymentDetailsRes
import ke.co.smartroundclinic.doctor.presentation.dto.response.toRes

class GetPaymentDetailsUseCase(private val repository: PaymentDetailsRepository) {
    suspend operator fun invoke(doctorId: String): DefaultResponse<PaymentDetailsRes?> =
        repository.getPaymentDetails(doctorId).toDefaultResponse { it?.toModel()?.toRes() }
}