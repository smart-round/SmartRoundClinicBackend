package ke.co.smartroundclinic.payments.presentation.dto.request

import kotlinx.serialization.Serializable

@Serializable
data class WithdrawInitiateReq(
    val idNumber: String,
    val amount: Double,
)
