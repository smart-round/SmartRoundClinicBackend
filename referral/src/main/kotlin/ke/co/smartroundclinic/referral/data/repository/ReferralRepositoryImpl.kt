package ke.co.smartroundclinic.referral.data.repository

import com.mongodb.client.model.Filters
import com.mongodb.client.model.FindOneAndUpdateOptions
import com.mongodb.client.model.ReturnDocument
import com.mongodb.client.model.Sorts
import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import ke.co.smartroundclinic.common.MongoDBConstants
import ke.co.smartroundclinic.common.ReferralDisplayInfo
import ke.co.smartroundclinic.common.ReferralDisplayResolver
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.referral.data.entity.ReferralEntity
import ke.co.smartroundclinic.referral.data.entity.ReferralStatus
import ke.co.smartroundclinic.referral.domain.repository.ReferralRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import kotlin.time.Clock

class ReferralRepositoryImpl(database: MongoDatabase) : ReferralRepository, ReferralDisplayResolver {

    private val log = LoggerFactory.getLogger(ReferralRepositoryImpl::class.java)
    private val col = database.getCollection<ReferralEntity>(MongoDBConstants.REFERRALS)

    override suspend fun create(entity: ReferralEntity): Resource<ReferralEntity> =
        withContext(Dispatchers.IO) {
            try {
                col.insertOne(entity)
                log.info("Referral created id=${entity.id} referringDoctorId=${entity.referringDoctorId} receivingDoctorId=${entity.receivingDoctorId} patientId=${entity.patientId}")
                Resource.Success(entity, "Referral created successfully")
            } catch (e: Exception) {
                log.error("Failed to create referral — ${e.message}", e)
                Resource.Error(e.localizedMessage ?: "Failed to create referral")
            }
        }

    override suspend fun getById(id: String): Resource<ReferralEntity?> =
        withContext(Dispatchers.IO) {
            try {
                Resource.Success(col.find(Filters.eq(ReferralEntity::id.name, id)).firstOrNull())
            } catch (e: Exception) {
                log.error("Failed to fetch referral id=$id — ${e.message}", e)
                Resource.Error(e.localizedMessage ?: "Failed to fetch referral")
            }
        }

    override suspend fun getByReferringDoctor(doctorId: String): Resource<List<ReferralEntity>> =
        withContext(Dispatchers.IO) {
            try {
                val items = col.find(Filters.eq(ReferralEntity::referringDoctorId.name, doctorId))
                    .sort(Sorts.descending(ReferralEntity::createdAt.name))
                    .toList()
                Resource.Success(items)
            } catch (e: Exception) {
                log.error("Failed to fetch referrals for referringDoctorId=$doctorId — ${e.message}", e)
                Resource.Error(e.localizedMessage ?: "Failed to fetch referrals")
            }
        }

    override suspend fun getByReceivingDoctor(doctorId: String): Resource<List<ReferralEntity>> =
        withContext(Dispatchers.IO) {
            try {
                val items = col.find(Filters.eq(ReferralEntity::receivingDoctorId.name, doctorId))
                    .sort(Sorts.descending(ReferralEntity::createdAt.name))
                    .toList()
                Resource.Success(items)
            } catch (e: Exception) {
                log.error("Failed to fetch referrals for receivingDoctorId=$doctorId — ${e.message}", e)
                Resource.Error(e.localizedMessage ?: "Failed to fetch referrals")
            }
        }

    override suspend fun getPendingByPatient(patientId: String): Resource<List<ReferralEntity>> =
        withContext(Dispatchers.IO) {
            try {
                val items = col.find(
                    Filters.and(
                        Filters.eq(ReferralEntity::patientId.name, patientId),
                        Filters.eq(ReferralEntity::status.name, ReferralStatus.PENDING),
                    )
                ).sort(Sorts.descending(ReferralEntity::createdAt.name)).toList()
                Resource.Success(items)
            } catch (e: Exception) {
                log.error("Failed to fetch pending referrals for patientId=$patientId — ${e.message}", e)
                Resource.Error(e.localizedMessage ?: "Failed to fetch referrals")
            }
        }

    override suspend fun getByPatient(patientId: String): Resource<List<ReferralEntity>> =
        withContext(Dispatchers.IO) {
            try {
                val items = col.find(Filters.eq(ReferralEntity::patientId.name, patientId))
                    .sort(Sorts.descending(ReferralEntity::createdAt.name)).toList()
                Resource.Success(items)
            } catch (e: Exception) {
                log.error("Failed to fetch referral history for patientId=$patientId — ${e.message}", e)
                Resource.Error(e.localizedMessage ?: "Failed to fetch referrals")
            }
        }

