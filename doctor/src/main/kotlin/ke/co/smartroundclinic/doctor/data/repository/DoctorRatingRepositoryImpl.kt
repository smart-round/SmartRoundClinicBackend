package ke.co.smartroundclinic.doctor.data.repository

import com.mongodb.client.model.Filters
import com.mongodb.client.model.FindOneAndUpdateOptions
import com.mongodb.client.model.ReturnDocument
import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import ke.co.smartroundclinic.common.MongoDBConstants
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.doctor.data.entity.DoctorRatingEntity
import ke.co.smartroundclinic.doctor.data.entity.PractitionerProfileEntity
import ke.co.smartroundclinic.doctor.domain.repository.DoctorRatingRepository
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import org.bson.Document
import org.bson.conversions.Bson
import org.slf4j.LoggerFactory
import kotlin.time.Clock

class DoctorRatingRepositoryImpl(
    database: MongoDatabase,
    schedulingDb: MongoDatabase,
) : DoctorRatingRepository {

    private val log = LoggerFactory.getLogger(DoctorRatingRepositoryImpl::class.java)
    private val col = database.getCollection<DoctorRatingEntity>(MongoDBConstants.DOCTOR_RATINGS)
    private val profileCol = database.getCollection<PractitionerProfileEntity>(MongoDBConstants.DOCTOR_PROFILES)
    private val appointmentsCol = schedulingDb.getCollection<Document>(MongoDBConstants.APPOINTMENTS)

    override suspend fun add(entity: DoctorRatingEntity): Resource<DoctorRatingEntity> = try {
        // Validate the appointment exists and belongs to this patient + doctor
        val appointment = appointmentsCol
            .find(Filters.eq("id", entity.appointmentId))
            .firstOrNull()
            ?: return Resource.Error("Appointment not found")

        if (appointment.getString("patientId") != entity.patientId)
            return Resource.Error("This appointment does not belong to you")
        if (appointment.getString("doctorId") != entity.doctorId)
            return Resource.Error("This appointment is not with the specified doctor")
        if (appointment.getString("status") != "COMPLETED")
            return Resource.Error("You can only rate a doctor after a completed appointment")

        // Enforce one rating per appointment
        val existing = col.find(Filters.eq(DoctorRatingEntity::appointmentId.name, entity.appointmentId)).firstOrNull()
        if (existing != null) {
            log.warn("Appointment ${entity.appointmentId} already has a rating")
            return Resource.Error("You have already rated this appointment")
        }

        col.insertOne(entity)
        log.info("Rating added for doctorId=${entity.doctorId} by patientId=${entity.patientId}")
        updateProfileStats(entity.doctorId)
        Resource.Success(entity, "Rating submitted successfully")
    } catch (e: Exception) {
        log.error("Failed to add rating for doctorId=${entity.doctorId} — ${e.message}", e)
        Resource.Error(e.message ?: "Failed to submit rating")
    }

    override suspend fun update(
        id: String,
        patientId: String,
        rating: Int?,
        comment: String?,
    ): Resource<DoctorRatingEntity?> = try {
        val fields = mutableListOf<Bson>()
        rating?.let { fields.add(Updates.set(DoctorRatingEntity::rating.name, it)) }
        comment?.let { fields.add(Updates.set(DoctorRatingEntity::comment.name, it)) }
        fields.add(Updates.set(DoctorRatingEntity::updatedAt.name, Clock.System.now().toString()))

        val updated = col.findOneAndUpdate(
            Filters.and(
                Filters.eq(DoctorRatingEntity::id.name, id),
                Filters.eq(DoctorRatingEntity::patientId.name, patientId),
            ),
            Updates.combine(fields),
            FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER),
        )
        if (updated == null) log.warn("No rating found id=$id for patientId=$patientId")
        else {
            log.info("Rating id=$id updated by patientId=$patientId")
            updateProfileStats(updated.doctorId)
        }
        Resource.Success(updated)
    } catch (e: Exception) {
        log.error("Failed to update rating id=$id — ${e.message}", e)
        Resource.Error(e.message ?: "Failed to update rating")
    }

    override suspend fun delete(id: String, patientId: String): Resource<Boolean> = try {
        val existing = col.find(
            Filters.and(
                Filters.eq(DoctorRatingEntity::id.name, id),
                Filters.eq(DoctorRatingEntity::patientId.name, patientId),
            )
        ).firstOrNull()
        val result = col.deleteOne(
            Filters.and(
                Filters.eq(DoctorRatingEntity::id.name, id),
                Filters.eq(DoctorRatingEntity::patientId.name, patientId),
            )
        )
        val deleted = result.deletedCount > 0
        if (deleted) {
            log.info("Rating id=$id deleted by patientId=$patientId")
            existing?.doctorId?.let { updateProfileStats(it) }
        } else {
            log.warn("No rating found to delete id=$id for patientId=$patientId")
        }
        Resource.Success(deleted)
    } catch (e: Exception) {
        log.error("Failed to delete rating id=$id — ${e.message}", e)
        Resource.Error(e.message ?: "Failed to delete rating")
    }

    override suspend fun getById(id: String): Resource<DoctorRatingEntity?> = try {
        Resource.Success(col.find(Filters.eq(DoctorRatingEntity::id.name, id)).firstOrNull())
    } catch (e: Exception) {
        Resource.Error(e.message ?: "Failed to fetch rating")
    }

    override suspend fun getByAppointmentId(appointmentId: String): Resource<DoctorRatingEntity?> = try {
        Resource.Success(col.find(Filters.eq(DoctorRatingEntity::appointmentId.name, appointmentId)).firstOrNull())
    } catch (e: Exception) {
        Resource.Error(e.message ?: "Failed to fetch rating")
    }

    override suspend fun getByDoctorId(
        doctorId: String,
        page: Int,
        size: Int,
    ): Resource<Pair<List<DoctorRatingEntity>, Long>> = try {
        val safePage = maxOf(1, page)
        val safeSize = minOf(maxOf(1, size), 100)
        val filter = Filters.eq(DoctorRatingEntity::doctorId.name, doctorId)
        val total = col.countDocuments(filter)
        val items = col.find(filter)
            .skip((safePage - 1) * safeSize)
            .limit(safeSize)
            .toList()
        Resource.Success(items to total)
    } catch (e: Exception) {
        Resource.Error(e.message ?: "Failed to fetch ratings")
    }

    override suspend fun getAverageRating(doctorId: String): Resource<Pair<Double, Int>> = try {
        val ratings = col.find(Filters.eq(DoctorRatingEntity::doctorId.name, doctorId)).toList()
        val total = ratings.size
        val average = if (total == 0) 0.0 else ratings.sumOf { it.rating }.toDouble() / total
        Resource.Success(average to total)
    } catch (e: Exception) {
        Resource.Error(e.message ?: "Failed to compute average rating")
    }

    private suspend fun updateProfileStats(doctorId: String) {
        try {
            val ratings = col.find(Filters.eq(DoctorRatingEntity::doctorId.name, doctorId)).toList()
            val total = ratings.size
            val average = if (total == 0) 0.0 else ratings.sumOf { it.rating }.toDouble() / total
            profileCol.findOneAndUpdate(
                Filters.eq(PractitionerProfileEntity::doctorId.name, doctorId),
                Updates.combine(
                    Updates.set(PractitionerProfileEntity::averageRating.name, average),
                    Updates.set(PractitionerProfileEntity::totalReviews.name, total),
                ),
            )
        } catch (e: Exception) {
            log.error("Failed to update profile stats for doctorId=$doctorId — ${e.message}", e)
        }
    }
}
