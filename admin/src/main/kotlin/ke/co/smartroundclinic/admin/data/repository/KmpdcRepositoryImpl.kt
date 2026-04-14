package ke.co.smartroundclinic.admin.data.repository

import com.mongodb.client.model.BulkWriteOptions
import com.mongodb.client.model.Filters
import com.mongodb.client.model.UpdateOneModel
import com.mongodb.client.model.UpdateOptions
import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import ke.co.smartroundclinic.admin.data.entity.KmpdcPractitionerEntity
import ke.co.smartroundclinic.admin.domain.model.DoctorLicenceStatus
import ke.co.smartroundclinic.admin.domain.model.KmpdcRefreshSummary
import ke.co.smartroundclinic.admin.domain.model.KmpdcRegisterType
import ke.co.smartroundclinic.admin.domain.model.toEntity
import ke.co.smartroundclinic.admin.domain.repository.KmpdcRepository
import ke.co.smartroundclinic.admin.utils.parseDentalGpHtml
import ke.co.smartroundclinic.admin.utils.parseDentalMasterHtml
import ke.co.smartroundclinic.admin.utils.parseDentalRegistrarHtml
import ke.co.smartroundclinic.admin.utils.parseDentalSeniorRegistrarHtml
import ke.co.smartroundclinic.admin.utils.parseDentalSpecialistHtml
import ke.co.smartroundclinic.admin.utils.parseMedicalGpHtml
import ke.co.smartroundclinic.admin.utils.parseMedicalMasterHtml
import ke.co.smartroundclinic.admin.utils.parseMedicalRegistrarHtml
import ke.co.smartroundclinic.admin.utils.parseMedicalSeniorRegistrarHtml
import ke.co.smartroundclinic.admin.utils.parseMedicalSpecialistHtml
import ke.co.smartroundclinic.common.MongoDBConstants
import ke.co.smartroundclinic.common.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withContext
import org.bson.conversions.Bson
import org.jsoup.Jsoup
import org.slf4j.LoggerFactory

class KmpdcRepositoryImpl(database: MongoDatabase) : KmpdcRepository {

    private val log = LoggerFactory.getLogger(KmpdcRepositoryImpl::class.java)

    private val collection =
        database.getCollection<KmpdcPractitionerEntity>(MongoDBConstants.ADMIN_KMPDC_PRACTITIONERS)

    // ── KMPDC page URLs ───────────────────────────────────────────────────────

    private object Urls {
        const val MEDICAL_MASTER           = "https://kmpdc.go.ke/Registers/medical_practitioners.php"
        const val MEDICAL_GP               = "https://kmpdc.go.ke/Registers/medical_general_practitioners.php"
        const val MEDICAL_REGISTRAR        = "https://kmpdc.go.ke/Registers/medical_registrar_practitioners.php"
        const val MEDICAL_SENIOR_REGISTRAR = "https://kmpdc.go.ke/Registers/medical_senior_registrar_practitioners.php"
        const val MEDICAL_SPECIALIST       = "https://kmpdc.go.ke/Registers/medical_specialist_practitioners.php"
        const val DENTAL_MASTER            = "https://kmpdc.go.ke/Registers/dental_practitioners.php"
        const val DENTAL_GP                = "https://kmpdc.go.ke/Registers/dental_general_practitioners.php"
        const val DENTAL_REGISTRAR         = "https://kmpdc.go.ke/Registers/dental_registrar_practitioners.php"
        const val DENTAL_SENIOR_REGISTRAR  = "https://kmpdc.go.ke/Registers/dental_senior_registrar_practitioners.php"
        const val DENTAL_SPECIALIST        = "https://kmpdc.go.ke/Registers/dental_specialist_practitioners.php"
    }

    private suspend fun fetchHtml(url: String): String = withContext(Dispatchers.IO) {
        Jsoup.connect(url)
            .userAgent("Mozilla/5.0")
            .timeout(30_000)
            .get()
            .outerHtml()
    }

    // ── Fetch helpers ─────────────────────────────────────────────────────────

    /** Fetches and parses one register. Returns null records on failure. */
    private suspend fun fetchRegister(
        name: String,
        url: String,
        parse: (String) -> List<KmpdcPractitionerEntity>,
    ): Pair<String, List<KmpdcPractitionerEntity>?> {
        log.info("[KMPDC] [$name] Fetching — $url")
        return try {
            val records = parse(fetchHtml(url))
            log.info("[KMPDC] [$name] OK — parsed ${records.size} records")
            name to records
        } catch (e: Exception) {
            log.error("[KMPDC] [$name] FAILED — ${e.message}")
            name to null
        }
    }

    // ── KmpdcRepository ───────────────────────────────────────────────────────

