package ke.co.smartroundclinic.doctor.domain.usecase.payment

import io.ktor.http.HttpStatusCode
import ke.co.smartroundclinic.common.AccountResolver
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.common.SubAccountCreator
import ke.co.smartroundclinic.doctor.domain.repository.PaymentDetailsRepository
import ke.co.smartroundclinic.doctor.presentation.dto.response.PaymentDetailsRes
import ke.co.smartroundclinic.doctor.presentation.dto.response.toRes
import org.slf4j.LoggerFactory

class UpdatePaymentDetailsUseCase(
    private val repository: PaymentDetailsRepository,
    private val subAccountCreator: SubAccountCreator? = null,
    private val accountResolver: AccountResolver? = null,
) {
    private val log = LoggerFactory.getLogger(UpdatePaymentDetailsUseCase::class.java)

    suspend operator fun invoke(
        doctorId: String,
        accountName: String?,
        accountNumber: String?,
        bankCode: String?,
        branchCode: String?,
        bankName: String?,
        branchName: String?,
    ): DefaultResponse<PaymentDetailsRes?> {
        // If bank-related fields are changing, resolve the account to verify and get the canonical name
        val resolvedAccountName: String? = if (accountResolver != null && (accountNumber != null || bankCode != null)) {
            val existing = (repository.getPaymentDetails(doctorId) as? Resource.Success)?.data
            val resolveNumber = accountNumber ?: existing?.accountNumber ?: ""
            val resolveCode = bankCode ?: existing?.bankCode ?: ""
            accountResolver.resolveAccount(resolveNumber, resolveCode)
                .fold(
                    onSuccess = { verifiedName ->
                        log.info("Account resolved on update for doctorId=$doctorId: accountName=$verifiedName")
                        verifiedName
                    },
                    onFailure = { err ->
                        log.warn("Account resolution failed on update for doctorId=$doctorId: ${err.message}")
                        return DefaultResponse(
                            httpStatusCode = HttpStatusCode.UnprocessableEntity.value,
                            status = false,
                            message = err.message ?: "Could not verify account number",
                            data = null,
                        )
                    }
                )
        } else {
            accountName
        }

        val result = repository.updatePaymentDetails(resolvedAccountName, accountNumber, bankCode, branchCode, bankName, branchName, doctorId)
        runCatching {
            subAccountCreator?.syncForDoctor(
                doctorId = doctorId,
                accountNumber = accountNumber,
                settlementBank = bankCode,
                businessName = resolvedAccountName,
            )
        }.onFailure { log.error("Paystack subaccount sync failed for doctorId=$doctorId — ${it.message}", it) }
        return result.toDefaultResponse { it?.toModel()?.toRes() }
    }
}