package ke.co.smartroundclinic.payments.data.remote.dto.response


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class UpdateSubAccountRes(
    @SerialName("data")
    val `data`: Data,
    @SerialName("message")
    val message: String,
    @SerialName("status")
    val status: Boolean
)

@Serializable
data class Data(
    @SerialName("account_name")
    val accountName: String? = null,
    @SerialName("account_number")
    val accountNumber: String,
    @SerialName("active")
    val active: Boolean,
    @SerialName("bank")
    val bank: Int,
    @SerialName("business_name")
    val businessName: String,
    @SerialName("createdAt")
    val createdAt: String,
    @SerialName("currency")
    val currency: String,
    @SerialName("description")
    val description: String? = null,
    @SerialName("domain")
    val domain: String,
    @SerialName("id")
    val id: Int,
    @SerialName("integration")
    val integration: Int,
    @SerialName("is_verified")
    val isVerified: Boolean,
    @SerialName("managed_by_integration")
    val managedByIntegration: Int? = null,
    @SerialName("metadata")
    val metadata: JsonElement? = null,
    @SerialName("migrate")
    val migrate: Boolean? = null,
    @SerialName("percentage_charge")
    val percentageCharge: Double,
    @SerialName("primary_contact_email")
    val primaryContactEmail: String? = null,
    @SerialName("primary_contact_name")
    val primaryContactName: String? = null,
    @SerialName("primary_contact_phone")
    val primaryContactPhone: String? = null,
    @SerialName("product")
    val product: String? = null,
    @SerialName("settlement_bank")
    val settlementBank: String,
    @SerialName("settlement_schedule")
    val settlementSchedule: String,
    @SerialName("subaccount_code")
    val subaccountCode: String,
    @SerialName("updatedAt")
    val updatedAt: String
)