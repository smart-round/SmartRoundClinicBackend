package ke.co.smartroundclinic.admin.domain.usecase

import ke.co.smartroundclinic.admin.domain.repository.SpecialityRepository
import ke.co.smartroundclinic.admin.presentation.dto.request.CreateSubSpecialityReq
import ke.co.smartroundclinic.admin.presentation.dto.response.SubSpecialityRes
import ke.co.smartroundclinic.common.DefaultResponse

class CreateSubSpecialityUseCase(
    private val specialityRepository: SpecialityRepository,
) {
    suspend operator fun invoke(
        specialityId: String,
        request: CreateSubSpecialityReq,
    ): DefaultResponse<SubSpecialityRes?> =
        specialityRepository.createSubSpeciality(specialityId, request.toEntity(specialityId))
            .toDefaultResponse { it?.toModel()?.toRes() }
}