    override suspend fun refreshAll(): Resource<KmpdcRefreshSummary> = try {
        log.info("[KMPDC] Starting full register refresh — fetching 10 registers concurrently")
        val startMs = System.currentTimeMillis()

        val results: List<Pair<String, List<KmpdcPractitionerEntity>?>> = coroutineScope {
            listOf(
                async { fetchRegister("MEDICAL_MASTER",           Urls.MEDICAL_MASTER)           { parseMedicalMasterHtml(it).map { r -> r.toEntity() } } },
                async { fetchRegister("MEDICAL_GP",               Urls.MEDICAL_GP)               { parseMedicalGpHtml(it).map { r -> r.toEntity() } } },
                async { fetchRegister("MEDICAL_REGISTRAR",        Urls.MEDICAL_REGISTRAR)        { parseMedicalRegistrarHtml(it).map { r -> r.toEntity() } } },
                async { fetchRegister("MEDICAL_SENIOR_REGISTRAR", Urls.MEDICAL_SENIOR_REGISTRAR) { parseMedicalSeniorRegistrarHtml(it).map { r -> r.toEntity() } } },
                async { fetchRegister("MEDICAL_SPECIALIST",       Urls.MEDICAL_SPECIALIST)       { parseMedicalSpecialistHtml(it).map { r -> r.toEntity() } } },
                async { fetchRegister("DENTAL_MASTER",            Urls.DENTAL_MASTER)            { parseDentalMasterHtml(it).map { r -> r.toEntity() } } },
                async { fetchRegister("DENTAL_GP",                Urls.DENTAL_GP)                { parseDentalGpHtml(it).map { r -> r.toEntity() } } },
                async { fetchRegister("DENTAL_REGISTRAR",         Urls.DENTAL_REGISTRAR)         { parseDentalRegistrarHtml(it).map { r -> r.toEntity() } } },
                async { fetchRegister("DENTAL_SENIOR_REGISTRAR",  Urls.DENTAL_SENIOR_REGISTRAR)  { parseDentalSeniorRegistrarHtml(it).map { r -> r.toEntity() } } },
                async { fetchRegister("DENTAL_SPECIALIST",        Urls.DENTAL_SPECIALIST)        { parseDentalSpecialistHtml(it).map { r -> r.toEntity() } } },
            ).awaitAll()
        }

        val fetchDurationMs = System.currentTimeMillis() - startMs
        log.info("[KMPDC] All fetches completed in ${fetchDurationMs}ms")

        val failedRegisters = results.filter { it.second == null }.map { it.first }
        val all = results.mapNotNull { it.second }.flatten()

        results.forEach { (name, records) ->
            if (records != null) log.info("[KMPDC] [$name] OK — ${records.size} records")
            else                 log.warn("[KMPDC] [$name] FAILED — 0 records (skipped)")
        }

        if (all.isEmpty()) {
            log.warn("[KMPDC] No records scraped from any register — aborting DB write")
            return Resource.Success(
                KmpdcRefreshSummary(newRecords = 0, totalScraped = 0, failedRegisters = failedRegisters),
                "No records scraped from KMPDC",
            )
        }

        log.info("[KMPDC] Total scraped: ${all.size} records — writing new records to DB (existing untouched)")

        // Use $setOnInsert so existing documents are never touched — only NEW records are written.
        val writes: List<UpdateOneModel<KmpdcPractitionerEntity>> = all.map { entity ->
            val filter = Filters.and(
                Filters.eq("regNumber", entity.regNumber),
                Filters.eq("registerType", entity.registerType),
            )
            val update = Updates.combine(
                Updates.setOnInsert("id",              entity.id),
                Updates.setOnInsert("fullName",         entity.fullName),
                Updates.setOnInsert("regNumber",        entity.regNumber),
                Updates.setOnInsert("address",          entity.address),
                Updates.setOnInsert("qualifications",   entity.qualifications),
                Updates.setOnInsert("status",           entity.status),
                Updates.setOnInsert("practitionerType", entity.practitionerType),
                Updates.setOnInsert("registerType",     entity.registerType),
                Updates.setOnInsert("speciality",       entity.speciality),
                Updates.setOnInsert("subSpeciality",    entity.subSpeciality),
                Updates.setOnInsert("licenceType",      entity.licenceType),
                Updates.setOnInsert("discipline",       entity.discipline),
                Updates.setOnInsert("specialty",        entity.specialty),
                Updates.setOnInsert("lastRefreshedAt",  entity.lastRefreshedAt),
            )
            UpdateOneModel<KmpdcPractitionerEntity>(filter, update, UpdateOptions().upsert(true))
        }

        val bulkResult = collection.bulkWrite(writes, BulkWriteOptions().ordered(false))
        val newCount = bulkResult.upserts.size
        val skipped = all.size - newCount
        val totalDurationMs = System.currentTimeMillis() - startMs

        log.info("[KMPDC] DB write complete — new: $newCount, already existed (skipped): $skipped")
        if (failedRegisters.isNotEmpty()) log.warn("[KMPDC] Failed registers: $failedRegisters")
        log.info("[KMPDC] Refresh finished in ${totalDurationMs}ms")

        Resource.Success(
            KmpdcRefreshSummary(
                newRecords = newCount,
                totalScraped = all.size,
                failedRegisters = failedRegisters,
            ),
        )
    } catch (e: Exception) {
        log.error("[KMPDC] Refresh aborted with unexpected error — ${e.message}", e)
        Resource.Error(e.message ?: "Failed to refresh KMPDC data")
    }

