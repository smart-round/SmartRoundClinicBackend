package ke.co.smartroundclinic.payments.presentation.dto.response

import ke.co.smartroundclinic.payments.data.remote.instasend.reseponse.Wallet
import kotlinx.serialization.Serializable

/** Balance for a platform-owned wallet (collections or commission) — not a doctor's own wallet. */
@Serializable
data class PlatformWalletBalanceRes(
    val walletId: String,
    val label: String,
    val currency: String,
    val availableBalance: Double,
    val currentBalance: Double,
    val canDisburse: Boolean,
)

fun Wallet.toRes(): PlatformWalletBalanceRes = PlatformWalletBalanceRes(
    walletId = walletId,
    label = label,
    currency = currency,
    availableBalance = availableBalance,
    currentBalance = currentBalance,
    canDisburse = canDisburse,
)
