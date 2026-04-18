package ke.co.smartroundclinic.admin.domain.usecase.subSpeciality

import ke.co.smartroundclinic.admin.data.entity.toEntity
import ke.co.smartroundclinic.admin.domain.repository.SpecialityRepository
import ke.co.smartroundclinic.admin.presentation.dto.request.CreateSubSpecialityReq
import ke.co.smartroundclinic.admin.presentation.dto.response.SubSpecialityRes
import ke.co.smartroundclinic.admin.presentation.dto.response.toRes
import ke.co.smartroundclinic.common.DefaultResponse

class CreateSubSpecialityUseCase(
    private val specialityRepository: SpecialityRepository,
) {
    suspend operator fun invoke(
        specialityId: String,
        request: CreateSubSpecialityReq,
    ): DefaultResponse<SubSpecialityRes?> =
        specialityRepository.createSubSpeciality(specialityId, request.toModel(specialityId).toEntity())
            .toDefaultResponse { it?.toModel()?.toRes() }
}
