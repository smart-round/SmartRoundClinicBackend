package ke.co.smartroundclinic.support.domain.usecase.issueCategory

import ke.co.smartroundclinic.support.domain.repository.IssueCategoryRepository
import ke.co.smartroundclinic.support.presentation.dto.response.IssueCategoryRes
import ke.co.smartroundclinic.support.presentation.dto.response.toRes
import ke.co.smartroundclinic.common.DefaultResponse

class GetIssueCategoryByIdUseCase(private val repository: IssueCategoryRepository) {
    suspend operator fun invoke(id: String): DefaultResponse<IssueCategoryRes?> =
        repository.getById(id)
            .toDefaultResponse(failedStatusCode = 404) { it?.toModel()?.toRes() }
}
