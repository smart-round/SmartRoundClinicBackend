package ke.co.smartroundclinic.medicalrecords.data.lookup

import com.mongodb.client.model.Filters
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import ke.co.smartroundclinic.common.MongoDBConstants
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory

@Serializable
private data class UserDoc(
    val id: String,
    val fullName: String,
)

// Resolves a doctorId to the doctor's display name for medical record responses.
class DoctorNameLookup(authDb: MongoDatabase) {
    private val log = LoggerFactory.getLogger(DoctorNameLookup::class.java)
    private val users = authDb.getCollection<UserDoc>(MongoDBConstants.AUTH_USER)

    suspend fun lookup(doctorId: String): String? = try {
        users.find(Filters.eq("id", doctorId)).firstOrNull()?.fullName
    } catch (e: Exception) {
        log.warn("Could not resolve doctor name for doctorId=$doctorId — ${e.message}")
        null
    }

    suspend fun bulkLookup(doctorIds: Set<String>): Map<String, String> = try {
        if (doctorIds.isEmpty()) return emptyMap()
        users.find(Filters.`in`("id", doctorIds)).toList()
            .associate { it.id to it.fullName }
    } catch (e: Exception) {
        log.warn("Could not bulk-resolve doctor names — ${e.message}")
        emptyMap()
    }
}
