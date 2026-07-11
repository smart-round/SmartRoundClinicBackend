package ke.co.smartroundclinic.scheduling.domain.usecase.appointment

import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.scheduling.data.lookup.RefundLookup
import ke.co.smartroundclinic.scheduling.domain.repository.AppointmentRepository
import ke.co.smartroundclinic.scheduling.presentation.dto.response.AppointmentRes
import ke.co.smartroundclinic.scheduling.presentation.dto.response.toAppointmentRefundRes
import ke.co.smartroundclinic.scheduling.presentation.dto.response.toRes

class GetAppointmentByIdUseCase(
    private val repository: AppointmentRepository,
    private val refundLookup: RefundLookup,
) {
    suspend operator fun invoke(id: String): DefaultResponse<AppointmentRes?> {
        val result = repository.getById(id)
        val model = result.data?.toModel()
        val refund = if (model?.status == "CANCELLED") {
            refundLookup.getByAppointmentId(id)?.toAppointmentRefundRes()
        } else {
            null
        }
        return result.toDefaultResponse(failedStatusCode = 404) { model?.toRes(refund = refund) }
    }
}
