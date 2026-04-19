package ke.co.smartroundclinic.support.domain.usecase.issueCategory

import ke.co.smartroundclinic.support.domain.repository.IssueCategoryRepository
import ke.co.smartroundclinic.support.presentation.dto.response.IssueCategoryPageResult
import ke.co.smartroundclinic.support.presentation.dto.response.toRes
import ke.co.smartroundclinic.common.DefaultResponse
import kotlin.math.ceil

class GetAllIssueCategoriesUseCase(private val repository: IssueCategoryRepository) {
    suspend operator fun invoke(page: Int, size: Int): DefaultResponse<IssueCategoryPageResult?> =
        repository.getAll(page, size)
            .toDefaultResponse(failedStatusCode = 400) { pair ->
                pair?.let { (items, total) ->
                    IssueCategoryPageResult(
                        items = items.map { it.toModel().toRes() },
                        total = total,
                        page = page,
                        size = size,
                        pages = ceil(total.toDouble() / size).toLong(),
                    )
                }
            }
}
