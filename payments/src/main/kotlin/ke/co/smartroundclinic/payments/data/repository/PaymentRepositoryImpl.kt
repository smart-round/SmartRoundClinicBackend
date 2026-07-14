package ke.co.smartroundclinic.payments.data.repository

import com.mongodb.client.model.Filters
import com.mongodb.client.model.FindOneAndUpdateOptions
import com.mongodb.client.model.ReturnDocument
import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import ke.co.smartroundclinic.common.MongoDBConstants
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.payments.data.entity.PaymentEntity
import ke.co.smartroundclinic.payments.domain.repository.PaymentRepository
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import org.slf4j.LoggerFactory
import kotlin.math.ceil
import kotlin.time.Clock

class PaymentRepositoryImpl(database: MongoDatabase) : PaymentRepository {

    private val log = LoggerFactory.getLogger(PaymentRepositoryImpl::class.java)
    private val col = database.getCollection<PaymentEntity>(MongoDBConstants.PAYMENTS)

    override suspend fun save(entity: PaymentEntity): Resource<PaymentEntity> = try {
        if (entity.appointmentId != null) {
            val existing = col.find(Filters.eq(PaymentEntity::appointmentId.name, entity.appointmentId)).firstOrNull()
            if (existing != null && existing.status == PaymentEntity.PaymentStatus.COMPLETED) {
                return Resource.Error("A completed payment for this appointment already exists")
            }
        }
        col.insertOne(entity)
        log.info("Payment created id=${entity.id} appointmentId=${entity.appointmentId}")
        Resource.Success(entity, "Payment initiated successfully")
    } catch (e: Exception) {
        log.error("Failed to save payment — ${e.message}", e)
        Resource.Error(e.message ?: "Failed to initiate payment")
    }

    override suspend fun linkToAppointment(paymentId: String, appointmentId: String): Resource<Unit> = try {
        col.findOneAndUpdate(
            Filters.eq(PaymentEntity::id.name, paymentId),
            Updates.combine(
                Updates.set(PaymentEntity::appointmentId.name, appointmentId),
                Updates.set(PaymentEntity::updatedAt.name, Clock.System.now().toString()),
            ),
        ) ?: return Resource.Error("Payment not found")
        log.info("Payment id=$paymentId linked to appointmentId=$appointmentId")
        Resource.Success(Unit)
    } catch (e: Exception) {
        log.error("Failed to link payment id=$paymentId to appointment id=$appointmentId — ${e.message}", e)
        Resource.Error(e.message ?: "Failed to link payment to appointment")
    }

    override suspend fun getById(id: String): Resource<PaymentEntity?> = try {
        Resource.Success(col.find(Filters.eq(PaymentEntity::id.name, id)).firstOrNull())
    } catch (e: Exception) {
        Resource.Error(e.message ?: "Failed to fetch payment")
    }

    override suspend fun getByAppointmentId(appointmentId: String): Resource<PaymentEntity?> = try {
        Resource.Success(col.find(Filters.eq(PaymentEntity::appointmentId.name, appointmentId)).firstOrNull())
    } catch (e: Exception) {
        Resource.Error(e.message ?: "Failed to fetch payment")
    }

    override suspend fun getByPatientId(patientId: String, page: Int, size: Int): Resource<Pair<List<PaymentEntity>, Long>> = try {
        val safePage = maxOf(1, page)
        val safeSize = minOf(maxOf(1, size), 100)
        val filter = Filters.eq(PaymentEntity::patientId.name, patientId)
        val total = col.countDocuments(filter)
        val items = col.find(filter).skip((safePage - 1) * safeSize).limit(safeSize).toList()
        Resource.Success(items to total)
    } catch (e: Exception) {
        Resource.Error(e.message ?: "Failed to fetch payments")
    }

    override suspend fun getByDoctorId(doctorId: String, page: Int, size: Int): Resource<Pair<List<PaymentEntity>, Long>> = try {
        val safePage = maxOf(1, page)
        val safeSize = minOf(maxOf(1, size), 100)
        val filter = Filters.eq(PaymentEntity::doctorId.name, doctorId)
        val total = col.countDocuments(filter)
        val items = col.find(filter).skip((safePage - 1) * safeSize).limit(safeSize).toList()
        Resource.Success(items to total)
    } catch (e: Exception) {
        Resource.Error(e.message ?: "Failed to fetch payments")
    }

