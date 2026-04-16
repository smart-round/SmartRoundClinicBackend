package ke.co.smartroundclinic.doctor.domain.usecase.payment

import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.doctor.domain.repository.PaymentDetailsRepository

class DeletePaymentDetailsUseCase(private val repository: PaymentDetailsRepository) {
    suspend operator fun invoke(doctorId: String): DefaultResponse<Nothing?> =
        repository.deletePaymentDetails(doctorId).toDefaultResponse()
}