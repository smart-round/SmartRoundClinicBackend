package ke.co.smartroundclinic.payments.data.repository

import com.mongodb.client.model.Filters
import com.mongodb.client.model.FindOneAndUpdateOptions
import com.mongodb.client.model.ReturnDocument
import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import ke.co.smartroundclinic.common.MongoDBConstants
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.payments.data.entity.EarningsLedgerEntity
import ke.co.smartroundclinic.payments.data.entity.EarningsLedgerLeg
import ke.co.smartroundclinic.payments.domain.repository.EarningsLedgerRepository
import kotlinx.coroutines.flow.toList
import org.slf4j.LoggerFactory
import kotlin.time.Clock

class EarningsLedgerRepositoryImpl(database: MongoDatabase) : EarningsLedgerRepository {

    private val log = LoggerFactory.getLogger(EarningsLedgerRepositoryImpl::class.java)
    private val col = database.getCollection<EarningsLedgerEntity>(MongoDBConstants.EARNINGS_LEDGER)

    override suspend fun recordLegCredited(
        leg: EarningsLedgerLeg,
        paymentId: String,
        appointmentId: String?,
        doctorId: String,
        grossAmount: Double,
        commissionRate: Double,
        commissionAmount: Double,
        netAmount: Double,
    ): Resource<EarningsLedgerEntity> = try {
        val creditedAtField = when (leg) {
            EarningsLedgerLeg.DOCTOR -> EarningsLedgerEntity::doctorCreditedAt.name
            EarningsLedgerLeg.COMMISSION -> EarningsLedgerEntity::commissionCreditedAt.name
        }
        val updated = col.findOneAndUpdate(
            Filters.eq(EarningsLedgerEntity::id.name, paymentId),
            Updates.combine(
                Updates.set(creditedAtField, Clock.System.now().toString()),
                Updates.setOnInsert(EarningsLedgerEntity::id.name, paymentId),
                Updates.setOnInsert(EarningsLedgerEntity::paymentId.name, paymentId),
                Updates.setOnInsert(EarningsLedgerEntity::appointmentId.name, appointmentId),
                Updates.setOnInsert(EarningsLedgerEntity::doctorId.name, doctorId),
                Updates.setOnInsert(EarningsLedgerEntity::grossAmount.name, grossAmount),
                Updates.setOnInsert(EarningsLedgerEntity::commissionRate.name, commissionRate),
                Updates.setOnInsert(EarningsLedgerEntity::commissionAmount.name, commissionAmount),
                Updates.setOnInsert(EarningsLedgerEntity::netAmount.name, netAmount),
                Updates.setOnInsert(EarningsLedgerEntity::createdAt.name, Clock.System.now().toString()),
            ),
            FindOneAndUpdateOptions().upsert(true).returnDocument(ReturnDocument.AFTER),
        ) ?: return Resource.Error("Failed to record earnings ledger entry")
        log.info("Earnings ledger leg=$leg recorded paymentId=$paymentId doctorId=$doctorId")
        Resource.Success(updated)
    } catch (e: Exception) {
        log.error("Failed to record earnings ledger leg=$leg paymentId=$paymentId — ${e.message}", e)
        Resource.Error(e.message ?: "Failed to record earnings ledger entry")
    }

    override suspend fun getByDoctorId(doctorId: String): Resource<List<EarningsLedgerEntity>> = try {
        Resource.Success(col.find(Filters.eq(EarningsLedgerEntity::doctorId.name, doctorId)).toList())
    } catch (e: Exception) {
        Resource.Error(e.message ?: "Failed to fetch earnings ledger entries")
    }

    override suspend fun getAllForAdmin(): Resource<List<EarningsLedgerEntity>> = try {
        Resource.Success(col.find().toList())
    } catch (e: Exception) {
        Resource.Error(e.message ?: "Failed to fetch earnings ledger entries")
    }

    override suspend fun getAll(page: Int, size: Int): Resource<Pair<List<EarningsLedgerEntity>, Long>> = try {
        val safePage = maxOf(1, page)
        val safeSize = minOf(maxOf(1, size), 100)
        val total = col.countDocuments()
        val items = col.find().skip((safePage - 1) * safeSize).limit(safeSize).toList()
        Resource.Success(items to total)
    } catch (e: Exception) {
        Resource.Error(e.message ?: "Failed to fetch earnings ledger entries")
    }
}
