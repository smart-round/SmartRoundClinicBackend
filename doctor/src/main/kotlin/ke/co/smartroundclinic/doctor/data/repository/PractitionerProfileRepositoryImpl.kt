package ke.co.smartroundclinic.doctor.data.repository

import com.mongodb.client.model.Filters
import com.mongodb.client.model.FindOneAndUpdateOptions
import com.mongodb.client.model.ReturnDocument
import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import ke.co.smartroundclinic.common.MongoDBConstants
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.doctor.data.entity.PractitionerProfileEntity
import ke.co.smartroundclinic.doctor.domain.model.PractitionerProfileUpdate
import ke.co.smartroundclinic.doctor.domain.repository.PractitionerProfileRepository
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import org.bson.conversions.Bson
import org.slf4j.LoggerFactory

class PractitionerProfileRepositoryImpl(database: MongoDatabase) : PractitionerProfileRepository {

    private val log = LoggerFactory.getLogger(PractitionerProfileRepositoryImpl::class.java)
    private val col = database.getCollection<PractitionerProfileEntity>(MongoDBConstants.DOCTOR_PROFILES)

    override suspend fun create(entity: PractitionerProfileEntity): Resource<PractitionerProfileEntity> = try {
        val existing = col.find(Filters.eq(PractitionerProfileEntity::doctorId.name, entity.doctorId)).firstOrNull()
        if (existing != null) {
            log.warn("Profile already exists for doctorId=${entity.doctorId}")
            return Resource.Error("A profile already exists for this doctor")
        }
        col.insertOne(entity)
        log.info("Created profile for doctorId=${entity.doctorId}")
        Resource.Success(entity, "Profile created successfully")
    } catch (e: Exception) {
        log.error("Failed to create profile — ${e.message}", e)
        Resource.Error(e.message ?: "Failed to create profile")
    }

    override suspend fun update(
        doctorId: String,
        update: PractitionerProfileUpdate,
    ): Resource<PractitionerProfileEntity?> = try {
        val fields = mutableListOf<Bson>()
        update.kmpdcRegNumber?.let     { fields.add(Updates.set(PractitionerProfileEntity::kmpdcRegNumber.name, it)) }
        update.title?.let              { fields.add(Updates.set(PractitionerProfileEntity::title.name, it)) }
        update.bio?.let                { fields.add(Updates.set(PractitionerProfileEntity::bio.name, it)) }
        update.yearsOfExperience?.let  { fields.add(Updates.set(PractitionerProfileEntity::yearsOfExperience.name, it)) }
        update.languages?.let          { fields.add(Updates.set(PractitionerProfileEntity::languages.name, it)) }
        update.facilityName?.let       { fields.add(Updates.set(PractitionerProfileEntity::facilityName.name, it)) }
        fields.add(Updates.set(PractitionerProfileEntity::updatedAt.name, System.currentTimeMillis()))

        val updated = col.findOneAndUpdate(
            Filters.eq(PractitionerProfileEntity::doctorId.name, doctorId),
            Updates.combine(fields),
            FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER),
        )
        if (updated == null) log.warn("No profile found to update for doctorId=$doctorId")
        else log.info("Updated profile for doctorId=$doctorId")
        Resource.Success(updated)
    } catch (e: Exception) {
        log.error("Failed to update profile — ${e.message}", e)
        Resource.Error(e.message ?: "Failed to update profile")
    }

    override suspend fun getByDoctorId(doctorId: String): Resource<PractitionerProfileEntity?> = try {
        Resource.Success(col.find(Filters.eq(PractitionerProfileEntity::doctorId.name, doctorId)).firstOrNull())
    } catch (e: Exception) {
        Resource.Error(e.message ?: "Failed to fetch profile")
    }

    override suspend fun getById(id: String): Resource<PractitionerProfileEntity?> = try {
        Resource.Success(col.find(Filters.eq(PractitionerProfileEntity::id.name, id)).firstOrNull())
    } catch (e: Exception) {
        Resource.Error(e.message ?: "Failed to fetch profile")
    }

    override suspend fun getAll(
        page: Int,
        size: Int,
    ): Resource<Pair<List<PractitionerProfileEntity>, Long>> = try {
        val safePage = maxOf(1, page)
        val safeSize = minOf(maxOf(1, size), 100)
        val total = col.countDocuments()
        val items = col.find()
            .skip((safePage - 1) * safeSize)
            .limit(safeSize)
            .toList()
        Resource.Success(items to total)
    } catch (e: Exception) {
        Resource.Error(e.message ?: "Failed to fetch profiles")
    }

}