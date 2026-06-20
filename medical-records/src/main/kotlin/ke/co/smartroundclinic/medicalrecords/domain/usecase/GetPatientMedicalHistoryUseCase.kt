package ke.co.smartroundclinic.medicalrecords.domain.usecase

import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.medicalrecords.domain.repository.MedicalRecordRepository
import ke.co.smartroundclinic.medicalrecords.presentation.dto.response.MedicalRecordRes
import ke.co.smartroundclinic.medicalrecords.presentation.dto.response.toRes

class GetPatientMedicalHistoryUseCase(private val repository: MedicalRecordRepository) {
    suspend operator fun invoke(patientId: String): DefaultResponse<List<MedicalRecordRes>?> =
        repository.getByPatientId(patientId).toDefaultResponse(failedStatusCode = 404) { list ->
            list?.map { it.toModel().toRes() }
        }
}
