package ke.co.smartroundclinic.admin.data.repository

import com.mongodb.client.model.BulkWriteOptions
import com.mongodb.client.model.Filters
import com.mongodb.client.model.ReplaceOneModel
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import ke.co.smartroundclinic.admin.data.entity.KmpdcPractitionerEntity
import ke.co.smartroundclinic.admin.domain.model.DoctorLicenceStatus
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

class KmpdcRepositoryImpl(database: MongoDatabase) : KmpdcRepository {

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

    // ── KmpdcRepository ───────────────────────────────────────────────────────

    private suspend fun safeFetch(url: String, parse: (String) -> List<KmpdcPractitionerEntity>): List<KmpdcPractitionerEntity> =
        try { parse(fetchHtml(url)) } catch (e: Exception) {
            println("[KMPDC] Skipped $url — ${e.message}")
            emptyList()
        }

    override suspend fun refreshAll(): Resource<Int> = try {
        val all: List<KmpdcPractitionerEntity> = coroutineScope {
            listOf(
                async { safeFetch(Urls.MEDICAL_MASTER)           { parseMedicalMasterHtml(it).map { r -> r.toEntity() } } },
                async { safeFetch(Urls.MEDICAL_GP)               { parseMedicalGpHtml(it).map { r -> r.toEntity() } } },
                async { safeFetch(Urls.MEDICAL_REGISTRAR)        { parseMedicalRegistrarHtml(it).map { r -> r.toEntity() } } },
                async { safeFetch(Urls.MEDICAL_SENIOR_REGISTRAR) { parseMedicalSeniorRegistrarHtml(it).map { r -> r.toEntity() } } },
                async { safeFetch(Urls.MEDICAL_SPECIALIST)       { parseMedicalSpecialistHtml(it).map { r -> r.toEntity() } } },
                async { safeFetch(Urls.DENTAL_MASTER)            { parseDentalMasterHtml(it).map { r -> r.toEntity() } } },
                async { safeFetch(Urls.DENTAL_GP)                { parseDentalGpHtml(it).map { r -> r.toEntity() } } },
                async { safeFetch(Urls.DENTAL_REGISTRAR)         { parseDentalRegistrarHtml(it).map { r -> r.toEntity() } } },
                async { safeFetch(Urls.DENTAL_SENIOR_REGISTRAR)  { parseDentalSeniorRegistrarHtml(it).map { r -> r.toEntity() } } },
                async { safeFetch(Urls.DENTAL_SPECIALIST)        { parseDentalSpecialistHtml(it).map { r -> r.toEntity() } } },
            ).awaitAll().flatten()
        }

        if (all.isEmpty()) return Resource.Success(0, "No records found on KMPDC website")

        val writes = all.map { entity ->
            val filter = Filters.and(
                Filters.eq("regNumber", entity.regNumber),
                Filters.eq("registerType", entity.registerType),
            )
            ReplaceOneModel(filter, entity, ReplaceOptions().upsert(true))
        }

        collection.bulkWrite(writes, BulkWriteOptions().ordered(false))
        Resource.Success(all.size, "Refreshed ${all.size} records from KMPDC")
    } catch (e: Exception) {
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
