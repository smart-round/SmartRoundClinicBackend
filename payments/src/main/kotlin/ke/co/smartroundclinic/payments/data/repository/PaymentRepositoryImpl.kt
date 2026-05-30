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
        val existing = col.find(Filters.eq(PaymentEntity::appointmentId.name, entity.appointmentId)).firstOrNull()
        if (existing != null) return Resource.Error("A payment for this appointment already exists")
        col.insertOne(entity)
        log.info("Payment created id=${entity.id} appointmentId=${entity.appointmentId}")
        Resource.Success(entity, "Payment initiated successfully")
    } catch (e: Exception) {
        log.error("Failed to save payment — ${e.message}", e)
        Resource.Error(e.message ?: "Failed to initiate payment")
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
