package ke.co.smartroundclinic.medicalrecords.domain.service

import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.medicalrecords.domain.usecase.GetMedicalRecordByAppointmentUseCase
import ke.co.smartroundclinic.medicalrecords.domain.usecase.GetPatientMedicalHistoryUseCase
import ke.co.smartroundclinic.medicalrecords.domain.usecase.SaveMedicalRecordUseCase
import ke.co.smartroundclinic.medicalrecords.presentation.dto.request.SaveMedicalRecordReq
import ke.co.smartroundclinic.medicalrecords.presentation.dto.response.MedicalRecordRes

class MedicalRecordService(
    private val saveUseCase: SaveMedicalRecordUseCase,
    private val getByAppointmentUseCase: GetMedicalRecordByAppointmentUseCase,
    private val getPatientHistoryUseCase: GetPatientMedicalHistoryUseCase,
) {
    suspend fun save(req: SaveMedicalRecordReq, doctorId: String, senderName: String): DefaultResponse<MedicalRecordRes?> =
        saveUseCase(req.toModel(doctorId), senderName)

    suspend fun getByAppointment(appointmentId: String): DefaultResponse<MedicalRecordRes?> =
        getByAppointmentUseCase(appointmentId)

    suspend fun getPatientHistory(patientId: String): DefaultResponse<List<MedicalRecordRes>?> =
        getPatientHistoryUseCase(patientId)
}
