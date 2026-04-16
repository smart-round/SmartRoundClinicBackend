package ke.co.smartroundclinic.doctor.domain.usecase.specialization

import io.ktor.http.HttpStatusCode
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.doctor.domain.model.Specialization
import ke.co.smartroundclinic.doctor.domain.model.toEntity
import ke.co.smartroundclinic.doctor.domain.model.toModel
import ke.co.smartroundclinic.doctor.domain.repository.SpecializationRepository
import ke.co.smartroundclinic.doctor.presentation.dto.response.SpecializationRes
import ke.co.smartroundclinic.doctor.presentation.dto.response.toRes

class AddSpecializationUseCase(private val repository: SpecializationRepository) {
    suspend operator fun invoke(model: Specialization): DefaultResponse<SpecializationRes?> =
        repository.add(model.toEntity()).toDefaultResponse(
            successStatusCode = HttpStatusCode.Created.value,
            failedStatusCode = HttpStatusCode.Conflict.value,
            successMessage = "Specialization added successfully",
        ) { it?.toModel()?.toRes() }
}
