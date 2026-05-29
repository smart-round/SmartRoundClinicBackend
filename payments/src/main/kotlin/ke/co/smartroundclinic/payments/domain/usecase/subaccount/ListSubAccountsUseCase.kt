package ke.co.smartroundclinic.payments.domain.usecase.subaccount

import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.payments.data.remote.dto.response.ListSubAccountsRes
import ke.co.smartroundclinic.payments.domain.repository.PaystackRepository

class ListSubAccountsUseCase(private val paystackRepository: PaystackRepository) {
    suspend operator fun invoke(
        perPage: Int,
        page: Int,
        from: String?,
        to: String?,
    ): DefaultResponse<ListSubAccountsRes?> =
        paystackRepository.listSubAccounts(perPage, page, from, to).toDefaultResponse { it }
}
