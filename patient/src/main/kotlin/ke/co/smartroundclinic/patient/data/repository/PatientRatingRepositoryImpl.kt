package ke.co.smartroundclinic.patient.data.repository

import com.mongodb.client.model.Filters
import com.mongodb.client.model.FindOneAndUpdateOptions
import com.mongodb.client.model.ReturnDocument
import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import ke.co.smartroundclinic.common.MongoDBConstants
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.patient.data.entity.PatientRatingEntity
import ke.co.smartroundclinic.patient.data.entity.PersonalInformationEntity
import ke.co.smartroundclinic.patient.domain.repository.PatientRatingRepository
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import org.bson.Document
import org.bson.conversions.Bson
import org.slf4j.LoggerFactory
import kotlin.time.Clock

class PatientRatingRepositoryImpl(
    database: MongoDatabase,
    schedulingDb: MongoDatabase,
) : PatientRatingRepository {

    private val log = LoggerFactory.getLogger(PatientRatingRepositoryImpl::class.java)
    private val col = database.getCollection<PatientRatingEntity>(MongoDBConstants.PATIENT_RATINGS)
    private val profileCol = database.getCollection<PersonalInformationEntity>(MongoDBConstants.PATIENT_PERSONAL_INFORMATION)
    private val appointmentsCol = schedulingDb.getCollection<Document>(MongoDBConstants.APPOINTMENTS)

    override suspend fun add(entity: PatientRatingEntity): Resource<PatientRatingEntity> = try {
        // Validate the appointment exists and belongs to this doctor + patient
        val appointment = appointmentsCol
            .find(Filters.eq("id", entity.appointmentId))
            .firstOrNull()
            ?: return Resource.Error("Appointment not found")

        if (appointment.getString("doctorId") != entity.doctorId)
            return Resource.Error("This appointment does not belong to you")
        if (appointment.getString("patientId") != entity.patientId)
            return Resource.Error("This appointment is not with the specified patient")
        if (appointment.getString("status") != "COMPLETED")
            return Resource.Error("You can only rate a patient after a completed appointment")

        // Enforce one rating per appointment
        val existing = col.find(Filters.eq(PatientRatingEntity::appointmentId.name, entity.appointmentId)).firstOrNull()
        if (existing != null) {
            log.warn("Appointment ${entity.appointmentId} already has a rating")
            return Resource.Error("You have already rated this appointment")
        }

        col.insertOne(entity)
        log.info("Rating added for patientId=${entity.patientId} by doctorId=${entity.doctorId}")
        updateProfileStats(entity.patientId)
        Resource.Success(entity, "Rating submitted successfully")
    } catch (e: Exception) {
        log.error("Failed to add rating for patientId=${entity.patientId} — ${e.message}", e)
        Resource.Error(e.message ?: "Failed to submit rating")
    }

    override suspend fun update(
        id: String,
        doctorId: String,
        rating: Int?,
        comment: String?,
    ): Resource<PatientRatingEntity?> = try {
        val fields = mutableListOf<Bson>()
        rating?.let { fields.add(Updates.set(PatientRatingEntity::rating.name, it)) }
        comment?.let { fields.add(Updates.set(PatientRatingEntity::comment.name, it)) }
        fields.add(Updates.set(PatientRatingEntity::updatedAt.name, Clock.System.now().toString()))

        val updated = col.findOneAndUpdate(
            Filters.and(
                Filters.eq(PatientRatingEntity::id.name, id),
                Filters.eq(PatientRatingEntity::doctorId.name, doctorId),
            ),
            Updates.combine(fields),
            FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER),
        )
        if (updated == null) log.warn("No rating found id=$id for doctorId=$doctorId")
        else {
            log.info("Rating id=$id updated by doctorId=$doctorId")
            updateProfileStats(updated.patientId)
        }
        Resource.Success(updated)
    } catch (e: Exception) {
        log.error("Failed to update rating id=$id — ${e.message}", e)
        Resource.Error(e.message ?: "Failed to update rating")
    }

    override suspend fun delete(id: String, doctorId: String): Resource<Boolean> = try {
        val existing = col.find(
            Filters.and(
                Filters.eq(PatientRatingEntity::id.name, id),
                Filters.eq(PatientRatingEntity::doctorId.name, doctorId),
            )
        ).firstOrNull()
        val result = col.deleteOne(
            Filters.and(
                Filters.eq(PatientRatingEntity::id.name, id),
                Filters.eq(PatientRatingEntity::doctorId.name, doctorId),
            )
        )
        val deleted = result.deletedCount > 0
        if (deleted) {
            log.info("Rating id=$id deleted by doctorId=$doctorId")
            existing?.patientId?.let { updateProfileStats(it) }
        } else {
            log.warn("No rating found to delete id=$id for doctorId=$doctorId")
        }
        Resource.Success(deleted)
    } catch (e: Exception) {
        log.error("Failed to delete rating id=$id — ${e.message}", e)
        Resource.Error(e.message ?: "Failed to delete rating")
    }

    override suspend fun getById(id: String): Resource<PatientRatingEntity?> = try {
        Resource.Success(col.find(Filters.eq(PatientRatingEntity::id.name, id)).firstOrNull())
    } catch (e: Exception) {
        Resource.Error(e.message ?: "Failed to fetch rating")
    }

    override suspend fun getByPatientId(
        patientId: String,
        page: Int,
        size: Int,
    ): Resource<Pair<List<PatientRatingEntity>, Long>> = try {
        val safePage = maxOf(1, page)
        val safeSize = minOf(maxOf(1, size), 100)
        val filter = Filters.eq(PatientRatingEntity::patientId.name, patientId)
        val total = col.countDocuments(filter)
        val items = col.find(filter)
            .skip((safePage - 1) * safeSize)
            .limit(safeSize)
            .toList()
        Resource.Success(items to total)
    } catch (e: Exception) {
        Resource.Error(e.message ?: "Failed to fetch ratings")
    }

    override suspend fun getAverageRating(patientId: String): Resource<Pair<Double, Int>> = try {
        val ratings = col.find(Filters.eq(PatientRatingEntity::patientId.name, patientId)).toList()
        val total = ratings.size
        val average = if (total == 0) 0.0 else ratings.sumOf { it.rating }.toDouble() / total
        Resource.Success(average to total)
    } catch (e: Exception) {
        Resource.Error(e.message ?: "Failed to compute average rating")
    }

    private suspend fun updateProfileStats(patientId: String) {
        try {
            val ratings = col.find(Filters.eq(PatientRatingEntity::patientId.name, patientId)).toList()
            val total = ratings.size
            val average = if (total == 0) 0.0 else ratings.sumOf { it.rating }.toDouble() / total
            profileCol.findOneAndUpdate(
                Filters.eq(PersonalInformationEntity::patientId.name, patientId),
                Updates.combine(
                    Updates.set(PersonalInformationEntity::averageRating.name, average),
                    Updates.set(PersonalInformationEntity::totalReviews.name, total),
                ),
            )
        } catch (e: Exception) {
            log.error("Failed to update profile stats for patientId=$patientId — ${e.message}", e)
        }
    }
}
