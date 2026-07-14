package ke.co.smartroundclinic.doctor.domain.model

import ke.co.smartroundclinic.doctor.data.entity.PractitionerPaymentDetailsEntity
import kotlin.time.Clock

data class PaymentDetails(
    val id: String,
    val doctorId: String,
    val bankName: String,
    val branchName: String,
    val bankCode: String,
    val branchCode: String,
    val accountNumber: String,
    val accountName: String,
    val walletId: String? = null,
    val createdAt: String,
    val updatedAt: String?,
){
    fun toEntity() = PractitionerPaymentDetailsEntity(
        id = id,
        doctorId = doctorId,
        bankName = bankName,
        branchName = branchName,
        bankCode = bankCode,
        branchCode = branchCode,
        accountNumber = accountNumber,
        accountName = accountName,
        walletId = walletId,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}
