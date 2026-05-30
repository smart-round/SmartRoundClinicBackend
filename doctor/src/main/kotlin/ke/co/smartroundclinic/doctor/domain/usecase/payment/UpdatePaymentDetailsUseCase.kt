package ke.co.smartroundclinic.doctor.domain.usecase.payment

import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.doctor.domain.repository.PaymentDetailsRepository
import ke.co.smartroundclinic.doctor.presentation.dto.response.PaymentDetailsRes
import ke.co.smartroundclinic.doctor.presentation.dto.response.toRes

class UpdatePaymentDetailsUseCase(
    private val repository: PaymentDetailsRepository,
) {
    suspend operator fun invoke(
        doctorId: String,
        accountName: String?,
        accountNumber: String?,
        bankCode: String?,
        branchCode: String?,
        bankName: String?,
        branchName: String?,
    ): DefaultResponse<PaymentDetailsRes?> =
        repository.updatePaymentDetails(accountName, accountNumber, bankCode, branchCode, bankName, branchName, doctorId)
            .toDefaultResponse { it?.toModel()?.toRes() }
}