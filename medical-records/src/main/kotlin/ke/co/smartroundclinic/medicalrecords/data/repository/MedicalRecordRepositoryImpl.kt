package ke.co.smartroundclinic.medicalrecords.data.repository

import com.mongodb.client.model.Filters
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import ke.co.smartroundclinic.common.MongoDBConstants
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.medicalrecords.data.entity.MedicalRecordEntity
import ke.co.smartroundclinic.medicalrecords.domain.model.MedicalRecordField
import ke.co.smartroundclinic.medicalrecords.domain.model.MedicalRecordSaveResult
import ke.co.smartroundclinic.medicalrecords.domain.repository.MedicalRecordRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withContext
import java.time.Instant

class MedicalRecordRepositoryImpl(database: MongoDatabase) : MedicalRecordRepository {

    private val col = database.getCollection<MedicalRecordEntity>(MongoDBConstants.MEDICAL_RECORDS)

    override suspend fun upsert(entity: MedicalRecordEntity): Resource<MedicalRecordSaveResult?> =
        withContext(Dispatchers.IO) {
            try {
                val existing = col.find(Filters.eq(MedicalRecordEntity::appointmentId.name, entity.appointmentId)).firstOrNull()
                if (existing != null) {
                    if (existing.doctorId != entity.doctorId) {
                        return@withContext Resource.Error("You are not authorized to edit this medical record")
                    }
                    val updated = entity.copy(id = existing.id, createdAt = existing.createdAt, updatedAt = Instant.now().toString())
                    col.replaceOne(Filters.eq(MedicalRecordEntity::appointmentId.name, entity.appointmentId), updated)
                    Resource.Success(
                        // The previous document is already in hand for the ownership check, so
                        // diffing it costs no extra read.
                        data = MedicalRecordSaveResult(
                            record = updated,
                            isUpdate = true,
                            changedFields = changedFieldsBetween(existing, updated),
                        ),
                        message = "Medical record updated successfully",
                    )
                } else {
                    col.insertOne(entity)
                    Resource.Success(
                        data = MedicalRecordSaveResult(record = entity, isUpdate = false),
                        message = "Medical record saved successfully",
                    )
                }
            } catch (e: Exception) {
                Resource.Error(e.localizedMessage ?: "Failed to save medical record")
            }
        }

    /** Only the fields a patient is notified about — see [MedicalRecordField]. */
    private fun changedFieldsBetween(old: MedicalRecordEntity, new: MedicalRecordEntity): Set<MedicalRecordField> =
        buildSet {
            if (old.diagnosis != new.diagnosis) add(MedicalRecordField.DIAGNOSIS)
            if (old.prescription != new.prescription) add(MedicalRecordField.PRESCRIPTION)
            if (old.labRequests != new.labRequests) add(MedicalRecordField.LAB_REQUESTS)
            if (old.referralNote != new.referralNote) add(MedicalRecordField.REFERRAL_NOTE)
        }

    override suspend fun getByAppointmentId(appointmentId: String): Resource<MedicalRecordEntity?> =
        withContext(Dispatchers.IO) {
            try {
                val entity = col.find(Filters.eq(MedicalRecordEntity::appointmentId.name, appointmentId)).firstOrNull()
                    ?: return@withContext Resource.Error("Medical record not found")
                Resource.Success(data = entity)
            } catch (e: Exception) {
                Resource.Error(e.localizedMessage ?: "Failed to retrieve medical record")
            }
        }

    override suspend fun getByPatientId(patientId: String): Resource<List<MedicalRecordEntity>> =
        withContext(Dispatchers.IO) {
            try {
                val entities = col.find(Filters.eq(MedicalRecordEntity::patientId.name, patientId)).toList()
                Resource.Success(data = entities)
            } catch (e: Exception) {
                Resource.Error(e.localizedMessage ?: "Failed to retrieve medical history")
            }
        }
}
