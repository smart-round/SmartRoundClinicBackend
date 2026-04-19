package ke.co.smartroundclinic.support.domain.usecase.issueCategory

import ke.co.smartroundclinic.support.domain.repository.IssueCategoryRepository
import ke.co.smartroundclinic.support.presentation.dto.request.UpdateIssueCategoryReq
import ke.co.smartroundclinic.support.presentation.dto.response.IssueCategoryRes
import ke.co.smartroundclinic.support.presentation.dto.response.toRes
import ke.co.smartroundclinic.common.DefaultResponse

class UpdateIssueCategoryUseCase(private val repository: IssueCategoryRepository) {
    suspend operator fun invoke(id: String, req: UpdateIssueCategoryReq): DefaultResponse<IssueCategoryRes?> =
        repository.update(id, req.name, req.description, req.status)
            .toDefaultResponse(failedStatusCode = 404) { it?.toModel()?.toRes() }
}
