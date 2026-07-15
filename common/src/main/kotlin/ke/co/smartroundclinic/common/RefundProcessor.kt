package ke.co.smartroundclinic.common

/** Admin-triggered — there is no automatic refund processing. See implementations for details. */
interface RefundProcessor {
    suspend fun processRefund(refundId: String): RefundProcessResult
}

sealed class RefundProcessResult {
    data class Submitted(val trackingId: String) : RefundProcessResult()

    /** The collections wallet's available balance can't cover this refund right now — the refund
     * stays PENDING so an admin can retry once the wallet is topped up. */
    data class InsufficientBalance(val requiredAmount: Double, val availableBalance: Double) : RefundProcessResult()
    data class Failed(val reason: String) : RefundProcessResult()
    data object NotFound : RefundProcessResult()
    data object NotPending : RefundProcessResult()
}
