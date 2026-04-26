package ke.co.smartroundclinic.admin.domain.usecase.serviceCategory

import ke.co.smartroundclinic.admin.domain.repository.ServiceCategoryRepository
import ke.co.smartroundclinic.admin.presentation.dto.response.ServiceCategoryPageResult
import ke.co.smartroundclinic.admin.presentation.dto.response.toRes
import ke.co.smartroundclinic.common.DefaultResponse
import kotlin.math.ceil

class GetAllServiceCategoriesUseCase(private val repository: ServiceCategoryRepository) {
    suspend operator fun invoke(page: Int, size: Int): DefaultResponse<ServiceCategoryPageResult?> =
        repository.getAll(page, size).toDefaultResponse { pair ->
            pair?.let { (items, total) ->
                ServiceCategoryPageResult(
                    items = items.map { it.toModel().toRes() },
                    total = total,
                    page = page,
                    size = size,
                    pages = ceil(total.toDouble() / size).toLong(),
                )
            }
        }
}
