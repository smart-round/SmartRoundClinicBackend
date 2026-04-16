package ke.co.smartroundclinic.doctor.domain.usecase.bank

import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.doctor.domain.model.toModel
import ke.co.smartroundclinic.doctor.domain.repository.LocalBankRepository
import ke.co.smartroundclinic.doctor.presentation.dto.response.LocalBankPageResult
import ke.co.smartroundclinic.doctor.presentation.dto.response.toRes
import kotlin.math.ceil

class GetAllLocalBanksUseCase(private val repository: LocalBankRepository) {
    suspend operator fun invoke(page: Int, size: Int): DefaultResponse<LocalBankPageResult?> =
        repository.getAll(page, size).toDefaultResponse { pair ->
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