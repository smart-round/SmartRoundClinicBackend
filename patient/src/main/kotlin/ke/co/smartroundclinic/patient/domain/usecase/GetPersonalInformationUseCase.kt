package ke.co.smartroundclinic.patient.domain.usecase

import ke.co.smartroundclinic.patient.domain.repository.PersonalInformationRepository
import ke.co.smartroundclinic.patient.presentation.dto.response.PersonalInformationRes
import ke.co.smartroundclinic.patient.presentation.dto.response.toRes
import ke.co.smartroundclinic.common.DefaultResponse

class GetPersonalInformationUseCase(private val repository: PersonalInformationRepository) {
    suspend operator fun invoke(patientId: String): DefaultResponse<PersonalInformationRes?> =
        repository.getByPatientId(patientId)
            .toDefaultResponse(failedStatusCode = 404) { it?.toModel()?.toRes() }
}
