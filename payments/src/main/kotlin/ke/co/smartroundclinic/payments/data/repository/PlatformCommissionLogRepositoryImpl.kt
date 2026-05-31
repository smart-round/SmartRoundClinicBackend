package ke.co.smartroundclinic.payments.data.repository

import com.mongodb.client.model.Filters
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import ke.co.smartroundclinic.common.MongoDBConstants
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.payments.data.entity.PlatformCommissionLogEntity
import ke.co.smartroundclinic.payments.domain.repository.PlatformCommissionLogRepository
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import org.slf4j.LoggerFactory

class PlatformCommissionLogRepositoryImpl(database: MongoDatabase) : PlatformCommissionLogRepository {

    private val log = LoggerFactory.getLogger(PlatformCommissionLogRepositoryImpl::class.java)
    private val col = database.getCollection<PlatformCommissionLogEntity>(MongoDBConstants.PLATFORM_COMMISSION_LOGS)

    override suspend fun save(entity: PlatformCommissionLogEntity): Resource<PlatformCommissionLogEntity> = try {
        col.insertOne(entity)
        log.info("Commission log saved id=${entity.id} withdrawalId=${entity.withdrawalId} commission=${entity.totalCommission}")
        Resource.Success(entity, "Commission log recorded successfully")
    } catch (e: Exception) {
        log.error("Failed to save commission log — ${e.message}", e)
        Resource.Error(e.message ?: "Failed to record commission log")
    }

    override suspend fun getByDoctorId(doctorId: String): Resource<List<PlatformCommissionLogEntity>> = try {
        Resource.Success(col.find(Filters.eq(PlatformCommissionLogEntity::doctorId.name, doctorId)).toList())
    } catch (e: Exception) {
        Resource.Error(e.message ?: "Failed to fetch commission logs")
    }

    override suspend fun getByWithdrawalId(withdrawalId: String): Resource<PlatformCommissionLogEntity?> = try {
        Resource.Success(col.find(Filters.eq(PlatformCommissionLogEntity::withdrawalId.name, withdrawalId)).firstOrNull())
    } catch (e: Exception) {
        Resource.Error(e.message ?: "Failed to fetch commission log")
    }

    override suspend fun getAllForAdmin(): Resource<List<PlatformCommissionLogEntity>> = try {
        Resource.Success(col.find().toList())
    } catch (e: Exception) {
        Resource.Error(e.message ?: "Failed to fetch commission logs")
    }

    override suspend fun getAll(page: Int, size: Int): Resource<Pair<List<PlatformCommissionLogEntity>, Long>> = try {
        val safePage = maxOf(1, page)
        val safeSize = minOf(maxOf(1, size), 100)
        val total = col.countDocuments()
        val items = col.find().skip((safePage - 1) * safeSize).limit(safeSize).toList()
        Resource.Success(items to total)
    } catch (e: Exception) {
        Resource.Error(e.message ?: "Failed to fetch commission logs")
    }
}
