package ke.co.smartroundclinic.payments.domain.usecase.wallet

import io.ktor.http.HttpStatusCode
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.payments.domain.service.DoctorWalletResolver
import ke.co.smartroundclinic.payments.presentation.dto.response.WalletTransactionsPageRes

/** Admin lookup of any doctor's wallet statement by doctorId. */
class GetDoctorWalletStatementAdminUseCase(
    private val walletResolver: DoctorWalletResolver,
    private val getWalletStatementUseCase: GetPlatformWalletStatementUseCase,
) {
    suspend operator fun invoke(doctorId: String, page: Int): DefaultResponse<WalletTransactionsPageRes?> {
        val walletId = walletResolver.resolve(doctorId)
            ?: return DefaultResponse(
                httpStatusCode = HttpStatusCode.BadGateway.value,
                status = false,
                message = "Unable to reach this doctor's wallet right now. Please try again shortly.",
                data = null,
            )
        return getWalletStatementUseCase(walletId, page)
    }
}
