package ke.co.smartroundclinic.payments.domain.usecase.verification

import io.ktor.http.HttpStatusCode
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.payments.data.remote.dto.response.ResolveCardBinData
import ke.co.smartroundclinic.payments.domain.repository.PaystackRepository

class ResolveCardBinUseCase(private val paystackRepository: PaystackRepository) {
    suspend operator fun invoke(bin: String): DefaultResponse<ResolveCardBinData?> =
        paystackRepository.resolveCardBin(bin).toDefaultResponse(
            successStatusCode = HttpStatusCode.OK.value,
            failedStatusCode = HttpStatusCode.UnprocessableEntity.value,
        ) { it?.data }
}