    override suspend fun getAll(page: Int, size: Int, status: String?): Resource<Pair<List<PaymentEntity>, Long>> = try {
        val safePage = maxOf(1, page)
        val safeSize = minOf(maxOf(1, size), 100)
        val filter = status?.let { Filters.eq(PaymentEntity::status.name, it.uppercase()) }
        val total = if (filter != null) col.countDocuments(filter) else col.countDocuments()
        val items = (if (filter != null) col.find(filter) else col.find())
            .skip((safePage - 1) * safeSize).limit(safeSize).toList()
        Resource.Success(items to total)
    } catch (e: Exception) {
        Resource.Error(e.message ?: "Failed to fetch payments")
    }

    override suspend fun getAllByDoctorId(doctorId: String): Resource<List<PaymentEntity>> = try {
        Resource.Success(col.find(Filters.eq(PaymentEntity::doctorId.name, doctorId)).toList())
    } catch (e: Exception) {
        Resource.Error(e.message ?: "Failed to fetch payments")
    }

    override suspend fun getAllForAdmin(status: String?): Resource<List<PaymentEntity>> = try {
        val items = if (status != null)
            col.find(Filters.eq(PaymentEntity::status.name, status.uppercase())).toList()
        else
            col.find().toList()
        Resource.Success(items)
    } catch (e: Exception) {
        Resource.Error(e.message ?: "Failed to fetch payments")
    }

    override suspend fun getByTransactionRef(transactionRef: String): Resource<PaymentEntity?> = try {
        Resource.Success(col.find(Filters.eq(PaymentEntity::transactionRef.name, transactionRef)).firstOrNull())
    } catch (e: Exception) {
        Resource.Error(e.message ?: "Failed to fetch payment")
    }

