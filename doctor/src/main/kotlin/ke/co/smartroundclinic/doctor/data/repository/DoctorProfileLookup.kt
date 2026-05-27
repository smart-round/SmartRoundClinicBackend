package ke.co.smartroundclinic.doctor.data.repository

import com.mongodb.client.model.Filters
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import ke.co.smartroundclinic.common.MongoDBConstants
import ke.co.smartroundclinic.doctor.data.entity.PractitionerProfileEntity
import ke.co.smartroundclinic.doctor.data.entity.SpecializationEntity
import ke.co.smartroundclinic.doctor.presentation.dto.response.DoctorProfileInfo
import ke.co.smartroundclinic.doctor.presentation.dto.response.SpecializationWithNamesRes
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import org.bson.Document
import org.slf4j.LoggerFactory

class DoctorProfileLookup(
    doctorDb: MongoDatabase,
    authDb: MongoDatabase,
    adminDb: MongoDatabase,
) {
    private val log = LoggerFactory.getLogger(DoctorProfileLookup::class.java)
    private val profilesCol = doctorDb.getCollection<PractitionerProfileEntity>(MongoDBConstants.DOCTOR_PROFILES)
    private val usersCol = authDb.getCollection<Document>(MongoDBConstants.AUTH_USER)
    private val specializationsCol = doctorDb.getCollection<SpecializationEntity>(MongoDBConstants.DOCTOR_SPECIALIZATIONS)
    private val specialitiesCol = adminDb.getCollection<Document>(MongoDBConstants.ADMIN_SPECIALITIES)
    private val subSpecialitiesCol = adminDb.getCollection<Document>(MongoDBConstants.ADMIN_SUB_SPECIALITIES)

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

    suspend fun searchDoctorIdsByName(name: String): Set<String> = try {
        usersCol.find(Filters.regex("fullName", name, "i")).toList()
            .mapNotNull { it.getString("id") }
            .toSet()
    } catch (e: Exception) {
        log.warn("Could not search doctors by name — ${e.message}")
        emptySet()
    }

    suspend fun lookupSpecializations(doctorId: String): List<SpecializationWithNamesRes> = try {
        val specs = specializationsCol.find(Filters.eq(SpecializationEntity::doctorId.name, doctorId)).toList()
        buildSpecializationRes(specs)
    } catch (e: Exception) {
        log.warn("Could not load specializations for doctorId=$doctorId — ${e.message}")
        emptyList()
    }

    suspend fun bulkLookupSpecializations(doctorIds: Set<String>): Map<String, List<SpecializationWithNamesRes>> = try {
        if (doctorIds.isEmpty()) return emptyMap()
        val specs = specializationsCol.find(Filters.`in`(SpecializationEntity::doctorId.name, doctorIds)).toList()
        val specsByDoctor = specs.groupBy { it.doctorId }
        val result = mutableMapOf<String, List<SpecializationWithNamesRes>>()
        for ((doctorId, doctorSpecs) in specsByDoctor) {
            result[doctorId] = buildSpecializationRes(doctorSpecs)
        }
        result
    } catch (e: Exception) {
        log.warn("Could not bulk-load specializations — ${e.message}")
        emptyMap()
    }

    private suspend fun buildSpecializationRes(specs: List<SpecializationEntity>): List<SpecializationWithNamesRes> {
        val specIds = specs.map { it.specializationId }.toSet()
        val subSpecIds = specs.mapNotNull { it.subSpecializationId }.toSet()
        val specialityNames = if (specIds.isNotEmpty())
            specialitiesCol.find(Filters.`in`("id", specIds)).toList()
                .associate { it.getString("id") to (it.getString("title") ?: "") }
        else emptyMap()
        val subSpecialityNames = if (subSpecIds.isNotEmpty())
            subSpecialitiesCol.find(Filters.`in`("id", subSpecIds)).toList()
                .associate { it.getString("id") to (it.getString("title") ?: "") }
        else emptyMap()
        return specs.map { spec ->
            SpecializationWithNamesRes(
                id = spec.id,
                specializationId = spec.specializationId,
                specializationName = specialityNames[spec.specializationId] ?: spec.specializationId,
                subSpecializationId = spec.subSpecializationId,
                subSpecializationName = spec.subSpecializationId?.let { subSpecialityNames[it] },
            )
        }
    }
}
