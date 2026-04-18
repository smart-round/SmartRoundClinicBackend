package ke.co.smartroundclinic.admin.domain.usecase.kmpdc

import ke.co.smartroundclinic.admin.domain.repository.KmpdcRepository
import ke.co.smartroundclinic.admin.presentation.dto.response.KmpdcPractitionerRes
import ke.co.smartroundclinic.admin.presentation.dto.response.toRes
import ke.co.smartroundclinic.common.DefaultResponse
import io.ktor.http.HttpStatusCode

class FindKmpdcByRegNumberUseCase(private val repository: KmpdcRepository) {
    suspend operator fun invoke(regNumber: String): DefaultResponse<KmpdcPractitionerRes?> =
        repository.findByRegNumber(regNumber).toDefaultResponse(
            failedStatusCode = HttpStatusCode.InternalServerError.value,
        ) { entity ->
            entity?.toRes()
        }.let { response ->
            if (response.status && response.data == null) {
                response.copy(
                    httpStatusCode = HttpStatusCode.NotFound.value,
                    status = false,
                    message = "No practitioner found with registration number: $regNumber",
                )
            } else response
        }
}
