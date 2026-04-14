package ke.co.smartroundclinic.doctor.domain.usecase

import io.ktor.http.HttpStatusCode
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.doctor.domain.model.PractitionerProfile
import ke.co.smartroundclinic.doctor.domain.model.toEntity
import ke.co.smartroundclinic.doctor.domain.model.toModel
import ke.co.smartroundclinic.doctor.domain.repository.PractitionerProfileRepository
import ke.co.smartroundclinic.doctor.presentation.dto.response.PractitionerProfileRes
import ke.co.smartroundclinic.doctor.presentation.dto.response.toRes

class CreatePractitionerProfileUseCase(private val repository: PractitionerProfileRepository) {
    suspend operator fun invoke(model: PractitionerProfile): DefaultResponse<PractitionerProfileRes?> =
        repository.create(model.toEntity()).toDefaultResponse(
            successStatusCode = HttpStatusCode.Created.value,
            failedStatusCode = HttpStatusCode.Conflict.value,
            successMessage = "Profile created successfully",
        ) { it?.toModel()?.toRes() }
}