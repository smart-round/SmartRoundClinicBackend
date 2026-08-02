package ke.co.smartroundclinic.referral.data.lookup

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
    val fullName: String? = null,
    val profilePicture: String? = null,
)

data class DoctorDisplayInfo(val name: String?, val profilePicture: String?)

/** Resolves a doctorId to its current display name/photo — used to snapshot the referring doctor's info onto a new Referral at creation time. */
class DoctorDisplayLookup(authDb: MongoDatabase) {
    private val log = LoggerFactory.getLogger(DoctorDisplayLookup::class.java)
    private val users = authDb.getCollection<UserDoc>(MongoDBConstants.AUTH_USER)

    suspend fun lookup(doctorId: String): DoctorDisplayInfo = try {
        users.find(Filters.eq("id", doctorId)).firstOrNull()
            ?.let { DoctorDisplayInfo(it.fullName, it.profilePicture) }
            ?: DoctorDisplayInfo(null, null)
    } catch (e: Exception) {
        log.warn("Could not resolve doctor display info for doctorId=$doctorId — ${e.message}")
        DoctorDisplayInfo(null, null)
    }

    suspend fun bulkLookup(doctorIds: Set<String>): Map<String, DoctorDisplayInfo> = try {
        if (doctorIds.isEmpty()) return emptyMap()
        users.find(Filters.`in`("id", doctorIds)).toList()
            .associate { it.id to DoctorDisplayInfo(it.fullName, it.profilePicture) }
    } catch (e: Exception) {
        log.warn("Could not bulk-resolve doctor display info — ${e.message}")
        emptyMap()
    }
}
