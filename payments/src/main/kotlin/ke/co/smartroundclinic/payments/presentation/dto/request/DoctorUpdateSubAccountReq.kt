package ke.co.smartroundclinic.payments.presentation.dto.request

import ke.co.smartroundclinic.payments.data.remote.dto.request.UpdateSubAccountReq
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DoctorUpdateSubAccountReq(
    @SerialName("business_name")
    val businessName: String? = null,
    @SerialName("description")
    val description: String? = null,
    @SerialName("primary_contact_email")
    val primaryContactEmail: String? = null,
    @SerialName("primary_contact_name")
    val primaryContactName: String? = null,
    @SerialName("primary_contact_phone")
    val primaryContactPhone: String? = null,
    @SerialName("settlement_schedule")
    val settlementSchedule: String? = null,
    @SerialName("metadata")
    val metadata: Map<String, String>? = null,
)

fun DoctorUpdateSubAccountReq.toPaystackReq() = UpdateSubAccountReq(
    businessName = businessName,
    description = description,
    primaryContactEmail = primaryContactEmail,
    primaryContactName = primaryContactName,
    primaryContactPhone = primaryContactPhone,
    settlementSchedule = settlementSchedule,
    metadata = metadata,
)