    override suspend fun updateFromWebhook(
        id: String,
        status: String,
        invoiceId: String?,
        mpesaReference: String?,
        charges: String?,
        netAmount: String?,
        account: String?,
        paymentMethod: String?,
    ): Resource<PaymentEntity?> = try {
        val updates = mutableListOf(
            Updates.set(PaymentEntity::status.name, status.uppercase()),
            Updates.set(PaymentEntity::updatedAt.name, Clock.System.now().toString()),
        )
        invoiceId?.let     { updates.add(Updates.set(PaymentEntity::invoiceId.name, it)) }
        mpesaReference?.let { updates.add(Updates.set(PaymentEntity::mpesaReference.name, it)) }
        charges?.let        { updates.add(Updates.set(PaymentEntity::charges.name, it)) }
        netAmount?.let      { updates.add(Updates.set(PaymentEntity::netAmount.name, it)) }
        account?.let        { updates.add(Updates.set(PaymentEntity::account.name, it)) }
        paymentMethod?.let  { updates.add(Updates.set(PaymentEntity::paymentMethod.name, it)) }
        val updated = col.findOneAndUpdate(
            Filters.eq(PaymentEntity::id.name, id),
            Updates.combine(updates),
            FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER),
        ) ?: return Resource.Error("Payment not found")
        log.info("Payment id=$id updated from webhook — status=$status invoiceId=$invoiceId mpesaRef=$mpesaReference")
        Resource.Success(updated)
    } catch (e: Exception) {
        log.error("Failed to update payment from webhook id=$id — ${e.message}", e)
        Resource.Error(e.message ?: "Failed to update payment")
    }

    override suspend fun claimDoctorCredit(paymentId: String): Resource<PaymentEntity?> = try {
        val claimed = col.findOneAndUpdate(
            Filters.and(
                Filters.eq(PaymentEntity::id.name, paymentId),
                Filters.eq(PaymentEntity::doctorCreditedAt.name, null),
            ),
            Updates.set(PaymentEntity::doctorCreditedAt.name, Clock.System.now().toString()),
            FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER),
        )
        Resource.Success(claimed)
    } catch (e: Exception) {
        log.error("Failed to claim doctor credit paymentId=$paymentId — ${e.message}", e)
        Resource.Error(e.message ?: "Failed to claim doctor credit")
    }

    override suspend fun claimCommissionCredit(paymentId: String): Resource<PaymentEntity?> = try {
        val claimed = col.findOneAndUpdate(
            Filters.and(
                Filters.eq(PaymentEntity::id.name, paymentId),
                Filters.eq(PaymentEntity::commissionCreditedAt.name, null),
            ),
            Updates.set(PaymentEntity::commissionCreditedAt.name, Clock.System.now().toString()),
            FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER),
        )
        Resource.Success(claimed)
    } catch (e: Exception) {
        log.error("Failed to claim commission credit paymentId=$paymentId — ${e.message}", e)
        Resource.Error(e.message ?: "Failed to claim commission credit")
    }

    override suspend fun releaseDoctorCredit(paymentId: String): Resource<Unit> = try {
        col.updateOne(
            Filters.eq(PaymentEntity::id.name, paymentId),
            Updates.set(PaymentEntity::doctorCreditedAt.name, null),
        )
        Resource.Success(Unit)
    } catch (e: Exception) {
        log.error("Failed to release doctor credit claim paymentId=$paymentId — ${e.message}", e)
        Resource.Error(e.message ?: "Failed to release doctor credit claim")
    }

    override suspend fun releaseCommissionCredit(paymentId: String): Resource<Unit> = try {
        col.updateOne(
            Filters.eq(PaymentEntity::id.name, paymentId),
            Updates.set(PaymentEntity::commissionCreditedAt.name, null),
        )
        Resource.Success(Unit)
    } catch (e: Exception) {
        log.error("Failed to release commission credit claim paymentId=$paymentId — ${e.message}", e)
        Resource.Error(e.message ?: "Failed to release commission credit claim")
    }

    override suspend fun getCompletedUncredited(limit: Int): Resource<List<PaymentEntity>> = try {
        val filter = Filters.and(
            Filters.eq(PaymentEntity::status.name, PaymentEntity.PaymentStatus.COMPLETED.name),
            Filters.ne(PaymentEntity::creditIneligible.name, true),
            Filters.or(
                Filters.eq(PaymentEntity::doctorCreditedAt.name, null),
                Filters.eq(PaymentEntity::commissionCreditedAt.name, null),
            ),
        )
        Resource.Success(col.find(filter).limit(limit).toList())
    } catch (e: Exception) {
        log.error("Failed to fetch completed-uncredited payments — ${e.message}", e)
        Resource.Error(e.message ?: "Failed to fetch completed-uncredited payments")
    }

    override suspend fun markCreditIneligible(paymentId: String): Resource<Unit> = try {
        col.updateOne(
            Filters.eq(PaymentEntity::id.name, paymentId),
            Updates.set(PaymentEntity::creditIneligible.name, true),
        )
        Resource.Success(Unit)
    } catch (e: Exception) {
        log.error("Failed to mark payment id=$paymentId credit-ineligible — ${e.message}", e)
        Resource.Error(e.message ?: "Failed to mark payment credit-ineligible")
    }

    override suspend fun updateStatus(id: String, status: String, transactionRef: String?, paymentMethod: String?): Resource<PaymentEntity?> = try {
        val updates = mutableListOf(
            Updates.set(PaymentEntity::status.name, status.uppercase()),
            Updates.set(PaymentEntity::updatedAt.name, Clock.System.now().toString()),
        )
        if (transactionRef != null) updates.add(Updates.set(PaymentEntity::transactionRef.name, transactionRef))
        if (paymentMethod != null) updates.add(Updates.set(PaymentEntity::paymentMethod.name, paymentMethod))
        val updated = col.findOneAndUpdate(
            Filters.eq(PaymentEntity::id.name, id),
            Updates.combine(updates),
            FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER),
        ) ?: return Resource.Error("Payment not found")
        log.info("Payment id=$id status updated to $status")
        Resource.Success(updated, "Payment status updated")
    } catch (e: Exception) {
        log.error("Failed to update payment id=$id — ${e.message}", e)
        Resource.Error(e.message ?: "Failed to update payment status")
    }
}
