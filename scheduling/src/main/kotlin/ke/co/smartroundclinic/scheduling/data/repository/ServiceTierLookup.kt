package ke.co.smartroundclinic.scheduling.data.repository

import com.mongodb.client.model.Filters
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import ke.co.smartroundclinic.common.MongoDBConstants
import kotlinx.coroutines.flow.firstOrNull
import org.bson.Document
import org.slf4j.LoggerFactory

sealed class ConsultationInfoResult {
    data class Success(val serviceTierId: String, val consultationDuration: Int, val gracePeriod: Int) : ConsultationInfoResult()
    data class Error(val message: String, val httpStatus: Int = 400) : ConsultationInfoResult()
}

class ServiceTierLookup(
    private val adminDb: MongoDatabase,
    private val doctorDb: MongoDatabase,
) {
    private val log = LoggerFactory.getLogger(ServiceTierLookup::class.java)
    private val serviceTiersCol = adminDb.getCollection<Document>(MongoDBConstants.ADMIN_SERVICE_TIERS)
    private val specialitiesCol = adminDb.getCollection<Document>(MongoDBConstants.ADMIN_SPECIALITIES)
    private val specializationsCol = doctorDb.getCollection<Document>(MongoDBConstants.DOCTOR_SPECIALIZATIONS)

    // Full chain: doctorId → primary specializationId → speciality → serviceTier → consultationDuration
    suspend fun getConsultationInfoForDoctor(doctorId: String): ConsultationInfoResult {
        val specializationDoc = try {
            specializationsCol.find(Filters.eq("doctorId", doctorId)).firstOrNull()
        } catch (e: Exception) {
            log.warn("Could not fetch specialization for doctor $doctorId — ${e.message}")
            null
        } ?: return ConsultationInfoResult.Error(
            "Doctor is not yet configured for appointments. Please set up your specialisation first.",
        )

        val specializationId = specializationDoc.getString("specializationId")
            ?: return ConsultationInfoResult.Error("Doctor specialisation record is incomplete.")

        val specialityDoc = try {
            specialitiesCol.find(Filters.eq("id", specializationId)).firstOrNull()
        } catch (e: Exception) {
            log.warn("Could not fetch speciality $specializationId — ${e.message}")
            null
        } ?: return ConsultationInfoResult.Error("Speciality not found.")

        val serviceTierId = specialityDoc.getString("serviceTierId")
            ?: return ConsultationInfoResult.Error(
                "This speciality is not yet approved for offering appointments. Please contact the administrator.",
            )

        val tierDoc = try {
            serviceTiersCol.find(Filters.eq("id", serviceTierId)).firstOrNull()
        } catch (e: Exception) {
            log.warn("Could not fetch service tier $serviceTierId — ${e.message}")
            null
        } ?: return ConsultationInfoResult.Error("Service tier '$serviceTierId' not found.")

        val consultationDuration = tierDoc.getLong("consultationDuration")?.let { (it / 60_000).toInt() }
            ?: return ConsultationInfoResult.Error("Service tier is missing consultationDuration.")

        val gracePeriod = tierDoc.getLong("gracePeriod")?.let { (it / 60_000).toInt() } ?: 0

        return ConsultationInfoResult.Success(serviceTierId, consultationDuration, gracePeriod)
    }

    // Direct lookup by serviceTierId (for calendar and slot queries where tier is already known)
    suspend fun getConsultationDuration(serviceTierId: String): Int? = try {
        serviceTiersCol.find(Filters.eq("id", serviceTierId)).firstOrNull()
            ?.getLong("consultationDuration")?.let { (it / 60_000).toInt() }
    } catch (e: Exception) {
        log.warn("Could not fetch service tier $serviceTierId — ${e.message}")
        null
    }
}
