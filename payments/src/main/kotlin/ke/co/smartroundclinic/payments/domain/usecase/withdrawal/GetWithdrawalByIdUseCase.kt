package ke.co.smartroundclinic.payments.domain.usecase.withdrawal

import io.ktor.http.HttpStatusCode
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.payments.domain.repository.IntaSendRepository
import ke.co.smartroundclinic.payments.domain.service.DoctorWalletResolver
import ke.co.smartroundclinic.payments.presentation.dto.response.DoctorWithdrawalItemRes
import ke.co.smartroundclinic.payments.presentation.dto.response.toRes

private const val MAX_OWNERSHIP_SCAN_PAGES = 20

/**
 * Withdrawal detail is read live from IntaSend by transaction_id. IntaSend's single-transaction
 * response has no wallet_id field to check ownership against directly, so before returning detail
 * we confirm the id appears somewhere in this doctor's own wallet-scoped withdrawal list — same
 * 403-if-not-yours protection the old local-Mongo lookup had, just re-derived against IntaSend.
 */
class GetWithdrawalByIdUseCase(
    private val intaSendRepository: IntaSendRepository,
    private val walletResolver: DoctorWalletResolver,
) {
    suspend operator fun invoke(id: String, doctorId: String): DefaultResponse<DoctorWithdrawalItemRes?> {
        val walletId = walletResolver.resolve(doctorId)
            ?: return DefaultResponse(
                httpStatusCode = HttpStatusCode.BadGateway.value,
                status = false,
                message = "Unable to reach your wallet right now. Please try again shortly.",
                data = null,
            )

        if (!ownsTransaction(walletId, id)) {
            return DefaultResponse(
                httpStatusCode = HttpStatusCode.Forbidden.value,
                status = false,
                message = "You do not have access to this withdrawal",
                data = null,
            )
        }

        return when (val result = intaSendRepository.getSendMoneyTransactionById(id)) {
            is Resource.Success -> DefaultResponse(
                httpStatusCode = HttpStatusCode.OK.value,
                status = true,
                message = "Withdrawal fetched successfully",
                data = result.data?.toRes(),
            )
            is Resource.Error -> DefaultResponse(
                httpStatusCode = HttpStatusCode.NotFound.value,
                status = false,
                message = result.message ?: "Withdrawal not found",
                data = null,
            )
        }
    }

    private suspend fun ownsTransaction(walletId: String, transactionId: String): Boolean {
        var page = 1
        while (page <= MAX_OWNERSHIP_SCAN_PAGES) {
            val result = intaSendRepository.getSendMoneyTransactions(walletId, page)
            val transactionsPage = (result as? Resource.Success)?.data ?: return false
            if (transactionsPage.results.any { it.transactionId == transactionId }) return true
            if (transactionsPage.next == null) return false
            page++
        }
        return false
    }
}
