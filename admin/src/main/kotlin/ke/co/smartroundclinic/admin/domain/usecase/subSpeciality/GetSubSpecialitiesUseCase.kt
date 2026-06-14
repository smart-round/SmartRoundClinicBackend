package ke.co.smartroundclinic.admin.domain.usecase.subSpeciality

import ke.co.smartroundclinic.admin.domain.repository.SpecialityRepository
import ke.co.smartroundclinic.admin.domain.usecase.resolveIconUrl
import ke.co.smartroundclinic.admin.presentation.dto.response.SubSpecialityRes
import ke.co.smartroundclinic.admin.presentation.dto.response.toRes
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.infra.storage.StorageRepository

class GetSubSpecialitiesUseCase(
    private val specialityRepository: SpecialityRepository,
    private val storageRepository: StorageRepository,
) {
    suspend operator fun invoke(specialityId: String): DefaultResponse<List<SubSpecialityRes>?> {
        val result = specialityRepository.getSubSpecialities(specialityId)
        val mapped = (result as? Resource.Success)?.data?.map {
            it.copy(iconUrl = resolveIconUrl(it.iconUrl, storageRepository)).toModel().toRes()
        }
        return result.toDefaultResponse { mapped }
    }
}
