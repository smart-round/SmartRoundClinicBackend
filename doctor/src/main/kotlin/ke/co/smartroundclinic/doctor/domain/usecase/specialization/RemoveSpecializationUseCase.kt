package ke.co.smartroundclinic.doctor.domain.usecase.specialization

import io.ktor.http.HttpStatusCode
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.doctor.domain.repository.SpecializationRepository

class RemoveSpecializationUseCase(private val repository: SpecializationRepository) {
    suspend operator fun invoke(id: String, doctorId: String): DefaultResponse<Boolean?> =
        repository.remove(id, doctorId).toDefaultResponse(
            failedStatusCode = HttpStatusCode.InternalServerError.value,
            successMessage = "Specialization removed successfully",
        ) { it }.let { response ->
            if (response.status && response.data == false)
                response.copy(
                    httpStatusCode = HttpStatusCode.NotFound.value,
                    status = false,
                    message = "Specialization not found",
                )
            else response
        }
}