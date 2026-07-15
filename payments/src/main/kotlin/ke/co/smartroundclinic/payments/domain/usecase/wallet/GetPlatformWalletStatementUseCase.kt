package ke.co.smartroundclinic.payments.domain.usecase.wallet

import io.ktor.http.HttpStatusCode
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.payments.domain.repository.IntaSendRepository
import ke.co.smartroundclinic.payments.presentation.dto.response.WalletTransactionsPageRes
import ke.co.smartroundclinic.payments.presentation.dto.response.toRes

/** Transaction statement for a platform-owned wallet (collections or commission) — admin-only. */
class GetPlatformWalletStatementUseCase(
    private val intaSendRepository: IntaSendRepository,
) {
    suspend operator fun invoke(walletId: String, page: Int): DefaultResponse<WalletTransactionsPageRes?> {
        val safePage = maxOf(1, page)
        return when (val result = intaSendRepository.getWalletTransactions(walletId, safePage)) {
            is Resource.Success -> {
                val transactionsPage = result.data!!
                DefaultResponse(
                    httpStatusCode = HttpStatusCode.OK.value,
                    status = true,
                    message = "Wallet statement fetched successfully",
                    data = WalletTransactionsPageRes(
                        items = transactionsPage.results.map { it.toRes() },
                        total = transactionsPage.count,
                        page = safePage,
                        hasMore = transactionsPage.next != null,
                    ),
                )
            }
            is Resource.Error -> DefaultResponse(
                httpStatusCode = HttpStatusCode.BadGateway.value,
                status = false,
                message = result.message ?: "Failed to fetch wallet statement",
                data = null,
            )
        }
    }
}
