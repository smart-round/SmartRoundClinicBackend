package ke.co.smartroundclinic.medicalrecords.domain.usecase

import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.medicalrecords.domain.repository.MedicalRecordRepository
import ke.co.smartroundclinic.medicalrecords.presentation.dto.response.MedicalRecordRes
import ke.co.smartroundclinic.medicalrecords.presentation.dto.response.toRes

class GetMedicalRecordByAppointmentUseCase(private val repository: MedicalRecordRepository) {
    suspend operator fun invoke(appointmentId: String): DefaultResponse<MedicalRecordRes?> =
        repository.getByAppointmentId(appointmentId).toDefaultResponse(failedStatusCode = 404) { it?.toModel()?.toRes() }
}
