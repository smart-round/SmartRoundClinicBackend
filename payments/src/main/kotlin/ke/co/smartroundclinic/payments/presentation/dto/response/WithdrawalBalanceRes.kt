package ke.co.smartroundclinic.payments.presentation.dto.response

import kotlinx.serialization.Serializable

@Serializable
data class WithdrawalBalanceRes(
    val totalNetEarnings: Double,
    val totalWithdrawn: Double,
    val totalPending: Double,
    val totalCompleted: Double,
    val availableBalance: Double,
    val minimumWithdrawal: Double = 100.0,
)
