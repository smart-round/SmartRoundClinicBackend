package ke.co.smartroundclinic.payments.domain.usecase.wallet

import io.ktor.http.HttpStatusCode
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.payments.domain.service.DoctorWalletResolver
import ke.co.smartroundclinic.payments.presentation.dto.response.PlatformWalletBalanceRes

/** Admin lookup of any doctor's wallet balance by doctorId — mirrors GetWalletTransactionsUseCase's
 * own-wallet resolution, but for an admin viewing a specific doctor rather than the doctor themselves. */
class GetDoctorWalletBalanceAdminUseCase(
    private val walletResolver: DoctorWalletResolver,
    private val getWalletBalanceUseCase: GetPlatformWalletBalanceUseCase,
) {
    suspend operator fun invoke(doctorId: String): DefaultResponse<PlatformWalletBalanceRes?> {
        val walletId = walletResolver.resolve(doctorId)
            ?: return DefaultResponse(
                httpStatusCode = HttpStatusCode.BadGateway.value,
                status = false,
                message = "Unable to reach this doctor's wallet right now. Please try again shortly.",
                data = null,
            )
        return getWalletBalanceUseCase(walletId)
    }
}
