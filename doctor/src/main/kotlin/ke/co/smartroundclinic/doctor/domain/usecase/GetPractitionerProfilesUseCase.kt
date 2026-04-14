package ke.co.smartroundclinic.doctor.domain.usecase

import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.doctor.domain.model.toModel
import ke.co.smartroundclinic.doctor.domain.repository.PractitionerProfileRepository
import ke.co.smartroundclinic.doctor.presentation.dto.response.PractitionerProfilePageResult
import ke.co.smartroundclinic.doctor.presentation.dto.response.toRes
import kotlin.math.ceil

class GetPractitionerProfilesUseCase(private val repository: PractitionerProfileRepository) {
    suspend operator fun invoke(page: Int, size: Int): DefaultResponse<PractitionerProfilePageResult?> =
        repository.getAll(page, size).toDefaultResponse { pair ->
            pair?.let { (items, total) ->
                PractitionerProfilePageResult(
                    items = items.map { it.toModel().toRes() },
                    total = total,
                    page = page,
                    size = size,
                    pages = ceil(total.toDouble() / size).toLong(),
                )
            }
        }
}
