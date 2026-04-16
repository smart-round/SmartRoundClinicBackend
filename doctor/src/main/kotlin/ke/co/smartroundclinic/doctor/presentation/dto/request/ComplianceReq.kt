package ke.co.smartroundclinic.doctor.presentation.dto.request

import kotlinx.serialization.Serializable

@Serializable
data class RejectComplianceReq(
    val reason: String,
)