package ke.co.smartroundclinic.payments.data.lookup

import com.mongodb.client.model.Filters
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import ke.co.smartroundclinic.common.MongoDBConstants
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory

// ── Minimal typed projections for cross-module lookups ────────────────────────

@Serializable
private data class AppointmentDoc(
    val id: String,
    val doctorId: String,
    val patientId: String,
    val serviceTierId: String? = null,
)

@Serializable
private data class UserDoc(
    val id: String,
    val fullName: String,
)

@Serializable
private data class ServiceTierDoc(
    val id: String,
    val followUpFee: Double? = null,
)

// ── Public result type ─────────────────────────────────────────────────────────

data class AppointmentParticipants(
    val doctorId: String,
    val patientId: String,
    val doctorName: String,
    val patientName: String,
    val followUpFee: Double,
)

// ── Lookup ─────────────────────────────────────────────────────────────────────

class AppointmentInfoLookup(
    schedulingDb: MongoDatabase,
    authDb: MongoDatabase,
    adminDb: MongoDatabase,
) {
    private val log = LoggerFactory.getLogger(AppointmentInfoLookup::class.java)
    private val appointments = schedulingDb.getCollection<AppointmentDoc>(MongoDBConstants.APPOINTMENTS)
    private val users = authDb.getCollection<UserDoc>(MongoDBConstants.AUTH_USER)
    private val serviceTiers = adminDb.getCollection<ServiceTierDoc>(MongoDBConstants.ADMIN_SERVICE_TIERS)

    suspend fun getParticipants(appointmentId: String): AppointmentParticipants? {
        val appointment = appointments.find(Filters.eq("id", appointmentId)).firstOrNull()
        if (appointment == null) {
            log.warn("AppointmentInfoLookup: appointment not found id=$appointmentId")
            return null
        }

        val doctorName = users.find(Filters.eq("id", appointment.doctorId)).firstOrNull()
            ?.fullName ?: "Unknown Doctor"
        val patientName = users.find(Filters.eq("id", appointment.patientId)).firstOrNull()
            ?.fullName ?: "Unknown Patient"

        val followUpFee = appointment.serviceTierId?.let { tierId ->
            serviceTiers.find(Filters.eq("id", tierId)).firstOrNull()?.followUpFee
                .also { if (it == null) log.warn("followUpFee missing on service tier id=$tierId") }
        } ?: 0.0

        return AppointmentParticipants(
            doctorId = appointment.doctorId,
            patientId = appointment.patientId,
            doctorName = doctorName,
            patientName = patientName,
            followUpFee = followUpFee,
        )
    }
}
