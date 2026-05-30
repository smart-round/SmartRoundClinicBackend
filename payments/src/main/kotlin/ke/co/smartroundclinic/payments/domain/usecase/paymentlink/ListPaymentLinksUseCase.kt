package ke.co.smartroundclinic.payments.domain.usecase.paymentlink

import io.ktor.http.HttpStatusCode
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.payments.data.remote.dto.response.PaginatedPaymentLinksRes
import ke.co.smartroundclinic.payments.domain.repository.IntaSendRepository

class ListPaymentLinksUseCase(
    private val repository: IntaSendRepository,
) {
    suspend operator fun invoke(page: Int): DefaultResponse<PaginatedPaymentLinksRes?> =
        repository.listPaymentLinks(page).toDefaultResponse(
            failedStatusCode = HttpStatusCode.BadGateway.value,
        ) { it }
}
