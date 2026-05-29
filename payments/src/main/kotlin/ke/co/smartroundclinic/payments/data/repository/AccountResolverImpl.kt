package ke.co.smartroundclinic.payments.data.repository

import ke.co.smartroundclinic.common.AccountResolver
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.payments.data.remote.dto.request.ValidateAccountReq
import ke.co.smartroundclinic.payments.domain.repository.PaystackRepository
import org.slf4j.LoggerFactory

class AccountResolverImpl(
    private val paystackRepository: PaystackRepository,
) : AccountResolver {

    private val log = LoggerFactory.getLogger(AccountResolverImpl::class.java)

    override suspend fun resolveAccount(accountNumber: String, bankCode: String): Result<String> {
        return when (val result = paystackRepository.resolveAccount(accountNumber, bankCode)) {
            is Resource.Success -> {
                val accountName = result.data!!.data.accountName
                log.info("Account resolved: accountNumber=$accountNumber bankCode=$bankCode accountName=$accountName")
                Result.success(accountName)
            }
            is Resource.Error -> {
                log.warn("Account resolution failed: accountNumber=$accountNumber bankCode=$bankCode — ${result.message}")
                Result.failure(Exception(result.message ?: "Failed to resolve account number"))
            }
        }
    }

    override suspend fun validateAccount(
        accountNumber: String,
        bankCode: String,
        accountName: String,
        accountType: String,
        countryCode: String,
        documentType: String,
        documentNumber: String?,
    ): Result<Boolean> {
        val body = ValidateAccountReq(
            accountName = accountName,
            accountNumber = accountNumber,
            accountType = accountType,
            bankCode = bankCode,
            countryCode = countryCode,
            documentType = documentType,
            documentNumber = documentNumber,
        )
        return when (val result = paystackRepository.validateAccount(body)) {
            is Resource.Success -> {
                val responseData = result.data!!.data
                log.info("Account validation result: accountNumber=$accountNumber verified=${responseData.verified} message=${responseData.verificationMessage}")
                Result.success(responseData.verified)
            }
            is Resource.Error -> {
                log.warn("Account validation failed: accountNumber=$accountNumber — ${result.message}")
                Result.failure(Exception(result.message ?: "Failed to validate account"))
            }
        }
    }
}
