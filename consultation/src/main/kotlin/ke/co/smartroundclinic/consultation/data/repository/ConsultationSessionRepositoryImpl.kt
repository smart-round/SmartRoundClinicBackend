package ke.co.smartroundclinic.consultation.data.repository

import com.mongodb.client.model.Filters
import com.mongodb.client.model.FindOneAndUpdateOptions
import com.mongodb.client.model.ReturnDocument
import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import ke.co.smartroundclinic.common.MongoDBConstants
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.consultation.data.entity.ConsultationSessionEntity
import ke.co.smartroundclinic.consultation.data.entity.ConsultationStatus
import ke.co.smartroundclinic.consultation.domain.repository.ConsultationSessionRepository
import kotlinx.coroutines.flow.firstOrNull
import org.bson.Document
import org.slf4j.LoggerFactory
import kotlin.time.Clock

class ConsultationSessionRepositoryImpl(
    consultationDb: MongoDatabase,
    schedulingDb: MongoDatabase,
) : ConsultationSessionRepository {

    private val log = LoggerFactory.getLogger(ConsultationSessionRepositoryImpl::class.java)
    private val col = consultationDb.getCollection<ConsultationSessionEntity>(MongoDBConstants.CONSULTATION_SESSIONS)
    private val appointmentsCol = schedulingDb.getCollection<Document>(MongoDBConstants.APPOINTMENTS)

    override suspend fun startOrGet(appointmentId: String, userId: String): Resource<ConsultationSessionEntity> = try {
        val appointment = appointmentsCol.find(Filters.eq("id", appointmentId)).firstOrNull()
            ?: return Resource.Error("Appointment not found")

        val doctorId = appointment.getString("doctorId")
            ?: return Resource.Error("Invalid appointment data")
        val patientId = appointment.getString("patientId")
            ?: return Resource.Error("Invalid appointment data")

        if (userId != doctorId && userId != patientId)
            return Resource.Error("You are not a participant of this appointment")

        val status = appointment.getString("status") ?: ""
        if (status != "CONFIRMED" && status != "COMPLETED")
            return Resource.Error("Consultation is only available for confirmed or completed appointments")

        // Return existing session if one already exists for this appointment
        val existing = col.find(Filters.eq(ConsultationSessionEntity::appointmentId.name, appointmentId)).firstOrNull()
        if (existing != null) {
            log.info("Returning existing consultation for appointmentId=$appointmentId")
            return Resource.Success(existing, "Consultation retrieved")
        }

        val entity = ConsultationSessionEntity(
            appointmentId = appointmentId,
            doctorId = doctorId,
            patientId = patientId,
        )
        col.insertOne(entity)
        log.info("Created consultation id=${entity.id} for appointmentId=$appointmentId")
        Resource.Success(entity, "Consultation started")
    } catch (e: Exception) {
        log.error("Failed to start consultation — ${e.message}", e)
        Resource.Error(e.message ?: "Failed to start consultation")
    }

    override suspend fun getById(id: String): Resource<ConsultationSessionEntity?> = try {
        Resource.Success(col.find(Filters.eq(ConsultationSessionEntity::id.name, id)).firstOrNull())
    } catch (e: Exception) {
        Resource.Error(e.message ?: "Failed to fetch consultation")
    }

    override suspend fun getByAppointmentId(appointmentId: String): Resource<ConsultationSessionEntity?> = try {
        Resource.Success(col.find(Filters.eq(ConsultationSessionEntity::appointmentId.name, appointmentId)).firstOrNull())
    } catch (e: Exception) {
        Resource.Error(e.message ?: "Failed to fetch consultation")
    }

    override suspend fun end(id: String, doctorId: String): Resource<ConsultationSessionEntity?> = try {
        val updated = col.findOneAndUpdate(
            Filters.and(
                Filters.eq(ConsultationSessionEntity::id.name, id),
                Filters.eq(ConsultationSessionEntity::doctorId.name, doctorId),
            ),
            Updates.combine(
                Updates.set(ConsultationSessionEntity::status.name, ConsultationStatus.ENDED),
                Updates.set(ConsultationSessionEntity::updatedAt.name, Clock.System.now().toString()),
            ),
            FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER),
        )
        if (updated == null) log.warn("No active consultation found id=$id for doctorId=$doctorId")
        else log.info("Consultation id=$id ended by doctorId=$doctorId")
        Resource.Success(updated)
    } catch (e: Exception) {
        log.error("Failed to end consultation id=$id — ${e.message}", e)
        Resource.Error(e.message ?: "Failed to end consultation")
    }
}
