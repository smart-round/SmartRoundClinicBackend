package ke.co.smartroundclinic.payments.data.remote.instasend.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Body for POST /wallets/{originWalletId}/intra_transfer/ — originWalletId is a path param, not in this body. */
@Serializable
data class IntraTransferReq(
    @SerialName("wallet_id")
    val destinationWalletId: String,
    @SerialName("amount")
    val amount: String,
    @SerialName("narrative")
    val narrative: String,
)
