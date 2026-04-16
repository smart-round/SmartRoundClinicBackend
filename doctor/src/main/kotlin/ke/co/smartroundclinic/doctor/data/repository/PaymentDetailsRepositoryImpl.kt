package ke.co.smartroundclinic.doctor.data.repository

import com.mongodb.client.model.Filters
import com.mongodb.client.model.FindOneAndUpdateOptions
import com.mongodb.client.model.ReturnDocument
import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import ke.co.smartroundclinic.common.MongoDBConstants
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.doctor.data.entity.PractitionerPaymentDetailsEntity
import ke.co.smartroundclinic.doctor.domain.repository.PaymentDetailsRepository
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import org.bson.conversions.Bson
import org.slf4j.LoggerFactory

class PaymentDetailsRepositoryImpl(database: MongoDatabase) : PaymentDetailsRepository {

    private val log = LoggerFactory.getLogger(PaymentDetailsRepositoryImpl::class.java)
    private val col = database.getCollection<PractitionerPaymentDetailsEntity>(MongoDBConstants.DOCTOR_PAYMENT_DETAILS)

    override suspend fun addPaymentDetails(paymentDetails: PractitionerPaymentDetailsEntity): Resource<Nothing> = try {
        val existing = col.find(Filters.eq(PractitionerPaymentDetailsEntity::doctorId.name, paymentDetails.doctorId)).firstOrNull()
        if (existing != null) {
            log.warn("Payment details already exist for doctorId=${paymentDetails.doctorId}")
            return Resource.Error("Payment details already exist for this doctor")
        }
        col.insertOne(paymentDetails)
        log.info("Payment details added for doctorId=${paymentDetails.doctorId}")
        Resource.Success(null, "Payment details added successfully")
    } catch (e: Exception) {
        log.error("Failed to add payment details for doctorId=${paymentDetails.doctorId} — ${e.message}", e)
        Resource.Error(e.message ?: "Failed to add payment details")
    }

    override suspend fun getPaymentDetails(doctorId: String): Resource<PractitionerPaymentDetailsEntity?> = try {
        Resource.Success(col.find(Filters.eq(PractitionerPaymentDetailsEntity::doctorId.name, doctorId)).firstOrNull())
    } catch (e: Exception) {
        log.error("Failed to fetch payment details for doctorId=$doctorId — ${e.message}", e)
        Resource.Error(e.message ?: "Failed to fetch payment details")
    }

    override suspend fun updatePaymentDetails(
        accountName: String?,
        accountNumber: String?,
        bankCode: String?,
        branchCode: String?,
        bankName: String?,
        branchName: String?,
        doctorId: String,
    ): Resource<PractitionerPaymentDetailsEntity?> = try {
        val fields = mutableListOf<Bson>()
        accountName?.let   { fields.add(Updates.set(PractitionerPaymentDetailsEntity::accountName.name, it)) }
        accountNumber?.let { fields.add(Updates.set(PractitionerPaymentDetailsEntity::accountNumber.name, it)) }
        bankCode?.let      { fields.add(Updates.set(PractitionerPaymentDetailsEntity::bankCode.name, it)) }
        branchCode?.let    { fields.add(Updates.set(PractitionerPaymentDetailsEntity::branchCode.name, it)) }
        bankName?.let      { fields.add(Updates.set(PractitionerPaymentDetailsEntity::bankName.name, it)) }
        branchName?.let    { fields.add(Updates.set(PractitionerPaymentDetailsEntity::branchName.name, it)) }
        fields.add(Updates.set(PractitionerPaymentDetailsEntity::updatedAt.name, System.currentTimeMillis().toString()))

        val updated = col.findOneAndUpdate(
            Filters.eq(PractitionerPaymentDetailsEntity::doctorId.name, doctorId),
            Updates.combine(fields),
            FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER),
        )
        if (updated == null) log.warn("No payment details found to update for doctorId=$doctorId")
        else log.info("Payment details updated for doctorId=$doctorId")
        Resource.Success(updated)
    } catch (e: Exception) {
        log.error("Failed to update payment details for doctorId=$doctorId — ${e.message}", e)
        Resource.Error(e.message ?: "Failed to update payment details")
    }

    override suspend fun deletePaymentDetails(doctorId: String): Resource<Nothing> = try {
        val result = col.deleteOne(Filters.eq(PractitionerPaymentDetailsEntity::doctorId.name, doctorId))
        if (result.deletedCount > 0) {
            log.info("Payment details deleted for doctorId=$doctorId")
            Resource.Success(null, "Payment details deleted successfully")
        } else {
            log.warn("No payment details found to delete for doctorId=$doctorId")
            Resource.Error("Payment details not found")
        }
    } catch (e: Exception) {
        log.error("Failed to delete payment details for doctorId=$doctorId — ${e.message}", e)
        Resource.Error(e.message ?: "Failed to delete payment details")
    }

    override suspend fun getAllPaymentDetails(page: Int, size: Int): Resource<Pair<List<PractitionerPaymentDetailsEntity>, Long>> = try {
        val safePage = maxOf(1, page)
        val safeSize = minOf(maxOf(1, size), 100)
        val total = col.countDocuments()
        val items = col.find()
            .skip((safePage - 1) * safeSize)
            .limit(safeSize)
            .toList()
        Resource.Success(items to total)
    } catch (e: Exception) {
        log.error("Failed to fetch all payment details — ${e.message}", e)
        Resource.Error(e.message ?: "Failed to fetch payment details")
    }

    override suspend fun getPaymentDetailsById(id: String): Resource<PractitionerPaymentDetailsEntity?> = try {
        Resource.Success(col.find(Filters.eq(PractitionerPaymentDetailsEntity::id.name, id)).firstOrNull())
    } catch (e: Exception) {
        log.error("Failed to fetch payment details id=$id — ${e.message}", e)
        Resource.Error(e.message ?: "Failed to fetch payment details")
    }
}
