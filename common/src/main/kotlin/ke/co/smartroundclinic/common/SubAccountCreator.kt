package ke.co.smartroundclinic.common

interface SubAccountCreator {
    suspend fun createForDoctor(
        businessName: String,
        settlementBank: String,
        accountNumber: String,
    ): Result<String>

    suspend fun syncForDoctor(
        doctorId: String,
        accountNumber: String?,
        settlementBank: String?,
        businessName: String?,
    ): Result<Unit>
}
