package ke.co.smartroundclinic.payments.domain.usecase.subaccount

import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.payments.data.remote.dto.response.GetSubAccountRes
import ke.co.smartroundclinic.payments.domain.repository.PaystackRepository

class GetSubAccountUseCase(private val paystackRepository: PaystackRepository) {
    suspend operator fun invoke(idOrCode: String): DefaultResponse<GetSubAccountRes?> =
        paystackRepository.getSubAccount(idOrCode).toDefaultResponse { it }
}
