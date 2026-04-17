package ke.co.smartroundclinic.doctor.domain.usecase.compliance

import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.doctor.domain.repository.PaymentDetailsRepository
import ke.co.smartroundclinic.doctor.domain.repository.PractitionerLicenceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ComplianceCheckUseCase(
    private val licence: PractitionerLicenceRepository,
    private val paymentDetails: PaymentDetailsRepository,
) {
    suspend operator fun invoke(doctorId: String): Resource<Nothing> = withContext(Dispatchers.IO){
        val getLicenceById = licence.getByIdDoctorId(doctorId)
        if (getLicenceById is Resource.Error) return@withContext Resource.Error(message = getLicenceById.message ?: "Failed to get licence")
        if (getLicenceById.data == null) return@withContext Resource.Error(message = "No licence found")

        val getPaymentDetails = paymentDetails.getPaymentDetails(doctorId)
        if (getPaymentDetails is Resource.Error) return@withContext Resource.Error(message = getPaymentDetails.message ?: "Failed to get payment details")
        if (getPaymentDetails.data == null) return@withContext Resource.Error(message = "No payment details found")

        Resource.Success(null)

    }
}