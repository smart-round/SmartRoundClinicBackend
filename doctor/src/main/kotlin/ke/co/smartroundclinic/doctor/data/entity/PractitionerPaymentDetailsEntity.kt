package ke.co.smartroundclinic.doctor.data.entity

import ke.co.smartroundclinic.doctor.domain.model.PaymentDetails
import org.bson.types.ObjectId
import kotlin.time.Clock

data class PractitionerPaymentDetailsEntity(
    val id: String = ObjectId().toString(),
    val doctorId: String,
    val bankName: String,
    val branchName: String,
    val bankCode: String,
    val branchCode: String,
    val accountNumber: String,
    val accountName: String,
    val paystackSubaccountCode: String? = null,
    val createdAt: String = Clock.System.now().toString(),
    val updatedAt: String? = null,
) {
    fun toModel() = PaymentDetails(
        id = id,
        doctorId = doctorId,
        bankName = bankName,
        branchName = branchName,
        bankCode = bankCode,
        branchCode = branchCode,
        accountNumber = accountNumber,
        accountName = accountName,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}
