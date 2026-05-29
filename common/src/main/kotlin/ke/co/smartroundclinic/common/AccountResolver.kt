package ke.co.smartroundclinic.common

interface AccountResolver {
    /**
     * Resolves an account number and bank code against the Paystack /bank/resolve endpoint.
     * @return [Result.success] containing the verified account name, or [Result.failure] with a descriptive error.
     */
    suspend fun resolveAccount(accountNumber: String, bankCode: String): Result<String>

    /**
     * Validates an account's identity documents via the Paystack /bank/validate endpoint.
     * @return [Result.success] containing true if verified, or [Result.failure] with a descriptive error.
     */
    suspend fun validateAccount(
        accountNumber: String,
        bankCode: String,
        accountName: String,
        accountType: String,
        countryCode: String,
        documentType: String,
        documentNumber: String?,
    ): Result<Boolean>
}