    override suspend fun getActiveBySourceAppointment(appointmentId: String): Resource<ReferralEntity?> =
        withContext(Dispatchers.IO) {
            try {
                val active = col.find(
                    Filters.and(
                        Filters.eq(ReferralEntity::sourceAppointmentId.name, appointmentId),
                        Filters.ne(ReferralEntity::status.name, ReferralStatus.DECLINED),
                        Filters.or(
                            Filters.exists(ReferralEntity::resultingAppointmentId.name, false),
                            Filters.eq(ReferralEntity::resultingAppointmentId.name, null),
                        ),
                    )
                ).firstOrNull()
                Resource.Success(active)
            } catch (e: Exception) {
                log.error("Failed to check active referral for appointmentId=$appointmentId — ${e.message}", e)
                Resource.Error(e.localizedMessage ?: "Failed to check active referral")
            }
        }

    override suspend fun accept(id: String, patientId: String): Resource<ReferralEntity?> =
        respondTo(id, patientId, ReferralStatus.ACCEPTED)

    override suspend fun decline(id: String, patientId: String): Resource<ReferralEntity?> =
        respondTo(id, patientId, ReferralStatus.DECLINED)

    private suspend fun respondTo(id: String, patientId: String, newStatus: String): Resource<ReferralEntity?> =
        withContext(Dispatchers.IO) {
            try {
                val updated = col.findOneAndUpdate(
                    Filters.and(
                        Filters.eq(ReferralEntity::id.name, id),
                        Filters.eq(ReferralEntity::patientId.name, patientId),
                        Filters.eq(ReferralEntity::status.name, ReferralStatus.PENDING),
                    ),
                    Updates.combine(
                        Updates.set(ReferralEntity::status.name, newStatus),
                        Updates.set(ReferralEntity::respondedAt.name, Clock.System.now().toString()),
                    ),
                    FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER),
                )
                if (updated == null) {
                    log.warn("No pending referral found id=$id for patientId=$patientId")
                    Resource.Error("Referral not found or already responded to")
                } else {
                    log.info("Referral id=$id -> $newStatus by patientId=$patientId")
                    Resource.Success(updated)
                }
            } catch (e: Exception) {
                log.error("Failed to respond to referral id=$id — ${e.message}", e)
                Resource.Error(e.localizedMessage ?: "Failed to respond to referral")
            }
        }

    override suspend fun linkResultingAppointment(id: String, appointmentId: String): Resource<Boolean> =
        withContext(Dispatchers.IO) {
            try {
                val result = col.updateOne(
                    Filters.eq(ReferralEntity::id.name, id),
                    Updates.set(ReferralEntity::resultingAppointmentId.name, appointmentId),
                )
                if (result.modifiedCount > 0) {
                    log.info("Linked referral id=$id to resultingAppointmentId=$appointmentId")
                } else {
                    log.error("Failed to link referral id=$id to resultingAppointmentId=$appointmentId — matchedCount=${result.matchedCount} modifiedCount=${result.modifiedCount} (no document matched)")
                }
                Resource.Success(result.modifiedCount > 0)
            } catch (e: Exception) {
                log.error("Failed to link referral id=$id to appointmentId=$appointmentId — ${e.message}", e)
                Resource.Error(e.localizedMessage ?: "Failed to link referral to appointment")
            }
        }

    override suspend fun getAllForAdmin(status: String?, page: Int, size: Int): Resource<Pair<List<ReferralEntity>, Long>> =
        withContext(Dispatchers.IO) {
            try {
                val safePage = maxOf(1, page)
                val safeSize = minOf(maxOf(1, size), 100)
                val filter = status?.let { Filters.eq(ReferralEntity::status.name, it.uppercase()) }
                val total = if (filter != null) col.countDocuments(filter) else col.countDocuments()
                val items = (if (filter != null) col.find(filter) else col.find())
                    .sort(Sorts.descending(ReferralEntity::createdAt.name))
                    .skip((safePage - 1) * safeSize)
                    .limit(safeSize)
                    .toList()
                Resource.Success(items to total)
            } catch (e: Exception) {
                log.error("Failed to fetch admin referrals — ${e.message}", e)
                Resource.Error(e.localizedMessage ?: "Failed to fetch referrals")
            }
        }

    override suspend fun getAll(): Resource<List<ReferralEntity>> =
        withContext(Dispatchers.IO) {
            try {
                Resource.Success(col.find().sort(Sorts.descending(ReferralEntity::createdAt.name)).toList())
            } catch (e: Exception) {
                log.error("Failed to fetch all referrals — ${e.message}", e)
                Resource.Error(e.localizedMessage ?: "Failed to fetch referrals")
            }
        }

    override suspend fun getReferralDisplayInfo(referralId: String): ReferralDisplayInfo? = try {
        col.find(Filters.eq(ReferralEntity::id.name, referralId)).firstOrNull()?.let {
            ReferralDisplayInfo(
                referringDoctorName = it.referringDoctorName,
                referringDoctorPicture = it.referringDoctorPicture,
                status = it.status,
            )
        }
    } catch (e: Exception) {
        log.warn("Could not resolve referral display info for referralId=$referralId — ${e.message}")
        null
    }
}
