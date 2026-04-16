package ke.co.smartroundclinic.doctor.domain.usecase.bank

import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.doctor.domain.model.toModel
import ke.co.smartroundclinic.doctor.domain.repository.LocalBankRepository
import ke.co.smartroundclinic.doctor.presentation.dto.response.LocalBankPageResult
import ke.co.smartroundclinic.doctor.presentation.dto.response.toRes
import kotlin.math.ceil

class SearchLocalBanksByNameUseCase(private val repository: LocalBankRepository) {
    suspend operator fun invoke(query: String, page: Int, size: Int): DefaultResponse<LocalBankPageResult?> =
        repository.searchByName(query, page, size).toDefaultResponse { pair ->
            pair?.let { (items, total) ->
                LocalBankPageResult(
                    items = items.map { it.toModel().toRes() },
                    total = total,
                    page = page,
                    size = size,
                    pages = ceil(total.toDouble() / size).toLong(),
                )
            }
        }
}