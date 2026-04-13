package ke.co.smartroundclinic.admin.domain.repository

import ke.co.smartroundclinic.admin.data.entity.KmpdcPractitionerEntity
import ke.co.smartroundclinic.admin.domain.model.DoctorLicenceStatus
import ke.co.smartroundclinic.admin.domain.model.KmpdcRegisterType
import ke.co.smartroundclinic.common.Resource

interface KmpdcRepository {

    /** Scrape all 10 KMPDC registers and upsert into the database. Returns count of records processed. */
    suspend fun refreshAll(): Resource<Int>

    /** Query practitioners from the database with optional filters and pagination. */
    suspend fun getPractitioners(
        registerType: KmpdcRegisterType?,
        status: DoctorLicenceStatus?,
        page: Int,
        size: Int,
    ): Resource<Pair<List<KmpdcPractitionerEntity>, Long>>

    /** Look up a single practitioner by their KMPDC registration number. */
    suspend fun findByRegNumber(regNumber: String): Resource<KmpdcPractitionerEntity?>

    /**
     * Fuzzy-search practitioners by name.
     * Results are ranked by similarity — most similar first, least similar last.
     */
    suspend fun searchByName(query: String, page: Int, size: Int): Resource<Pair<List<KmpdcPractitionerEntity>, Long>>
}
