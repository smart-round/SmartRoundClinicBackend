package ke.co.smartroundclinic.doctor.domain.usecase.payment

import io.ktor.http.HttpStatusCode
import ke.co.smartroundclinic.common.AccountResolver
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.doctor.data.entity.PractitionerPaymentDetailsEntity
import ke.co.smartroundclinic.doctor.domain.repository.PaymentDetailsRepository
import org.slf4j.LoggerFactory

class AddPaymentDetailsUseCase(
    private val repository: PaymentDetailsRepository,
    private val accountResolver: AccountResolver? = null,
) {
    private val log = LoggerFactory.getLogger(AddPaymentDetailsUseCase::class.java)

    suspend operator fun invoke(entity: PractitionerPaymentDetailsEntity): DefaultResponse<Nothing?> {
        val resolvedEntity = if (accountResolver != null) {
            accountResolver.resolveAccount(entity.accountNumber, entity.bankCode)
                .fold(
                    onSuccess = { verifiedName ->
                        log.info("Account resolved for doctorId=${entity.doctorId}: accountName=$verifiedName")
                        entity.copy(accountName = verifiedName)
                    },
                    onFailure = { err ->
                        log.warn("Account resolution failed for doctorId=${entity.doctorId}: ${err.message}")
                        return DefaultResponse(
                            httpStatusCode = HttpStatusCode.UnprocessableEntity.value,
                            status = false,
                            message = err.message ?: "Could not verify account number",
                            data = null,
                        )
                    }
                )
        } else {
            entity
        }

        return repository.addPaymentDetails(resolvedEntity).toDefaultResponse(
            successStatusCode = HttpStatusCode.Created.value,
            failedStatusCode = HttpStatusCode.Conflict.value,
            successMessage = "Payment details added successfully",
        )
    }
}