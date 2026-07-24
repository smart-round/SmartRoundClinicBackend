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
    val status: String = "",
    val date: String = "",
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
    val tierPrice: Double? = null,
)

@Serializable
private data class CommissionRateDoc(
    val id: String,
    val commissionRate: Double = 0.0,
)

// ── Public result types ────────────────────────────────────────────────────────

data class RebookingAppointmentInfo(
    val doctorId: String,
    val patientId: String,
    val status: String,
    val date: String,
    val serviceTierId: String?,
    val followUpFee: Double,
)

data class AppointmentParticipants(
    val doctorId: String,
    val patientId: String,
    val doctorName: String,
    val patientName: String,
    val followUpFee: Double,
    val tierPrice: Double,
    val commissionRate: Double,
)

data class AppointmentSummary(
    val id: String,
    val doctorId: String,
    val doctorName: String,
    val patientId: String,
    val patientName: String,
    val status: String,
    val date: String,
    val serviceTierId: String?,
    val tierPrice: Double,
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
    private val commissionRates = adminDb.getCollection<CommissionRateDoc>(MongoDBConstants.ADMIN_COMMISSION_RATES)

    suspend fun getCommissionRate(): Double =
        commissionRates.find().firstOrNull()?.commissionRate ?: 0.0

    suspend fun getStatus(appointmentId: String): String? =
        appointments.find(Filters.eq("id", appointmentId)).firstOrNull()?.status

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
            serviceTiers.find(Filters.eq("id", tierId)).firstOrNull()
                .also { if (it == null) log.warn("followUpFee missing on service tier id=$tierId") }
        }

        val commissionRate = commissionRates.find().firstOrNull()?.commissionRate ?: 0.0

        return AppointmentParticipants(
            doctorId = appointment.doctorId,
            patientId = appointment.patientId,
            doctorName = doctorName,
            patientName = patientName,
            followUpFee = followUpFee?.followUpFee ?: 0.0,
            tierPrice = followUpFee?.tierPrice ?: 0.0,
            commissionRate = commissionRate,
        )
    }

    /** Full-precision appointment lookup by id — used e.g. to resolve a wallet-transaction narrative
     *  suffix match down to a displayable appointment for admin support tooling. */
    suspend fun getSummary(appointmentId: String): AppointmentSummary? {
        val appointment = appointments.find(Filters.eq("id", appointmentId)).firstOrNull() ?: return null

        val doctorName = users.find(Filters.eq("id", appointment.doctorId)).firstOrNull()
            ?.fullName ?: "Unknown Doctor"
        val patientName = users.find(Filters.eq("id", appointment.patientId)).firstOrNull()
            ?.fullName ?: "Unknown Patient"
        val tier = appointment.serviceTierId?.let { tierId ->
            serviceTiers.find(Filters.eq("id", tierId)).firstOrNull()
        }

        return AppointmentSummary(
            id = appointment.id,
            doctorId = appointment.doctorId,
            doctorName = doctorName,
            patientId = appointment.patientId,
            patientName = patientName,
            status = appointment.status,
            date = appointment.date,
            serviceTierId = appointment.serviceTierId,
            tierPrice = tier?.tierPrice ?: 0.0,
            followUpFee = tier?.followUpFee ?: 0.0,
        )
    }

    /** Finds an appointment whose id ends with [suffix] — the 6-hex-char form IntaSend wallet-transfer
     *  narratives embed as "(APT-xxxxxx)" (see CreditDoctorEarningsUseCase). Not a unique key by
     *  construction, but collisions are vanishingly unlikely for a 6-hex-char suffix of a Mongo ObjectId. */
    suspend fun findIdBySuffix(suffix: String): String? =
        appointments.find(Filters.regex("id", "$suffix$")).firstOrNull()?.id

    suspend fun getForRebooking(appointmentId: String): RebookingAppointmentInfo? {
        val appointment = appointments.find(Filters.eq("id", appointmentId)).firstOrNull()
        if (appointment == null) {
            log.warn("AppointmentInfoLookup.getForRebooking: appointment not found id=$appointmentId")
            return null
        }
        val followUpFee = appointment.serviceTierId?.let { tierId ->
            serviceTiers.find(Filters.eq("id", tierId)).firstOrNull()?.followUpFee
        } ?: 0.0
        return RebookingAppointmentInfo(
            doctorId = appointment.doctorId,
            patientId = appointment.patientId,
            status = appointment.status,
            date = appointment.date,
            serviceTierId = appointment.serviceTierId,
            followUpFee = followUpFee,
        )
    }
}
