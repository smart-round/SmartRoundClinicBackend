package ke.co.smartroundclinic.payments.data.repository

import com.mongodb.client.model.Filters
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import ke.co.smartroundclinic.common.MongoDBConstants
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.payments.data.entity.PaymentLogEntity
import ke.co.smartroundclinic.payments.domain.repository.PaymentLogRepository
import kotlinx.coroutines.flow.toList
import org.slf4j.LoggerFactory

class PaymentLogRepositoryImpl(database: MongoDatabase) : PaymentLogRepository {

    private val log = LoggerFactory.getLogger(PaymentLogRepositoryImpl::class.java)
    private val col = database.getCollection<PaymentLogEntity>(MongoDBConstants.PAYMENT_LOGS)

    override suspend fun log(entity: PaymentLogEntity): Resource<PaymentLogEntity> = try {
        col.insertOne(entity)
        log.info("PaymentLog saved invoiceId=${entity.invoiceId} state=${entity.state} paymentEntityId=${entity.paymentEntityId}")
        Resource.Success(entity)
    } catch (e: Exception) {
        log.error("Failed to save payment log — ${e.message}", e)
        Resource.Error(e.message ?: "Failed to save payment log")
    }

    override suspend fun getByInvoiceId(invoiceId: String): Resource<List<PaymentLogEntity>> = try {
        Resource.Success(col.find(Filters.eq(PaymentLogEntity::invoiceId.name, invoiceId)).toList())
    } catch (e: Exception) {
        Resource.Error(e.message ?: "Failed to fetch payment logs")
    }

    override suspend fun getByAppointmentId(appointmentId: String): Resource<List<PaymentLogEntity>> = try {
        Resource.Success(col.find(Filters.eq(PaymentLogEntity::appointmentId.name, appointmentId)).toList())
    } catch (e: Exception) {
        Resource.Error(e.message ?: "Failed to fetch payment logs")
    }
}
