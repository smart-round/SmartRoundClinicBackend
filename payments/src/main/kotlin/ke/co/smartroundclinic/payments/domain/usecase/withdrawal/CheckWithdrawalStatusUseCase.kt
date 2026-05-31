package ke.co.smartroundclinic.payments.domain.usecase.withdrawal

import io.ktor.http.HttpStatusCode
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.payments.data.remote.instasend.request.CheckSendMoneyStatusReq
import ke.co.smartroundclinic.payments.data.remote.instasend.reseponse.CheckSendMoneyStatusRes
import ke.co.smartroundclinic.payments.domain.repository.IntaSendRepository

class CheckWithdrawalStatusUseCase(private val repository: IntaSendRepository) {
    suspend operator fun invoke(trackingId: String): DefaultResponse<CheckSendMoneyStatusRes?> =
        repository.checkSendMoneyStatus(CheckSendMoneyStatusReq(trackingId))
            .toDefaultResponse(failedStatusCode = HttpStatusCode.BadGateway.value) { it }
}
