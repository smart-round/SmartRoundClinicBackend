package ke.co.smartroundclinic.admin.domain.usecase.speciality

import ke.co.smartroundclinic.admin.domain.repository.SpecialityRepository
import ke.co.smartroundclinic.admin.domain.usecase.resolveIconUrl
import ke.co.smartroundclinic.admin.presentation.dto.response.SpecialityRes
import ke.co.smartroundclinic.admin.presentation.dto.response.toRes
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.infra.storage.StorageRepository

class GetSpecialityByIdUseCase(
    private val specialityRepository: SpecialityRepository,
    private val storageRepository: StorageRepository,
) {
    suspend operator fun invoke(id: String): DefaultResponse<SpecialityRes?> {
        val result = specialityRepository.getSpecialityById(id)
        val mapped = (result as? Resource.Success)?.data?.let {
            it.copy(iconUrl = resolveIconUrl(it.iconUrl, storageRepository)).toModel().toRes()
        }
        return result.toDefaultResponse { mapped }
    }
}
