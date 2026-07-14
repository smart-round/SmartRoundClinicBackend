package ke.co.smartroundclinic.payments.presentation.dto.response

import kotlinx.serialization.Serializable

/**
 * Response shape for POST /doctor/payments/withdraw — a stable envelope regardless of outcome,
 * rather than passing through IntaSend's much larger internal send-money response. Exactly one of
 * [trackingId] or [insufficientBalance] is populated: [trackingId] on success (use it to poll
 * GET /doctor/payments/withdraw/status), [insufficientBalance] when the request was rejected
 * because the wallet balance can't cover the withdrawal amount plus IntaSend's transfer fee.
 */
@Serializable
data class WithdrawInitiateRes(
    val trackingId: String? = null,
    val status: String? = null,
    val insufficientBalance: InsufficientBalanceRes? = null,
)

/** Breakdown of why a withdrawal was rejected for insufficient balance — amounts in KES. */
@Serializable
data class InsufficientBalanceRes(
    val requestedAmount: Double,
    val feeEstimate: Double,
    val totalRequired: Double,
    val availableBalance: Double,
)
