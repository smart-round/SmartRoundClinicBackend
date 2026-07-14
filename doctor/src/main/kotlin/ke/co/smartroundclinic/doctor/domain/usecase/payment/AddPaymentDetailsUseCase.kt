package ke.co.smartroundclinic.doctor.domain.usecase.payment

import io.ktor.http.HttpStatusCode
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.DoctorWalletProvisioner
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.doctor.data.entity.PractitionerPaymentDetailsEntity
import ke.co.smartroundclinic.doctor.domain.repository.PaymentDetailsRepository

/**
 * Provisions the doctor's IntaSend wallet at the single, one-per-doctor, create-once point where
 * bank details are first saved. If a wallet provisioner is wired (:payments module loaded) and it
 * fails, the whole request fails rather than persisting payment details with no payout wallet.
 * Checks for an existing record up front so a doctor who already has payment details on file
 * (e.g. saved during sign-up) never triggers a wallet provisioning call that's just going to be
 * discarded when the insert below rejects the duplicate.
 */
class AddPaymentDetailsUseCase(
    private val repository: PaymentDetailsRepository,
    private val walletProvisioner: DoctorWalletProvisioner? = null,
) {
    suspend operator fun invoke(entity: PractitionerPaymentDetailsEntity): DefaultResponse<Nothing?> {
        val existing = (repository.getPaymentDetails(entity.doctorId) as? Resource.Success)?.data
        if (existing != null) {
            return DefaultResponse(
                httpStatusCode = HttpStatusCode.Conflict.value,
                status = false,
                message = "Payment details already exist for this doctor",
                data = null,
            )
        }

        val entityToSave = if (walletProvisioner != null) {
            val walletId = walletProvisioner.provisionWallet(entity.doctorId)
                ?: return DefaultResponse(
                    httpStatusCode = HttpStatusCode.BadGateway.value,
                    status = false,
                    message = "Failed to set up your payout wallet. Please try again.",
                    data = null,
                )
            entity.copy(walletId = walletId)
        } else {
            entity
        }

        return repository.addPaymentDetails(entityToSave).toDefaultResponse(
            successStatusCode = HttpStatusCode.Created.value,
            failedStatusCode = HttpStatusCode.Conflict.value,
            successMessage = "Payment details added successfully",
        )
    }
}