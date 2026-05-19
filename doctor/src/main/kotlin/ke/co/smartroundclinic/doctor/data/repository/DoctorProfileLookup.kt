package ke.co.smartroundclinic.doctor.data.repository

import com.mongodb.client.model.Filters
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import ke.co.smartroundclinic.common.MongoDBConstants
import ke.co.smartroundclinic.doctor.data.entity.PractitionerProfileEntity
import ke.co.smartroundclinic.doctor.presentation.dto.response.DoctorProfileInfo
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import org.bson.Document
import org.slf4j.LoggerFactory

class DoctorProfileLookup(
    doctorDb: MongoDatabase,
    authDb: MongoDatabase,
) {
    private val log = LoggerFactory.getLogger(DoctorProfileLookup::class.java)
    private val profilesCol = doctorDb.getCollection<PractitionerProfileEntity>(MongoDBConstants.DOCTOR_PROFILES)
    private val usersCol = authDb.getCollection<Document>(MongoDBConstants.AUTH_USER)

    suspend fun lookup(doctorId: String): DoctorProfileInfo? = try {
        val userDoc = usersCol.find(Filters.eq("id", doctorId)).firstOrNull()
        val profileDoc = profilesCol.find(Filters.eq(PractitionerProfileEntity::doctorId.name, doctorId)).firstOrNull()
        userDoc?.let {
            DoctorProfileInfo(
                fullName = it.getString("fullName") ?: "",
                email = it.getString("email") ?: "",
                phoneNumber = it.getString("phoneNumber"),
                profilePicture = it.getString("profilePicture"),
                title = profileDoc?.title,
                facilityName = profileDoc?.facilityName,
                kmpdcRegNumber = profileDoc?.kmpdcRegNumber,
            )
        }
    } catch (e: Exception) {
        log.warn("Could not load doctor profile for doctorId=$doctorId — ${e.message}")
        null
    }

    suspend fun bulkLookup(doctorIds: Set<String>): Map<String, DoctorProfileInfo> = try {
        if (doctorIds.isEmpty()) return emptyMap()
        val userDocs = usersCol.find(Filters.`in`("id", doctorIds)).toList()
            .associateBy { it.getString("id") }
        val profileDocs = profilesCol.find(Filters.`in`(PractitionerProfileEntity::doctorId.name, doctorIds)).toList()
            .associateBy { it.doctorId }
        doctorIds.mapNotNull { id ->
            val userDoc = userDocs[id] ?: return@mapNotNull null
            id to DoctorProfileInfo(
                fullName = userDoc.getString("fullName") ?: "",
                email = userDoc.getString("email") ?: "",
                phoneNumber = userDoc.getString("phoneNumber"),
                profilePicture = userDoc.getString("profilePicture"),
                title = profileDocs[id]?.title,
                facilityName = profileDocs[id]?.facilityName,
                kmpdcRegNumber = profileDocs[id]?.kmpdcRegNumber,
            )
        }.toMap()
    } catch (e: Exception) {
        log.warn("Could not bulk-load doctor profiles — ${e.message}")
        emptyMap()
    }
}
