package ke.co.smartroundclinic.payments.data.lookup

import com.mongodb.client.model.Filters
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import ke.co.smartroundclinic.common.MongoDBConstants
import kotlinx.coroutines.flow.firstOrNull
import org.bson.Document
import org.slf4j.LoggerFactory

data class DoctorTierInfo(
    val tierPrice: Double,
    val followUpFee: Double,
    val commissionRate: Double,
)

class DoctorTierPriceLookup(
    adminDb: MongoDatabase,
    doctorDb: MongoDatabase,
) {
    private val log = LoggerFactory.getLogger(DoctorTierPriceLookup::class.java)
    private val serviceTiersCol = adminDb.getCollection<Document>(MongoDBConstants.ADMIN_SERVICE_TIERS)
    private val specialitiesCol = adminDb.getCollection<Document>(MongoDBConstants.ADMIN_SPECIALITIES)
    private val specializationsCol = doctorDb.getCollection<Document>(MongoDBConstants.DOCTOR_SPECIALIZATIONS)
    private val commissionRatesCol = adminDb.getCollection<Document>(MongoDBConstants.ADMIN_COMMISSION_RATES)

    // doctorId → doctor_specializations → admin_specialities → admin_service_tiers → tierPrice
    suspend fun getTierInfo(doctorId: String): DoctorTierInfo? {
        val specializationDoc = try {
            specializationsCol.find(Filters.eq("doctorId", doctorId)).firstOrNull()
        } catch (e: Exception) {
            log.warn("Could not fetch specialization for doctor $doctorId — ${e.message}")
            null
        } ?: run {
            log.warn("No specialization found for doctor $doctorId")
            return null
        }

        val specializationId = specializationDoc.getString("specializationId") ?: run {
            log.warn("Specialization record for doctor $doctorId has no specializationId")
            return null
        }

        val specialityDoc = try {
            specialitiesCol.find(Filters.eq("id", specializationId)).firstOrNull()
        } catch (e: Exception) {
            log.warn("Could not fetch speciality $specializationId — ${e.message}")
            null
        } ?: run {
            log.warn("Speciality $specializationId not found")
            return null
        }

        val serviceTierId = specialityDoc.getString("serviceTierId") ?: run {
            log.warn("Speciality $specializationId has no serviceTierId")
            return null
        }

        val tierDoc = try {
            serviceTiersCol.find(Filters.eq("id", serviceTierId)).firstOrNull()
        } catch (e: Exception) {
            log.warn("Could not fetch service tier $serviceTierId — ${e.message}")
            null
        } ?: run {
            log.warn("Service tier $serviceTierId not found")
            return null
        }

        val tierPrice = tierDoc.getDouble("tierPrice") ?: run {
            log.warn("Service tier $serviceTierId has no tierPrice")
            return null
        }

        val followUpFee = tierDoc.getDouble("followUpFee") ?: 0.0

        val commissionRate = try {
            commissionRatesCol.find().firstOrNull()?.getDouble("commissionRate") ?: 0.0
        } catch (e: Exception) {
            log.warn("Could not fetch commission rate — ${e.message}")
            0.0
        }

        return DoctorTierInfo(tierPrice = tierPrice, followUpFee = followUpFee, commissionRate = commissionRate)
    }
}
