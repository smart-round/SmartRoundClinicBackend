package ke.co.smartroundclinic.scheduling.data.lookup

import com.mongodb.client.model.Filters
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import ke.co.smartroundclinic.common.MongoDBConstants
import kotlinx.coroutines.flow.firstOrNull
import org.bson.Document
import org.slf4j.LoggerFactory

class ConsultationVideoRoomLookup(consultationDb: MongoDatabase) {

    private val log = LoggerFactory.getLogger(ConsultationVideoRoomLookup::class.java)
    private val col = consultationDb.getCollection<Document>(MongoDBConstants.CONSULTATION_SESSIONS)

    suspend fun getVideoRoomId(appointmentId: String): String? = try {
        col.find(Filters.eq("appointmentId", appointmentId)).firstOrNull()?.getString("videoRoomId")
    } catch (e: Exception) {
        log.warn("Could not fetch videoRoomId for appointmentId=$appointmentId — ${e.message}")
        null
    }
}
