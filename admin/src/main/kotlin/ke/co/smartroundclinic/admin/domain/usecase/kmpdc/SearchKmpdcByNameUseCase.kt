package ke.co.smartroundclinic.admin.domain.usecase.kmpdc

import ke.co.smartroundclinic.admin.domain.repository.KmpdcRepository
import ke.co.smartroundclinic.admin.presentation.dto.response.KmpdcPageResult
import ke.co.smartroundclinic.admin.presentation.dto.response.toRes
import ke.co.smartroundclinic.common.DefaultResponse
import kotlin.math.ceil

class SearchKmpdcByNameUseCase(private val repository: KmpdcRepository) {
    suspend operator fun invoke(query: String, page: Int, size: Int): DefaultResponse<KmpdcPageResult?> =
        repository.searchByName(query, page, size)
            .toDefaultResponse(failedStatusCode = 400) { pair ->
                pair?.let { (items, total) ->
                    KmpdcPageResult(
                        items = items.map { it.toRes() },
                        total = total,
                        page = page,
                        size = size,
                        pages = ceil(total.toDouble() / size).toLong(),
                    )
                }
            }
}
