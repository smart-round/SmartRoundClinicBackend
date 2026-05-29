package ke.co.smartroundclinic.payments.data.remote.dto.response


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ListSubAccountsRes(
    @SerialName("data")
    val `data`: List<ListSubAccountData>,
    @SerialName("message")
    val message: String,
    @SerialName("meta")
    val meta: ListSubAccountMeta,
    @SerialName("status")
    val status: Boolean
)

@Serializable
data class ListSubAccountData(
    @SerialName("account_number")
    val accountNumber: String,
    @SerialName("active")
    val active: Int,
    @SerialName("bank_id")
    val bankId: Int,
    @SerialName("business_name")
    val businessName: String,
    @SerialName("currency")
    val currency: String,
    @SerialName("description")
    val description: String,
    @SerialName("id")
    val id: Int,
    @SerialName("percentage_charge")
    val percentageCharge: Int,
    @SerialName("settlement_bank")
    val settlementBank: String,
    @SerialName("subaccount_code")
    val subaccountCode: String
)

@Serializable
data class ListSubAccountMeta(
    @SerialName("page")
    val page: Int,
    @SerialName("pageCount")
    val pageCount: Int,
    @SerialName("perPage")
    val perPage: Int,
    @SerialName("skipped")
    val skipped: Int,
    @SerialName("total")
    val total: Int
)