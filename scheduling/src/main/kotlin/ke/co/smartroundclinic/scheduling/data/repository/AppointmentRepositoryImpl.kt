package ke.co.smartroundclinic.scheduling.data.repository

import com.mongodb.client.model.Aggregates
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.MongoClient
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import ke.co.smartroundclinic.common.MongoDBConstants
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.scheduling.data.entity.AppointmentEntity
import ke.co.smartroundclinic.scheduling.domain.repository.AppointmentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import org.bson.conversions.Bson
import org.slf4j.LoggerFactory
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class AppointmentRepositoryImpl(
    private val client: MongoClient,
    database: MongoDatabase,
) : AppointmentRepository {

    private val log = LoggerFactory.getLogger(AppointmentRepositoryImpl::class.java)
    private val col = database.getCollection<AppointmentEntity>(MongoDBConstants.APPOINTMENTS)

    override suspend fun book(entity: AppointmentEntity): Resource<AppointmentEntity?> =
        withContext(Dispatchers.IO) {
            val session = client.startSession()
            try {
                session.startTransaction()
                val existing = col.find(
                    session,
                    Filters.and(
                        Filters.eq(AppointmentEntity::doctorId.name, entity.doctorId),
                        Filters.eq(AppointmentEntity::date.name, entity.date),
                        Filters.eq(AppointmentEntity::slotStart.name, entity.slotStart),
                        Filters.`in`(AppointmentEntity::status.name, "BOOKED", "CONFIRMED"),
                    )
                ).firstOrNull()
                if (existing != null) {
                    session.abortTransaction()
                    return@withContext Resource.Error("This slot has already been booked")
                }
                col.insertOne(session, entity)
                session.commitTransaction()
                log.info("Booked appointment id=${entity.id} for patientId=${entity.patientId}")
                Resource.Success(data = entity, message = "Appointment booked successfully")
            } catch (e: Exception) {
                runCatching { session.abortTransaction() }
                log.error("Failed to book appointment — ${e.message}", e)
                Resource.Error(e.localizedMessage ?: "Failed to book appointment")
            } finally {
                session.close()
            }
        }

    override suspend fun getById(id: String): Resource<AppointmentEntity?> =
        withContext(Dispatchers.IO) {
            try {
                val entity = col.find(Filters.eq(AppointmentEntity::id.name, id)).firstOrNull()
                    ?: return@withContext Resource.Error("Appointment not found")
                Resource.Success(data = entity, message = "Appointment retrieved successfully")
            } catch (e: Exception) {
                log.error("Failed to fetch appointment id=$id — ${e.message}", e)
                Resource.Error(e.localizedMessage ?: "Failed to fetch appointment")
            }
        }

    override suspend fun getAll(): Resource<List<AppointmentEntity>> =
        withContext(Dispatchers.IO) {
            try {
                Resource.Success(data = col.find().toList(), message = "Appointments retrieved successfully")
            } catch (e: Exception) {
                log.error("Failed to fetch all appointments — ${e.message}", e)
                Resource.Error(e.localizedMessage ?: "Failed to fetch appointments")
            }
        }

    override suspend fun getByDoctor(doctorId: String): Resource<List<AppointmentEntity>> =
        withContext(Dispatchers.IO) {
            try {
                val items = col.find(Filters.eq(AppointmentEntity::doctorId.name, doctorId)).toList()
                Resource.Success(data = items, message = "Appointments retrieved successfully")
            } catch (e: Exception) {
                log.error("Failed to fetch appointments for doctorId=$doctorId — ${e.message}", e)
                Resource.Error(e.localizedMessage ?: "Failed to fetch appointments")
            }
        }

    override suspend fun getByDoctorFiltered(
        doctorId: String,
        filter: String?,
        today: String,
    ): Resource<List<AppointmentEntity>> = withContext(Dispatchers.IO) {
        try {
            val doctorFilter = Filters.eq(AppointmentEntity::doctorId.name, doctorId)
            val combined = when (filter?.lowercase()) {
                "upcoming" -> Filters.and(
                    doctorFilter,
                    Filters.gte(AppointmentEntity::date.name, today),
                    Filters.`in`(AppointmentEntity::status.name, "BOOKED", "CONFIRMED"),
                )
                "past" -> Filters.and(
                    doctorFilter,
                    Filters.lt(AppointmentEntity::date.name, today),
                )
                "today" -> Filters.and(
                    doctorFilter,
                    Filters.eq(AppointmentEntity::date.name, today),
                )
                else -> doctorFilter
            }
            val items = col.find(combined).toList()
            Resource.Success(data = items, message = "Appointments retrieved successfully")
        } catch (e: Exception) {
            log.error("Failed to fetch filtered appointments for doctorId=$doctorId — ${e.message}", e)
            Resource.Error(e.localizedMessage ?: "Failed to fetch appointments")
        }
    }

    override suspend fun getByPatient(patientId: String): Resource<List<AppointmentEntity>> =
        withContext(Dispatchers.IO) {
            try {
                val items = col.find(Filters.eq(AppointmentEntity::patientId.name, patientId)).toList()
                Resource.Success(data = items, message = "Appointments retrieved successfully")
            } catch (e: Exception) {
                log.error("Failed to fetch appointments for patientId=$patientId — ${e.message}", e)
                Resource.Error(e.localizedMessage ?: "Failed to fetch appointments")
            }
        }

    override suspend fun getByDoctorAndDate(doctorId: String, date: String): Resource<List<AppointmentEntity>> =
        withContext(Dispatchers.IO) {
            try {
                val filter = Filters.and(
                    Filters.eq(AppointmentEntity::doctorId.name, doctorId),
                    Filters.eq(AppointmentEntity::date.name, date),
                )
                val items = col.find(filter).toList()
                Resource.Success(data = items, message = "Appointments retrieved successfully")
            } catch (e: Exception) {
                log.error("Failed to fetch appointments for doctorId=$doctorId date=$date — ${e.message}", e)
                Resource.Error(e.localizedMessage ?: "Failed to fetch appointments")
            }
        }

    override suspend fun getByDoctorAndDateRange(
        doctorId: String,
        from: String,
        to: String,
    ): Resource<List<AppointmentEntity>> = withContext(Dispatchers.IO) {
        try {
            val items = col.find(
                Filters.and(
                    Filters.eq(AppointmentEntity::doctorId.name, doctorId),
                    Filters.gte(AppointmentEntity::date.name, from),
                    Filters.lte(AppointmentEntity::date.name, to),
                )
            ).toList()
            Resource.Success(data = items, message = "Appointments retrieved successfully")
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to fetch appointments")
        }
    }

    override suspend fun getAllForAdmin(status: String?, page: Int, size: Int): Resource<Pair<List<AppointmentEntity>, Long>> =
        withContext(Dispatchers.IO) {
            try {
                val safePage = maxOf(1, page)
                val safeSize = minOf(maxOf(1, size), 100)
                val filter = status?.let { Filters.eq(AppointmentEntity::status.name, it.uppercase()) }
                val total = if (filter != null) col.countDocuments(filter) else col.countDocuments()
                val items = (if (filter != null) col.find(filter) else col.find())
                    .sort(com.mongodb.client.model.Sorts.descending(AppointmentEntity::bookedAt.name))
                    .skip((safePage - 1) * safeSize)
                    .limit(safeSize)
                    .toList()
                Resource.Success(data = items to total)
            } catch (e: Exception) {
                log.error("Failed to fetch admin appointments — ${e.message}", e)
                Resource.Error(e.localizedMessage ?: "Failed to fetch appointments")
            }
        }

    override fun watchByDoctorId(doctorId: String): Flow<AppointmentEntity> =
        col.watch(
            listOf(
                Aggregates.match(
                    Filters.and(
                        Filters.`in`("operationType", "insert", "update", "replace"),
                        Filters.eq("fullDocument.doctorId", doctorId),
                    )
                )
            )
        ).mapNotNull { it.fullDocument }

    override suspend fun updateStatus(
        id: String,
        status: String,
        cancellationReason: String?,
        cancelledBy: String?,
    ): Resource<AppointmentEntity?> =
        withContext(Dispatchers.IO) {
            try {
                col.find(Filters.eq(AppointmentEntity::id.name, id)).firstOrNull()
                    ?: return@withContext Resource.Error("Appointment not found")

                val updates = mutableListOf<Bson>()
                updates.add(Updates.set(AppointmentEntity::status.name, status))
                cancellationReason?.let { updates.add(Updates.set(AppointmentEntity::cancellationReason.name, it)) }
                cancelledBy?.let { updates.add(Updates.set(AppointmentEntity::cancelledBy.name, it)) }
                updates.add(Updates.set(AppointmentEntity::updatedAt.name, Clock.System.now().toString()))

                col.updateOne(Filters.eq(AppointmentEntity::id.name, id), Updates.combine(updates))
                val updated = col.find(Filters.eq(AppointmentEntity::id.name, id)).firstOrNull()
                log.info("Updated appointment id=$id to status=$status")
                Resource.Success(data = updated, message = "Appointment updated successfully")
            } catch (e: Exception) {
                log.error("Failed to update appointment id=$id — ${e.message}", e)
                Resource.Error(e.localizedMessage ?: "Failed to update appointment")
            }
        }

    override suspend fun existsConfirmedOrCompletedBetween(doctorId: String, patientId: String): Boolean =
        withContext(Dispatchers.IO) {
            try {
                col.find(
                    Filters.and(
                        Filters.eq(AppointmentEntity::doctorId.name, doctorId),
                        Filters.eq(AppointmentEntity::patientId.name, patientId),
                        Filters.`in`(AppointmentEntity::status.name, "CONFIRMED", "COMPLETED"),
                    )
                ).firstOrNull() != null
            } catch (e: Exception) {
                log.error("Failed to check appointment relationship doctorId=$doctorId patientId=$patientId — ${e.message}", e)
                false
            }
        }

    override suspend fun hasJoinableConfirmedAppointment(doctorId: String, patientId: String): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val now = kotlinx.datetime.Clock.System.now()
                col.find(
                    Filters.and(
                        Filters.eq(AppointmentEntity::doctorId.name, doctorId),
                        Filters.eq(AppointmentEntity::patientId.name, patientId),
                        Filters.eq(AppointmentEntity::status.name, "CONFIRMED"),
                    )
                ).toList().any { appointment ->
                    val slotStartInstant = parseSlotStart(appointment.date, appointment.slotStart) ?: return@any false
                    now >= slotStartInstant.minus(10.minutes) && now <= slotStartInstant.plus(24.hours)
                }
            } catch (e: Exception) {
                log.error("Failed to check joinable appointment doctorId=$doctorId patientId=$patientId — ${e.message}", e)
                false
            }
        }

    private fun parseSlotStart(date: String, slotStart: String): Instant? = runCatching {
        val (h, m) = slotStart.split(":").map { it.toInt() }
        val dateParts = date.split("-").map { it.toInt() }
        LocalDateTime(dateParts[0], dateParts[1], dateParts[2], h, m, 0, 0)
            .toInstant(TimeZone.of("Africa/Nairobi"))
    }.getOrNull()

    override suspend fun setReferralId(appointmentId: String, referralId: String): Resource<Boolean> =
        withContext(Dispatchers.IO) {
            try {
                val result = col.updateOne(
                    Filters.eq(AppointmentEntity::id.name, appointmentId),
                    Updates.combine(
                        Updates.set(AppointmentEntity::referralId.name, referralId),
                        Updates.set(AppointmentEntity::updatedAt.name, Clock.System.now().toString()),
                    ),
                )
                if (result.modifiedCount > 0) {
                    log.info("Tagged appointment id=$appointmentId with referralId=$referralId")
                } else {
                    log.error("Failed to tag appointment id=$appointmentId with referralId=$referralId — matchedCount=${result.matchedCount} modifiedCount=${result.modifiedCount} (no document matched or field already set to this value)")
                }
                Resource.Success(data = result.modifiedCount > 0)
            } catch (e: Exception) {
                log.error("Failed to tag appointment id=$appointmentId with referralId=$referralId — ${e.message}", e)
                Resource.Error(e.localizedMessage ?: "Failed to tag appointment with referral")
            }
        }

    override suspend fun getNextConfirmedAppointment(doctorId: String, patientId: String): Resource<AppointmentEntity?> =
        withContext(Dispatchers.IO) {
            try {
                val now = kotlinx.datetime.Clock.System.now()
                val next = col.find(
                    Filters.and(
                        Filters.eq(AppointmentEntity::doctorId.name, doctorId),
                        Filters.eq(AppointmentEntity::patientId.name, patientId),
                        Filters.eq(AppointmentEntity::status.name, "CONFIRMED"),
                    )
                ).toList()
                    .filter { appointment ->
                        val slotStartInstant = parseSlotStart(appointment.date, appointment.slotStart) ?: return@filter false
                        now <= slotStartInstant.plus(24.hours)
                    }
                    .minWithOrNull(compareBy({ it.date }, { it.slotStart }))
                Resource.Success(data = next, message = "Next appointment retrieved successfully")
            } catch (e: Exception) {
                log.error("Failed to fetch next appointment doctorId=$doctorId patientId=$patientId — ${e.message}", e)
                Resource.Error(e.localizedMessage ?: "Failed to fetch next appointment")
            }
        }
}
