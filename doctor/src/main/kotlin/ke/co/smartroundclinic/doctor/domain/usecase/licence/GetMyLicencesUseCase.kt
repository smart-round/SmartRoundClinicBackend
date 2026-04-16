package ke.co.smartroundclinic.doctor.domain.usecase.licence

import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.doctor.domain.model.toModel
import ke.co.smartroundclinic.doctor.domain.repository.PractitionerLicenceRepository
import ke.co.smartroundclinic.doctor.presentation.dto.response.PractitionerLicenceRes
import ke.co.smartroundclinic.doctor.presentation.dto.response.toRes
import ke.co.smartroundclinic.infra.AppConfig
import ke.co.smartroundclinic.infra.storage.StorageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GetMyLicencesUseCase(
    private val repository: PractitionerLicenceRepository,
    private val storageRepository: StorageRepository,
) {
    suspend operator fun invoke(doctorId: String): DefaultResponse<List<PractitionerLicenceRes>?> =
        withContext(Dispatchers.IO) {
            val entities = repository.getAll(doctorId)
            if (entities is Resource.Error) return@withContext entities.toDefaultResponse()
            val licences = entities.data?.map{ entity->
                val presignResult = storageRepository.presignedGetUrl(
                    bucket = AppConfig.r2.bucket,
                    key = entity.licenceUrl ?: "",
                    expiresInSeconds = 86400,
                )
                if (presignResult is Resource.Error) return@withContext presignResult.toDefaultResponse()
                entity.copy(licenceUrl = presignResult.data)
            }
            entities.toDefaultResponse { licences?.map { it.toModel().toRes() } }
        }
}