    override suspend fun getPractitioners(
        registerType: KmpdcRegisterType?,
        status: DoctorLicenceStatus?,
        page: Int,
        size: Int,
    ): Resource<Pair<List<KmpdcPractitionerEntity>, Long>> = try {
        val filters = mutableListOf<Bson>()
        if (registerType != null) filters.add(Filters.eq("registerType", registerType.name))
        if (status != null) filters.add(Filters.eq("status", status.name))

        val filter = if (filters.isEmpty()) Filters.empty() else Filters.and(filters)
        val safePage = maxOf(1, page)
        val safeSize = minOf(maxOf(1, size), 200)

        val total = collection.countDocuments(filter)
        val items = collection.find(filter)
            .skip((safePage - 1) * safeSize)
            .limit(safeSize)
            .toList()

        Resource.Success(items to total)
    } catch (e: Exception) {
        Resource.Error(e.message ?: "Failed to fetch practitioners")
    }

    override suspend fun findByRegNumber(regNumber: String): Resource<KmpdcPractitionerEntity?> = try {
        val entity = collection.find(Filters.eq("regNumber", regNumber.trim())).firstOrNull()
        Resource.Success(entity)
    } catch (e: Exception) {
        Resource.Error(e.message ?: "Lookup failed")
    }

    override suspend fun searchByName(query: String, page: Int, size: Int): Resource<Pair<List<KmpdcPractitionerEntity>, Long>> = try {
        val words = query.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }
        if (words.isEmpty()) return Resource.Error("Search query cannot be empty")

        // Let MongoDB do an initial broad filter: any name containing at least one query word
        val pattern = words.joinToString("|") { Regex.escape(it) }
        val filter = Filters.regex("fullName", pattern, "i")

        // Fetch candidates (capped at 2000 to avoid unbounded memory use)
        val candidates = collection.find(filter).limit(2000).toList()

        // Score and sort in-memory: highest similarity first
        val scored = candidates
            .map { it to similarityScore(it.fullName, query) }
            .sortedByDescending { (_, score) -> score }

        val total = scored.size.toLong()
        val safePage = maxOf(1, page)
        val safeSize = minOf(maxOf(1, size), 100)
        val items = scored
            .drop((safePage - 1) * safeSize)
            .take(safeSize)
            .map { (entity, _) -> entity }

        Resource.Success(items to total)
    } catch (e: Exception) {
        Resource.Error(e.message ?: "Search failed")
    }

    // ── Fuzzy scoring helpers ─────────────────────────────────────────────────

    private fun similarityScore(fullName: String, query: String): Double {
        val name = fullName.lowercase().trim()
        val q = query.lowercase().trim()

        // Exact or prefix match — highest tier
        if (name == q) return 1.0
        if (name.startsWith(q)) return 0.95
        if (name.contains(q)) return 0.85

        // Word-level scoring — mid tier
        val nameWords = name.split("\\s+".toRegex())
        val queryWords = q.split("\\s+".toRegex())

        val wordScore = queryWords.sumOf { qw ->
            nameWords.maxOfOrNull { nw ->
                when {
                    nw == qw              -> 1.0
                    nw.startsWith(qw)     -> 0.85
                    qw.startsWith(nw)     -> 0.75
                    nw.contains(qw)       -> 0.65
                    else                  -> levenshteinSimilarity(nw, qw).coerceAtLeast(0.0)
                }
            } ?: 0.0
        } / queryWords.size

        return wordScore * 0.75
    }

    private fun levenshteinSimilarity(a: String, b: String): Double {
        val maxLen = maxOf(a.length, b.length)
        if (maxLen == 0) return 1.0
        return 1.0 - levenshteinDistance(a, b).toDouble() / maxLen
    }

    private fun levenshteinDistance(a: String, b: String): Int {
        val dp = Array(a.length + 1) { IntArray(b.length + 1) }
        for (i in 0..a.length) dp[i][0] = i
        for (j in 0..b.length) dp[0][j] = j
        for (i in 1..a.length) {
            for (j in 1..b.length) {
                dp[i][j] = if (a[i - 1] == b[j - 1]) dp[i - 1][j - 1]
                           else 1 + minOf(dp[i - 1][j], dp[i][j - 1], dp[i - 1][j - 1])
            }
        }
        return dp[a.length][b.length]
    }
}
