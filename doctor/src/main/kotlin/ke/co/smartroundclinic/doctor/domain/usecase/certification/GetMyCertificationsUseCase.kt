package ke.co.smartroundclinic.doctor.domain.usecase.certification

import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.doctor.domain.model.toModel
import ke.co.smartroundclinic.doctor.domain.repository.CertificationRepository
import ke.co.smartroundclinic.doctor.presentation.dto.response.CertificationRes
import ke.co.smartroundclinic.doctor.presentation.dto.response.PractitionerLicenceRes
import ke.co.smartroundclinic.doctor.presentation.dto.response.toRes
import ke.co.smartroundclinic.infra.AppConfig
import ke.co.smartroundclinic.infra.storage.StorageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GetMyCertificationsUseCase(
    private val repository: CertificationRepository,
    private val storageRepository: StorageRepository,
) {

    suspend operator fun invoke(doctorId: String): DefaultResponse<List<CertificationRes>?> =
        withContext(Dispatchers.IO) {
            val entities = repository.getAll(doctorId)
            if (entities is Resource.Error) return@withContext entities.toDefaultResponse()
            val licences = entities.data?.map { entity ->
                val presignResult = storageRepository.presignedGetUrl(
                    bucket = AppConfig.r2.bucket,
                    key = entity.certificationUrl ?: "",
                    expiresInSeconds = 86400,
                )
                if (presignResult is Resource.Error) return@withContext presignResult.toDefaultResponse()
                entity.copy(certificationUrl = presignResult.data)
            }
            entities.toDefaultResponse { licences?.map { it.toModel().toRes() } }
        }
}