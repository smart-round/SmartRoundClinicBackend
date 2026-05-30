package ke.co.smartroundclinic.doctor.domain.usecase.payment

import io.ktor.http.HttpStatusCode
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.doctor.data.entity.PractitionerPaymentDetailsEntity
import ke.co.smartroundclinic.doctor.domain.repository.PaymentDetailsRepository

class AddPaymentDetailsUseCase(
    private val repository: PaymentDetailsRepository,
) {
    suspend operator fun invoke(entity: PractitionerPaymentDetailsEntity): DefaultResponse<Nothing?> =
        repository.addPaymentDetails(entity).toDefaultResponse(
            successStatusCode = HttpStatusCode.Created.value,
            failedStatusCode = HttpStatusCode.Conflict.value,
            successMessage = "Payment details added successfully",
        )
}