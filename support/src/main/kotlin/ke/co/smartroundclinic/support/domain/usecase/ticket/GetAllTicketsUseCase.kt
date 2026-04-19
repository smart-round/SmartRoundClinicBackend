package ke.co.smartroundclinic.support.domain.usecase.ticket

import ke.co.smartroundclinic.support.domain.repository.TicketRepository
import ke.co.smartroundclinic.support.presentation.dto.response.TicketPageResult
import ke.co.smartroundclinic.support.presentation.dto.response.toRes
import ke.co.smartroundclinic.common.DefaultResponse
import kotlin.math.ceil

class GetAllTicketsUseCase(private val repository: TicketRepository) {
    suspend operator fun invoke(page: Int, size: Int): DefaultResponse<TicketPageResult?> =
        repository.getAll(page, size)
            .toDefaultResponse(failedStatusCode = 400) { pair ->
                pair?.let { (items, total) ->
                    TicketPageResult(
                        items = items.map { (entity, categoryName, assigneeName) -> entity.toModel(categoryName, assigneeName).toRes() },
                        total = total,
                        page = page,
                        size = size,
                        pages = ceil(total.toDouble() / size).toLong(),
                    )
                }
            }
}
