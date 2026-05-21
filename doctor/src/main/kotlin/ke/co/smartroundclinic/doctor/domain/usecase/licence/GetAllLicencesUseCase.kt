package ke.co.smartroundclinic.doctor.domain.usecase.licence

import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.doctor.domain.model.toModel
import ke.co.smartroundclinic.doctor.domain.repository.PractitionerLicenceRepository
import ke.co.smartroundclinic.doctor.presentation.dto.response.LicencePageResult
import ke.co.smartroundclinic.doctor.presentation.dto.response.toRes
import ke.co.smartroundclinic.infra.AppConfig
import ke.co.smartroundclinic.infra.storage.StorageRepository
import kotlin.math.ceil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GetAllLicencesUseCase(
    private val repository: PractitionerLicenceRepository,
    private val storageRepository: StorageRepository,
) {
    suspend operator fun invoke(page: Int, size: Int): DefaultResponse<LicencePageResult?> =
        withContext(Dispatchers.IO) {
            val resource = repository.getAllPaged(page, size)
            if (resource is Resource.Error) return@withContext resource.toDefaultResponse()

            val (items, total) = resource.data!!
            val resolved = items.map { entity ->
                val presign = storageRepository.presignedGetUrl(
                    bucket = AppConfig.r2.bucket,
                    key = entity.licenceUrl ?: "",
                    expiresInSeconds = 86400,
                )
                if (presign is Resource.Error) return@withContext presign.toDefaultResponse()
                entity.copy(licenceUrl = presign.data)
            }

            resource.toDefaultResponse {
                LicencePageResult(
                    items = resolved.map { it.toModel().toRes() },
                    total = total,
                    page = page,
                    size = size,
                    pages = ceil(total.toDouble() / size).toLong(),
                )
            }
        }
}
