package ke.co.smartroundclinic.payments.data.remote.instasend.reseponse

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response schema for internal wallet-to-wallet transfer is not documented in IntaSend's
 * structured API reference — every field is nullable/defaulted so parsing degrades gracefully
 * regardless of exact shape. Success is determined by HTTP status, not by these fields.
 */
@Serializable
data class IntraTransferRes(
    @SerialName("status")
    val status: String? = null,
    @SerialName("origin_wallet")
    val originWallet: Wallet? = null,
    @SerialName("destination_wallet")
    val destinationWallet: Wallet? = null,
)
