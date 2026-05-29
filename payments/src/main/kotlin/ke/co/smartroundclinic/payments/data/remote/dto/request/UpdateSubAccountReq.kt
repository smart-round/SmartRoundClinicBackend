package ke.co.smartroundclinic.payments.data.remote.dto.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class UpdateSubAccountReq(
    @SerialName("business_name")
    val businessName: String? = null,
    @SerialName("settlement_bank")
    val settlementBank: String? = null,
    @SerialName("description")
    val description: String? = null,
    @SerialName("account_number")
    val accountNumber: String? = null,
    @SerialName("active")
    val active: Boolean? = null,
    @SerialName("percentage_charge")
    val percentageCharge: Int? = null,
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