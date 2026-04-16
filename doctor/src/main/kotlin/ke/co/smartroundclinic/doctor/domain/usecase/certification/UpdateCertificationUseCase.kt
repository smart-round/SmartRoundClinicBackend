package ke.co.smartroundclinic.doctor.domain.usecase.certification

import io.ktor.http.HttpStatusCode
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.doctor.domain.model.toModel
import ke.co.smartroundclinic.doctor.domain.repository.CertificationRepository
import ke.co.smartroundclinic.doctor.presentation.dto.response.CertificationRes
import ke.co.smartroundclinic.doctor.presentation.dto.response.toRes

class UpdateCertificationUseCase(private val repository: CertificationRepository) {
    suspend operator fun invoke(
        id: String,
        doctorId: String,
        name: String?,
        date: String?
    ): DefaultResponse<CertificationRes?> =
        repository.update(id, doctorId, name, date).toDefaultResponse(
            failedStatusCode = HttpStatusCode.Companion.InternalServerError.value,
            successMessage = "Certification updated successfully",
        ) { it?.toModel()?.toRes() }.let { response ->
            if (response.status && response.data == null)
                response.copy(
                    httpStatusCode = HttpStatusCode.Companion.NotFound.value,
                    status = false,
                    message = "Certification not found",
                )
            else response
        }
}