package ke.co.smartroundclinic.support.domain.usecase.issueCategory

import ke.co.smartroundclinic.support.data.entity.toEntity
import ke.co.smartroundclinic.support.domain.repository.IssueCategoryRepository
import ke.co.smartroundclinic.support.presentation.dto.request.CreateIssueCategoryReq
import ke.co.smartroundclinic.support.presentation.dto.response.IssueCategoryRes
import ke.co.smartroundclinic.support.presentation.dto.response.toRes
import ke.co.smartroundclinic.common.DefaultResponse

class CreateIssueCategoryUseCase(private val repository: IssueCategoryRepository) {
    suspend operator fun invoke(req: CreateIssueCategoryReq): DefaultResponse<IssueCategoryRes?> =
        repository.create(req.toModel().toEntity())
            .toDefaultResponse(successStatusCode = 201, failedStatusCode = 409) { it?.toModel()?.toRes() }
}
