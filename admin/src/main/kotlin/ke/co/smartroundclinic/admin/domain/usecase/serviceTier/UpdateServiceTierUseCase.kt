package ke.co.smartroundclinic.admin.domain.usecase.serviceTier

import ke.co.smartroundclinic.admin.domain.repository.ServiceTierRepository
import ke.co.smartroundclinic.admin.presentation.dto.request.UpdateServiceTierReq
import ke.co.smartroundclinic.admin.presentation.dto.response.ServiceTierRes
import ke.co.smartroundclinic.admin.presentation.dto.response.toRes
import ke.co.smartroundclinic.common.DefaultResponse

class UpdateServiceTierUseCase(private val repository: ServiceTierRepository) {
    suspend operator fun invoke(id: String, req: UpdateServiceTierReq): DefaultResponse<ServiceTierRes?> =
        repository.updateServiceTier(
            id = id,
            name = req.name,
            tierPrice = req.tierPrice,
            consultationDuration = req.consultationDuration,
            gracePeriod = req.gracePeriod,
            chatAccessWindow = req.chatAccessWindow,
            followUpWindow = req.followUpWindow,
            followUpFee = req.followUpFee,
        ).toDefaultResponse { it?.toModel()?.toRes() }
}
