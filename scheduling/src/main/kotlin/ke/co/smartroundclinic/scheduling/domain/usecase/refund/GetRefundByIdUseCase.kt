package ke.co.smartroundclinic.scheduling.domain.usecase.refund

import io.ktor.http.HttpStatusCode
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.scheduling.data.lookup.AppointmentAdminLookup
import ke.co.smartroundclinic.scheduling.data.lookup.RefundLookup
import ke.co.smartroundclinic.scheduling.presentation.dto.response.RefundRes
import ke.co.smartroundclinic.scheduling.presentation.dto.response.toRes

class GetRefundByIdUseCase(
    private val refundLookup: RefundLookup,
    private val nameLookup: AppointmentAdminLookup,
) {
    suspend operator fun invoke(id: String): DefaultResponse<RefundRes?> {
        val refund = refundLookup.getById(id)
            ?: return DefaultResponse(HttpStatusCode.NotFound.value, false, "Refund record not found", null)
        val names = nameLookup.getUserNames(listOf(refund.doctorId, refund.patientId))
        return DefaultResponse(
            httpStatusCode = HttpStatusCode.OK.value,
            status = true,
            message = "Refund fetched successfully",
            data = refund.toRes(names),
        )
    }
}
