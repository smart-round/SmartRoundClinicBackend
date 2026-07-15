package ke.co.smartroundclinic.scheduling.domain.service

import ke.co.smartroundclinic.scheduling.domain.usecase.refund.GetAdminAllRefundsUseCase
import ke.co.smartroundclinic.scheduling.domain.usecase.refund.GetRefundByIdUseCase
import ke.co.smartroundclinic.scheduling.domain.usecase.refund.TriggerRefundProcessingUseCase

class AdminRefundService(
    private val listUseCase: GetAdminAllRefundsUseCase,
    private val getByIdUseCase: GetRefundByIdUseCase,
    private val triggerProcessingUseCase: TriggerRefundProcessingUseCase,
) {
    suspend fun getAll(status: String?, page: Int, size: Int) = listUseCase(status, page, size)
    suspend fun getById(id: String) = getByIdUseCase(id)
    suspend fun process(id: String) = triggerProcessingUseCase(id)
}
