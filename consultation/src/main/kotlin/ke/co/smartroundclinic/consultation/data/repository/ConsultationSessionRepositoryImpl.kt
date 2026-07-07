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
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.time.Clock

private val APPOINTMENT_ZONE = ZoneId.of("Africa/Nairobi")
private val SLOT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
private const val EARLY_ACCESS_SECONDS = 5 * 60L

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

        // Enforce 5-minute early-access window for upcoming appointments
        if (status == "CONFIRMED") {
            val date = appointment.getString("date")
            val slotStart = appointment.getString("slotStart")
            if (date != null && slotStart != null) {
                val slotStartEpoch = runCatching {
                    LocalDateTime.parse("$date $slotStart", SLOT_FORMATTER)
                        .atZone(APPOINTMENT_ZONE)
                        .toEpochSecond()
                }.getOrNull()
                if (slotStartEpoch != null) {
                    val nowEpoch = System.currentTimeMillis() / 1000
                    if (nowEpoch < slotStartEpoch - EARLY_ACCESS_SECONDS) {
                        return Resource.Error(
                            "Consultation is not yet available. You can join up to 5 minutes before your appointment."
                        )
                    }
                }
            }
        }

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

    override suspend fun getByVideoRoomId(videoRoomId: String): Resource<ConsultationSessionEntity?> = try {
        Resource.Success(col.find(Filters.eq(ConsultationSessionEntity::videoRoomId.name, videoRoomId)).firstOrNull())
    } catch (e: Exception) {
        Resource.Error(e.message ?: "Failed to fetch consultation by videoRoomId")
    }

    override suspend fun setVideoRoomId(id: String, videoRoomId: String): Resource<ConsultationSessionEntity?> = try {
        val updated = col.findOneAndUpdate(
            Filters.eq(ConsultationSessionEntity::id.name, id),
            Updates.combine(
                Updates.set(ConsultationSessionEntity::videoRoomId.name, videoRoomId),
                Updates.set(ConsultationSessionEntity::updatedAt.name, Clock.System.now().toString()),
            ),
            FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER),
        )
        Resource.Success(updated)
    } catch (e: Exception) {
        log.error("Failed to set videoRoomId on consultation id=$id — ${e.message}", e)
        Resource.Error(e.message ?: "Failed to update consultation")
    }

    override suspend fun setVideoRoomIdIfAbsent(id: String, videoRoomId: String): Resource<String> = try {
        val updated = col.findOneAndUpdate(
            Filters.and(
                Filters.eq(ConsultationSessionEntity::id.name, id),
                Filters.or(
                    Filters.exists(ConsultationSessionEntity::videoRoomId.name, false),
                    Filters.eq(ConsultationSessionEntity::videoRoomId.name, null),
                ),
            ),
            Updates.combine(
                Updates.set(ConsultationSessionEntity::videoRoomId.name, videoRoomId),
                Updates.set(ConsultationSessionEntity::updatedAt.name, Clock.System.now().toString()),
            ),
            FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER),
        )
        if (updated != null) {
            // This request won the race — our meeting ID is now stored
            Resource.Success(videoRoomId)
        } else {
            // Another request already stored a meeting ID — read and reuse it
            val current = col.find(Filters.eq(ConsultationSessionEntity::id.name, id)).firstOrNull()
            val existingId = current?.videoRoomId
            if (!existingId.isNullOrBlank()) Resource.Success(existingId)
            else Resource.Success(videoRoomId)
        }
    } catch (e: Exception) {
        log.error("Failed to setVideoRoomIdIfAbsent on consultation id=$id — ${e.message}", e)
        Resource.Error(e.message ?: "Failed to set videoRoomId")
    }

    override suspend fun clearVideoRoomId(id: String, completedRoomId: String?): Resource<Unit> = try {
        val updates = mutableListOf(
            Updates.unset(ConsultationSessionEntity::videoRoomId.name),
            Updates.set(ConsultationSessionEntity::updatedAt.name, Clock.System.now().toString()),
        )
        if (completedRoomId != null) {
            updates.add(Updates.set(ConsultationSessionEntity::lastVideoRoomId.name, completedRoomId))
        }
        col.updateOne(
            Filters.eq(ConsultationSessionEntity::id.name, id),
            Updates.combine(updates),
        )
        log.info("Cleared videoRoomId on consultation id=$id${if (completedRoomId != null) " (lastVideoRoomId=$completedRoomId)" else ""}")
        Resource.Success(Unit)
    } catch (e: Exception) {
        log.error("Failed to clear videoRoomId on consultation id=$id — ${e.message}", e)
        Resource.Error(e.message ?: "Failed to clear videoRoomId")
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
