package ke.co.smartroundclinic.patient.domain.usecase

import ke.co.smartroundclinic.patient.domain.repository.PersonalInformationRepository
import ke.co.smartroundclinic.patient.presentation.dto.request.UpdatePersonalInformationReq
import ke.co.smartroundclinic.patient.presentation.dto.response.PersonalInformationRes
import ke.co.smartroundclinic.patient.presentation.dto.response.toRes
import ke.co.smartroundclinic.common.DefaultResponse

class UpdatePersonalInformationUseCase(private val repository: PersonalInformationRepository) {
    suspend operator fun invoke(patientId: String, req: UpdatePersonalInformationReq): DefaultResponse<PersonalInformationRes?> =
        repository.update(
            patientId = patientId,
            phoneNumber = req.phoneNumber,
            countryCode = req.countryCode,
            bloodGroup = req.bloodGroup,
            dateOfBirth = req.dateOfBirth,
            weight = req.weight,
            weightIn = req.weightIn?.name,
            height = req.height,
            heightIn = req.heightIn?.name,
            maritalStatus = req.maritalStatus?.name,
            allergies = req.allergies,
            chronicConditions = req.chronicConditions,
            currentMedications = req.currentMedications,
        ).toDefaultResponse(failedStatusCode = 404) { it?.toModel()?.toRes() }
}
