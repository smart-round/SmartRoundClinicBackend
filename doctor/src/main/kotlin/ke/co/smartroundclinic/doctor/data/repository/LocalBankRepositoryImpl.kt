package ke.co.smartroundclinic.doctor.data.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.mongodb.client.model.Filters
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import ke.co.smartroundclinic.common.MongoDBConstants
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.doctor.data.entity.LocalBankBranchEntity
import ke.co.smartroundclinic.doctor.data.entity.LocalBankEntity
import ke.co.smartroundclinic.doctor.domain.repository.LocalBankRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.net.URL

class LocalBankRepositoryImpl(database: MongoDatabase, private val banksJsonUrl: String) : LocalBankRepository {

    private val log = LoggerFactory.getLogger(LocalBankRepositoryImpl::class.java)
    private val col = database.getCollection<LocalBankEntity>(MongoDBConstants.ADMIN_LOCAL_BANKS)

    // Raw JSON shape from the static file
    private data class BranchJson(val branchCode: String, val branchName: String)
    private data class BankJson(val bankName: String, val bankCode: String, val branches: List<BranchJson>)

    /** Seeds the collection from the remote JSON if it is currently empty. */
    private suspend fun seedIfEmpty() {
        try {
            val count = col.countDocuments()
            if (count > 0L) return

            log.info("[LocalBanks] Collection empty — fetching seed data from $banksJsonUrl")
            @Suppress("DEPRECATION")
            val json = withContext(Dispatchers.IO) {
                URL(banksJsonUrl).readText()
            }

            val type = object : TypeToken<List<BankJson>>() {}.type
            val banks: List<BankJson> = Gson().fromJson(json, type)

            val entities = banks.map { b ->
                LocalBankEntity(
                    bankName = b.bankName,
                    bankCode = b.bankCode,
                    branches = b.branches.map { LocalBankBranchEntity(it.branchCode, it.branchName) },
                )
            }

            col.insertMany(entities)
            log.info("[LocalBanks] Seeded ${entities.size} banks successfully")
        } catch (e: Exception) {
            log.error("[LocalBanks] Seeding failed — ${e.message}", e)
        }
    }
    init {
        CoroutineScope(Dispatchers.IO).launch {
            seedIfEmpty()
        }
    }
    override suspend fun getAll(page: Int, size: Int): Resource<Pair<List<LocalBankEntity>, Long>> = try {

        val safePage = maxOf(1, page)
        val safeSize = minOf(maxOf(1, size), 100)
        val total = col.countDocuments()
        val items = col.find()
            .skip((safePage - 1) * safeSize)
            .limit(safeSize)
            .toList()
        Resource.Success(items to total)
    } catch (e: Exception) {
        log.error("[LocalBanks] getAll failed — ${e.message}", e)
        Resource.Error(e.message ?: "Failed to fetch banks")
    }

    override suspend fun searchByName(query: String, page: Int, size: Int): Resource<Pair<List<LocalBankEntity>, Long>> = try {
        seedIfEmpty()
        val trimmed = query.trim()
        if (trimmed.isBlank()) return Resource.Error("Search query cannot be empty")

        val filter = Filters.regex(LocalBankEntity::bankName.name, trimmed, "i")
        val safePage = maxOf(1, page)
        val safeSize = minOf(maxOf(1, size), 100)

        val total = col.countDocuments(filter)
        val items = col.find(filter)
            .skip((safePage - 1) * safeSize)
            .limit(safeSize)
            .toList()

        Resource.Success(items to total)
    } catch (e: Exception) {
        log.error("[LocalBanks] searchByName failed — ${e.message}", e)
        Resource.Error(e.message ?: "Search failed")
    }

    override suspend fun findByBankCode(bankCode: String): Resource<LocalBankEntity?> = try {
        seedIfEmpty()
        val entity = col.find(Filters.eq(LocalBankEntity::bankCode.name, bankCode.trim())).toList().firstOrNull()
        Resource.Success(entity)
    } catch (e: Exception) {
        log.error("[LocalBanks] findByBankCode failed — ${e.message}", e)
        Resource.Error(e.message ?: "Failed to find bank")
    }

    override suspend fun findByBranchCode(branchCode: String): Resource<List<LocalBankEntity>> = try {
        seedIfEmpty()
        val filter = Filters.elemMatch(
            LocalBankEntity::branches.name,
            Filters.eq(LocalBankBranchEntity::branchCode.name, branchCode.trim()),
        )
        val items = col.find(filter).toList()
        Resource.Success(items)
    } catch (e: Exception) {
        log.error("[LocalBanks] findByBranchCode failed — ${e.message}", e)
        Resource.Error(e.message ?: "Failed to find bank by branch code")
    }
}
