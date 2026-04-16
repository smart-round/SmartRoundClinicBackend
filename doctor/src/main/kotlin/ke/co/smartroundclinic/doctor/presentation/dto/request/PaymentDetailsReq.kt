package ke.co.smartroundclinic.doctor.presentation.dto.request

import ke.co.smartroundclinic.doctor.domain.model.PaymentDetails
import kotlinx.serialization.Serializable
import org.bson.types.ObjectId
import kotlin.time.Clock

@Serializable
data class AddPaymentDetailsReq(
    val bankName: String,
    val branchName: String,
    val bankCode: String,
    val branchCode: String,
    val accountNumber: String,
    val accountName: String,
) {
    fun toModel(doctorId: String) = PaymentDetails(
        id = ObjectId().toString(),
        doctorId = doctorId,
        bankName = bankName,
        branchName = branchName,
        bankCode = bankCode,
        branchCode = branchCode,
        accountNumber = accountNumber,
        accountName = accountName,
        createdAt = Clock.System.now().toString(),
        updatedAt = null,
    )
}

@Serializable
data class UpdatePaymentDetailsReq(
    val bankName: String? = null,
    val branchName: String? = null,
    val bankCode: String? = null,
    val branchCode: String? = null,
    val accountNumber: String? = null,
    val accountName: String? = null,
)
