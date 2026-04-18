package ke.co.smartroundclinic.admin.domain.usecase.speciality

import io.ktor.http.HttpStatusCode
import ke.co.smartroundclinic.admin.domain.repository.SpecialityRepository
import ke.co.smartroundclinic.admin.presentation.dto.response.SpecialityRes
import ke.co.smartroundclinic.admin.presentation.dto.response.toRes
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.infra.AppConfig
import ke.co.smartroundclinic.infra.storage.StorageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext

class RemoveSpecialityIconUseCase(
    private val specialityRepository: SpecialityRepository,
    private val storageRepository: StorageRepository,
) {
    suspend operator fun invoke(id: String): DefaultResponse<SpecialityRes?> =
        withContext(Dispatchers.IO) {
            supervisorScope {
                val existing = specialityRepository.getSpecialityById(id)
                val speciality = existing.data
                    ?: return@supervisorScope existing.toDefaultResponse(
                        failedStatusCode = HttpStatusCode.Companion.NotFound.value,
                        errorMessage = "Speciality not found"
                    ) { null }

                speciality.iconUrl?.let { url ->
                    launch {
                        val key = "speciality-icons/$id.${url.substringAfterLast(".")}"
                        storageRepository.delete(bucket = BUCKET, key = key)
                    }
                }

                specialityRepository.updateSpecialityIcon(id, null)
                    .toDefaultResponse(
                        successStatusCode = HttpStatusCode.Companion.OK.value,
                        successMessage = "Icon removed successfully"
                    ) { it?.toModel()?.toRes() }
            }
        }

    companion object {
        private val BUCKET get() = AppConfig.r2.bucket
    }
}