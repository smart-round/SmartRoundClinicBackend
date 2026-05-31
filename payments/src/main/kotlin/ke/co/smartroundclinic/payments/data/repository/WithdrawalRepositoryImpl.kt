package ke.co.smartroundclinic.payments.data.repository

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import kotlin.time.Clock
import ke.co.smartroundclinic.common.MongoDBConstants
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.payments.data.entity.WithdrawalEntity
import ke.co.smartroundclinic.payments.domain.repository.WithdrawalRepository
import kotlinx.coroutines.flow.toList
import org.slf4j.LoggerFactory

class WithdrawalRepositoryImpl(database: MongoDatabase) : WithdrawalRepository {

    private val log = LoggerFactory.getLogger(WithdrawalRepositoryImpl::class.java)
    private val col = database.getCollection<WithdrawalEntity>(MongoDBConstants.WITHDRAWALS)

    override suspend fun save(entity: WithdrawalEntity): Resource<WithdrawalEntity> = try {
        col.insertOne(entity)
        log.info("Withdrawal saved id=${entity.id} doctorId=${entity.doctorId} amount=${entity.amount}")
        Resource.Success(entity, "Withdrawal recorded successfully")
    } catch (e: Exception) {
        log.error("Failed to save withdrawal — ${e.message}", e)
        Resource.Error(e.message ?: "Failed to record withdrawal")
    }

    override suspend fun getByDoctorId(doctorId: String): Resource<List<WithdrawalEntity>> = try {
        val results = col.find(Filters.eq(WithdrawalEntity::doctorId.name, doctorId)).toList()
        Resource.Success(results)
    } catch (e: Exception) {
        log.error("Failed to fetch withdrawals for doctorId=$doctorId — ${e.message}", e)
        Resource.Error(e.message ?: "Failed to fetch withdrawals")
    }

    override suspend fun getByDoctorIdPaginated(doctorId: String, page: Int, size: Int): Resource<Pair<List<WithdrawalEntity>, Long>> = try {
        val safePage = maxOf(1, page)
        val safeSize = minOf(maxOf(1, size), 100)
        val filter = Filters.eq(WithdrawalEntity::doctorId.name, doctorId)
        val total = col.countDocuments(filter)
        val items = col.find(filter)
            .sort(com.mongodb.client.model.Sorts.descending(WithdrawalEntity::createdAt.name))
            .skip((safePage - 1) * safeSize).limit(safeSize).toList()
        Resource.Success(items to total)
    } catch (e: Exception) {
        log.error("Failed to fetch paginated withdrawals for doctorId=$doctorId — ${e.message}", e)
        Resource.Error(e.message ?: "Failed to fetch withdrawal history")
    }

    override suspend fun getAllForAdmin(status: String?): Resource<List<WithdrawalEntity>> = try {
        val items = if (status != null)
            col.find(Filters.eq(WithdrawalEntity::status.name, status.uppercase())).toList()
        else
            col.find().toList()
        Resource.Success(items)
    } catch (e: Exception) {
        Resource.Error(e.message ?: "Failed to fetch withdrawals")
    }

    override suspend fun getAll(page: Int, size: Int, status: String?): Resource<Pair<List<WithdrawalEntity>, Long>> = try {
        val safePage = maxOf(1, page)
        val safeSize = minOf(maxOf(1, size), 100)
        val filter = status?.let { Filters.eq(WithdrawalEntity::status.name, it.uppercase()) }
        val total = if (filter != null) col.countDocuments(filter) else col.countDocuments()
        val items = (if (filter != null) col.find(filter) else col.find())
            .skip((safePage - 1) * safeSize).limit(safeSize).toList()
        Resource.Success(items to total)
    } catch (e: Exception) {
        Resource.Error(e.message ?: "Failed to fetch withdrawals")
    }

    override suspend fun updateStatus(trackingId: String, status: String): Resource<Unit> = try {
        col.updateOne(
            Filters.eq(WithdrawalEntity::trackingId.name, trackingId),
            Updates.combine(
                Updates.set(WithdrawalEntity::status.name, status),
                Updates.set(WithdrawalEntity::updatedAt.name, Clock.System.now().toString()),
            )
        )
        log.info("Withdrawal status updated trackingId=$trackingId status=$status")
        Resource.Success(Unit)
    } catch (e: Exception) {
        log.error("Failed to update withdrawal status trackingId=$trackingId — ${e.message}", e)
        Resource.Error(e.message ?: "Failed to update withdrawal status")
    }
}
