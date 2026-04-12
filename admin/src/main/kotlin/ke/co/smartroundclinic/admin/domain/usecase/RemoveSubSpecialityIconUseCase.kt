package ke.co.smartroundclinic.admin.domain.usecase

import io.ktor.http.HttpStatusCode
import ke.co.smartroundclinic.admin.domain.repository.SpecialityRepository
import ke.co.smartroundclinic.admin.presentation.dto.response.SubSpecialityRes
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.infra.storage.StorageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext

class RemoveSubSpecialityIconUseCase(
    private val specialityRepository: SpecialityRepository,
    private val storageRepository: StorageRepository,
) {
    suspend operator fun invoke(id: String): DefaultResponse<SubSpecialityRes?> =
        withContext(Dispatchers.IO) {
            supervisorScope {
                val existing = specialityRepository.getSubSpeciality(id)
                val subspeciality = existing.data
                    ?: return@supervisorScope existing.toDefaultResponse(
                        failedStatusCode = HttpStatusCode.NotFound.value,
                        errorMessage = "Subspeciality not found"
                    ) { null }

                subspeciality.iconUrl?.let { url ->
                    launch {
                        val key = "subspeciality-icons/$id.${url.substringAfterLast(".")}"
                        storageRepository.delete(bucket = BUCKET, key = key)
                    }
                }

                specialityRepository.updateSubSpecialityIcon(id, null)
                    .toDefaultResponse(
                        successStatusCode = HttpStatusCode.OK.value,
                        successMessage = "Icon removed successfully"
                    ) { it?.toModel()?.toRes() }
            }
        }

    companion object {
        private val BUCKET get() = ke.co.smartroundclinic.infra.AppConfig.r2.bucket
    }
}
