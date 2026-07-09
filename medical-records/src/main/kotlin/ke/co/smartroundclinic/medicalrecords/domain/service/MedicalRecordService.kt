package ke.co.smartroundclinic.medicalrecords.domain.service

import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.medicalrecords.data.lookup.DoctorNameLookup
import ke.co.smartroundclinic.medicalrecords.domain.usecase.GetMedicalRecordByAppointmentUseCase
import ke.co.smartroundclinic.medicalrecords.domain.usecase.GetPatientMedicalHistoryUseCase
import ke.co.smartroundclinic.medicalrecords.domain.usecase.SaveMedicalRecordUseCase
import ke.co.smartroundclinic.medicalrecords.presentation.dto.request.SaveMedicalRecordReq
import ke.co.smartroundclinic.medicalrecords.presentation.dto.response.MedicalRecordRes

class MedicalRecordService(
    private val saveUseCase: SaveMedicalRecordUseCase,
    private val getByAppointmentUseCase: GetMedicalRecordByAppointmentUseCase,
    private val getPatientHistoryUseCase: GetPatientMedicalHistoryUseCase,
    private val doctorNameLookup: DoctorNameLookup,
) {
    suspend fun save(req: SaveMedicalRecordReq, doctorId: String, senderName: String): DefaultResponse<MedicalRecordRes?> =
        saveUseCase(req.toModel(doctorId), senderName).withDoctorName()

    suspend fun getByAppointment(appointmentId: String): DefaultResponse<MedicalRecordRes?> =
        getByAppointmentUseCase(appointmentId).withDoctorName()

    suspend fun getPatientHistory(patientId: String): DefaultResponse<List<MedicalRecordRes>?> =
        getPatientHistoryUseCase(patientId).withDoctorNames()

    private suspend fun DefaultResponse<MedicalRecordRes?>.withDoctorName(): DefaultResponse<MedicalRecordRes?> {
        val d = data ?: return this
        return copy(data = d.copy(doctorName = doctorNameLookup.lookup(d.doctorId)))
    }

    private suspend fun DefaultResponse<List<MedicalRecordRes>?>.withDoctorNames(): DefaultResponse<List<MedicalRecordRes>?> {
        val d = data ?: return this
        val names = doctorNameLookup.bulkLookup(d.map { it.doctorId }.toSet())
        return copy(data = d.map { it.copy(doctorName = names[it.doctorId]) })
    }
}
