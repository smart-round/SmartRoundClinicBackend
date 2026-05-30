package ke.co.smartroundclinic.payments.data.lookup

import com.mongodb.client.model.Filters
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import ke.co.smartroundclinic.common.MongoDBConstants
import kotlinx.coroutines.flow.firstOrNull
import org.bson.Document
import org.slf4j.LoggerFactory

data class AppointmentParticipants(
    val doctorId: String,
    val patientId: String,
    val doctorName: String,
    val patientName: String,
)

class AppointmentInfoLookup(
    schedulingDb: MongoDatabase,
    authDb: MongoDatabase,
) {
    private val log = LoggerFactory.getLogger(AppointmentInfoLookup::class.java)
    private val appointments = schedulingDb.getCollection<Document>(MongoDBConstants.APPOINTMENTS)
    private val users = authDb.getCollection<Document>(MongoDBConstants.AUTH_USER)

    suspend fun getParticipants(appointmentId: String): AppointmentParticipants? {
        val appointment = appointments.find(Filters.eq("id", appointmentId)).firstOrNull()
        if (appointment == null) {
            log.warn("AppointmentInfoLookup: appointment not found id=$appointmentId")
            return null
        }
        val doctorId = appointment.getString("doctorId") ?: return null
        val patientId = appointment.getString("patientId") ?: return null

        val doctorName = users.find(Filters.eq("id", doctorId)).firstOrNull()
            ?.getString("fullName") ?: "Unknown Doctor"
        val patientName = users.find(Filters.eq("id", patientId)).firstOrNull()
            ?.getString("fullName") ?: "Unknown Patient"

        return AppointmentParticipants(
            doctorId = doctorId,
            patientId = patientId,
            doctorName = doctorName,
            patientName = patientName,
        )
    }
}
