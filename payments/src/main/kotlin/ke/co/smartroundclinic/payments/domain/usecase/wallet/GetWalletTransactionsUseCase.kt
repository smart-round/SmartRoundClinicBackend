package ke.co.smartroundclinic.payments.domain.usecase.wallet

import io.ktor.http.HttpStatusCode
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.payments.domain.repository.IntaSendRepository
import ke.co.smartroundclinic.payments.domain.service.DoctorWalletResolver
import ke.co.smartroundclinic.payments.presentation.dto.response.WalletTransactionsPageRes
import ke.co.smartroundclinic.payments.presentation.dto.response.toRes

/**
 * "Transactions" for a doctor is now IntaSend's own wallet ledger — it already carries both
 * earnings credited in (narrative "APT-...") and withdrawals debited out (narrative "Payout: #...")
 * as one feed, live from IntaSend, rather than our own locally-computed PaymentEntity history.
 */
class GetWalletTransactionsUseCase(
    private val intaSendRepository: IntaSendRepository,
    private val walletResolver: DoctorWalletResolver,
) {
    suspend operator fun invoke(doctorId: String, page: Int): DefaultResponse<WalletTransactionsPageRes?> {
        val safePage = maxOf(1, page)
        val walletId = walletResolver.resolve(doctorId)
            ?: return DefaultResponse(
                httpStatusCode = HttpStatusCode.BadGateway.value,
                status = false,
                message = "Unable to reach your wallet right now. Please try again shortly.",
                data = null,
            )

        return when (val result = intaSendRepository.getWalletTransactions(walletId, safePage)) {
            is Resource.Success -> {
                val transactionsPage = result.data!!
                DefaultResponse(
                    httpStatusCode = HttpStatusCode.OK.value,
                    status = true,
                    message = "Wallet transactions fetched successfully",
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
                message = result.message ?: "Failed to fetch wallet transactions",
                data = null,
            )
        }
    }
}
