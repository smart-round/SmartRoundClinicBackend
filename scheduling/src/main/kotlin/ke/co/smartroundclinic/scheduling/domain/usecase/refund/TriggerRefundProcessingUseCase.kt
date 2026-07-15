package ke.co.smartroundclinic.scheduling.domain.usecase.refund

import io.ktor.http.HttpStatusCode
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.RefundProcessResult
import ke.co.smartroundclinic.common.RefundProcessor
import ke.co.smartroundclinic.scheduling.presentation.dto.response.RefundProcessRes

/** Admin action — submits a specific PENDING refund to the payment provider. See RefundProcessor. */
class TriggerRefundProcessingUseCase(
    private val refundProcessor: RefundProcessor?,
) {
    suspend operator fun invoke(id: String): DefaultResponse<RefundProcessRes?> {
        if (refundProcessor == null) {
            return DefaultResponse(
                httpStatusCode = HttpStatusCode.ServiceUnavailable.value,
                status = false,
                message = "Refund processing is not available right now",
                data = null,
            )
        }

        return when (val result = refundProcessor.processRefund(id)) {
            is RefundProcessResult.Submitted -> DefaultResponse(
                httpStatusCode = HttpStatusCode.OK.value,
                status = true,
                message = "Refund submitted to payment provider",
                data = RefundProcessRes(status = "SUBMITTED", trackingId = result.trackingId),
            )
            is RefundProcessResult.InsufficientBalance -> DefaultResponse(
                httpStatusCode = HttpStatusCode.Conflict.value,
                status = false,
                message = "Collections wallet available balance (KES ${"%.2f".format(result.availableBalance)}) " +
                    "is insufficient to cover this refund of KES ${"%.2f".format(result.requiredAmount)}",
                data = RefundProcessRes(
                    status = "INSUFFICIENT_BALANCE",
                    requiredAmount = result.requiredAmount,
                    availableBalance = result.availableBalance,
                ),
            )
            is RefundProcessResult.Failed -> DefaultResponse(
                httpStatusCode = HttpStatusCode.BadGateway.value,
                status = false,
                message = result.reason,
                data = null,
            )
            RefundProcessResult.NotFound -> DefaultResponse(
                httpStatusCode = HttpStatusCode.NotFound.value,
                status = false,
                message = "Refund not found",
                data = null,
            )
            RefundProcessResult.NotPending -> DefaultResponse(
                httpStatusCode = HttpStatusCode.Conflict.value,
                status = false,
                message = "Refund is not in a PENDING state",
                data = null,
            )
        }
    }
}
