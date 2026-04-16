package ke.co.smartroundclinic.doctor.presentation.dto.response

import ke.co.smartroundclinic.doctor.domain.model.PaymentDetails
import kotlinx.serialization.Serializable

@Serializable
data class PaymentDetailsRes(
    val id: String,
    val doctorId: String,
    val bankName: String,
    val branchName: String,
    val bankCode: String,
    val branchCode: String,
    val accountNumber: String,
    val accountName: String,
    val createdAt: String,
    val updatedAt: String?,
)

@Serializable
data class PaymentDetailsPageResult(
    val items: List<PaymentDetailsRes>,
    val total: Long,
    val page: Int,
    val size: Int,
    val pages: Long,
)

fun PaymentDetails.toRes() = PaymentDetailsRes(
    id = id,
    doctorId = doctorId,
    bankName = bankName,
    bankCode = bankCode,
    branchName = branchName,
    branchCode = branchCode,
    accountNumber = accountNumber,
    accountName = accountName,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
