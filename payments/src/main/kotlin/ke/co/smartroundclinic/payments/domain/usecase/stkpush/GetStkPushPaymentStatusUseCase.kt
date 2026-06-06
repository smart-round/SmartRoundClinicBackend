package ke.co.smartroundclinic.payments.domain.usecase.stkpush

import io.ktor.http.HttpStatusCode
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.payments.data.remote.instasend.request.GetPaymentStatusReq
import ke.co.smartroundclinic.payments.data.remote.instasend.reseponse.GetPaymentStatusRes
import ke.co.smartroundclinic.payments.domain.repository.IntaSendRepository

class GetStkPushPaymentStatusUseCase(
    private val intaSendRepository: IntaSendRepository,
) {
    suspend operator fun invoke(invoiceId: String): DefaultResponse<GetPaymentStatusRes?> =
        intaSendRepository.getPaymentStatus(GetPaymentStatusReq(invoiceId))
            .toDefaultResponse(failedStatusCode = HttpStatusCode.BadGateway.value) { it }
}
