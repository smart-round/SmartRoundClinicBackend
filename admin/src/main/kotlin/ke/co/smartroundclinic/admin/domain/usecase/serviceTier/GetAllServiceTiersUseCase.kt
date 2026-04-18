package ke.co.smartroundclinic.admin.domain.usecase.serviceTier

import ke.co.smartroundclinic.admin.domain.repository.ServiceTierRepository
import ke.co.smartroundclinic.admin.presentation.dto.response.ServiceTierPageResult
import ke.co.smartroundclinic.admin.presentation.dto.response.toRes
import ke.co.smartroundclinic.common.DefaultResponse
import kotlin.math.ceil

class GetAllServiceTiersUseCase(private val repository: ServiceTierRepository) {
    suspend operator fun invoke(page: Int, size: Int): DefaultResponse<ServiceTierPageResult?> =
        repository.getAllServiceTiers(page, size)
            .toDefaultResponse { pair ->
                pair?.let { (items, total) ->
                    ServiceTierPageResult(
                        items = items.map { it.toModel().toRes() },
                        total = total,
                        page = page,
                        size = size,
                        pages = ceil(total.toDouble() / size).toLong(),
                    )
                }
            }
}
