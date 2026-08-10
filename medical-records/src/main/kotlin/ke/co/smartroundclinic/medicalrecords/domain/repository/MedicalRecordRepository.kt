package ke.co.smartroundclinic.medicalrecords.domain.repository

import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.medicalrecords.data.entity.MedicalRecordEntity
import ke.co.smartroundclinic.medicalrecords.domain.model.MedicalRecordSaveResult

interface MedicalRecordRepository {
    suspend fun upsert(entity: MedicalRecordEntity): Resource<MedicalRecordSaveResult?>
    suspend fun getByAppointmentId(appointmentId: String): Resource<MedicalRecordEntity?>
    suspend fun getByPatientId(patientId: String): Resource<List<MedicalRecordEntity>>
}
