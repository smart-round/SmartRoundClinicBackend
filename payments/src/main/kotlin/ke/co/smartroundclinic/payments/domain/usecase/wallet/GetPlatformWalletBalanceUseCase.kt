package ke.co.smartroundclinic.payments.domain.usecase.wallet

import io.ktor.http.HttpStatusCode
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.payments.domain.repository.IntaSendRepository
import ke.co.smartroundclinic.payments.presentation.dto.response.PlatformWalletBalanceRes
import ke.co.smartroundclinic.payments.presentation.dto.response.toRes

/** Balance for a platform-owned wallet (collections or commission), by walletId — admin-only. */
class GetPlatformWalletBalanceUseCase(
    private val intaSendRepository: IntaSendRepository,
) {
    suspend operator fun invoke(walletId: String): DefaultResponse<PlatformWalletBalanceRes?> =
        when (val result = intaSendRepository.getWallet(walletId)) {
            is Resource.Success -> DefaultResponse(
                httpStatusCode = HttpStatusCode.OK.value,
                status = true,
                message = "Wallet balance fetched successfully",
                data = result.data!!.toRes(),
            )
            is Resource.Error -> DefaultResponse(
                httpStatusCode = HttpStatusCode.BadGateway.value,
                status = false,
                message = result.message ?: "Failed to fetch wallet balance",
                data = null,
            )
        }
}
