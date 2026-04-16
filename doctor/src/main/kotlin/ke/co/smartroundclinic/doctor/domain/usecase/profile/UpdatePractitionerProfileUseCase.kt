package ke.co.smartroundclinic.doctor.domain.usecase.profile

import io.ktor.http.HttpStatusCode
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.doctor.domain.model.PractitionerProfileUpdate
import ke.co.smartroundclinic.doctor.domain.model.toModel
import ke.co.smartroundclinic.doctor.domain.repository.PractitionerProfileRepository
import ke.co.smartroundclinic.doctor.presentation.dto.response.PractitionerProfileRes
import ke.co.smartroundclinic.doctor.presentation.dto.response.toRes

class UpdatePractitionerProfileUseCase(private val repository: PractitionerProfileRepository) {
    suspend operator fun invoke(
        doctorId: String,
        update: PractitionerProfileUpdate,
    ): DefaultResponse<PractitionerProfileRes?> =
        repository.update(doctorId, update).toDefaultResponse(
            failedStatusCode = HttpStatusCode.Companion.InternalServerError.value,
            successMessage = "Profile updated successfully",
        ) { it?.toModel()?.toRes() }
}