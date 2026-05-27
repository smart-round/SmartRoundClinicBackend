package ke.co.smartroundclinic.doctor.data.repository

import com.mongodb.client.model.Accumulators
import com.mongodb.client.model.Aggregates
import com.mongodb.client.model.Filters
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import ke.co.smartroundclinic.common.MongoDBConstants
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.doctor.data.entity.PractitionerProfileEntity
import ke.co.smartroundclinic.doctor.data.entity.SpecializationEntity
import ke.co.smartroundclinic.doctor.domain.repository.RecommendationRepository
import ke.co.smartroundclinic.doctor.presentation.dto.response.RecommendedDoctorRes
import ke.co.smartroundclinic.doctor.presentation.dto.response.RecommendedDoctorsPageRes
import ke.co.smartroundclinic.doctor.presentation.dto.response.SpecializationWithNamesRes
import kotlinx.coroutines.flow.toList
import org.bson.Document
import org.slf4j.LoggerFactory
import kotlin.math.ln1p

class RecommendationRepositoryImpl(
    doctorDb: MongoDatabase,
    adminDb: MongoDatabase,
    schedulingDb: MongoDatabase,
    authDb: MongoDatabase,
) : RecommendationRepository {

    private val log = LoggerFactory.getLogger(RecommendationRepositoryImpl::class.java)
    private val profileCol = doctorDb.getCollection<PractitionerProfileEntity>(MongoDBConstants.DOCTOR_PROFILES)
    private val specializationsCol = doctorDb.getCollection<SpecializationEntity>(MongoDBConstants.DOCTOR_SPECIALIZATIONS)
    private val complianceCol = doctorDb.getCollection<Document>(MongoDBConstants.DOCTOR_COMPLIANCE)
    private val specialitiesCol = adminDb.getCollection<Document>(MongoDBConstants.ADMIN_SPECIALITIES)
    private val subSpecialitiesCol = adminDb.getCollection<Document>(MongoDBConstants.ADMIN_SUB_SPECIALITIES)
    private val appointmentsCol = schedulingDb.getCollection<Document>(MongoDBConstants.APPOINTMENTS)
    private val usersCol = authDb.getCollection<Document>(MongoDBConstants.AUTH_USER)

    override suspend fun getRecommendations(
        specializationId: String?,
        page: Int,
        size: Int,
    ): Resource<Pair<List<RecommendedDoctorRes>, Long>> = try {
        val safePage = maxOf(1, page)
        val safeSize = minOf(maxOf(1, size), 50)

        // Booking counts per doctor from scheduling DB (aggregation avoids full collection scan)
        val bookingCounts = loadBookingCounts()

        // All specializations: doctorId → entries
        val allSpecializations = specializationsCol.find().toList()
        val specsByDoctor = allSpecializations.groupBy { it.doctorId }

        // Filter doctor IDs when a speciality is specified
        val allowedDoctorIds: Set<String>? = specializationId?.let { sid ->
            allSpecializations.filter { it.specializationId == sid }.map { it.doctorId }.toSet()
                .also { if (it.isEmpty()) return Resource.Success(emptyList<RecommendedDoctorRes>() to 0L) }
        }

        // Only include doctors who have been verified (compliance approved)
        val verifiedDoctorIds = loadVerifiedDoctorIds()

        // Load only the relevant profiles
        val profileFilter = when {
            allowedDoctorIds != null -> Filters.`in`(PractitionerProfileEntity::doctorId.name, allowedDoctorIds.intersect(verifiedDoctorIds))
            else -> Filters.`in`(PractitionerProfileEntity::doctorId.name, verifiedDoctorIds)
        }
        if ((allowedDoctorIds != null && allowedDoctorIds.intersect(verifiedDoctorIds).isEmpty()) || verifiedDoctorIds.isEmpty()) {
            return Resource.Success(emptyList<RecommendedDoctorRes>() to 0L)
        }
        val profiles = profileCol.find(profileFilter).toList()
        if (profiles.isEmpty()) return Resource.Success(emptyList<RecommendedDoctorRes>() to 0L)

        // Pre-fetch all speciality/sub-speciality names needed
        val allSpecIds = allSpecializations.map { it.specializationId }.toSet()
        val allSubSpecIds = allSpecializations.mapNotNull { it.subSpecializationId }.toSet()
        val specialityNames = loadSpecialityNames(allSpecIds)
        val subSpecialityNames = if (allSubSpecIds.isNotEmpty()) loadSubSpecialityNames(allSubSpecIds) else emptyMap()

        // Bulk-load doctor names and profile pictures from auth users
        val allDoctorIds = profiles.map { it.doctorId }.toSet()
        val doctorInfo = loadDoctorInfo(allDoctorIds)

        // Normalization denominators (log-scale to avoid outlier domination)
        val maxBookings = (bookingCounts.values.maxOrNull() ?: 0).toDouble()
        val maxReviews = (profiles.maxOfOrNull { it.totalReviews } ?: 0).toDouble()

        // Speciality popularity for general listing: sum of all bookings across doctors in each speciality
        val specialityPopularity: Map<String, Double>? = if (specializationId == null) {
            val pop = mutableMapOf<String, Double>()
            for (profile in profiles) {
                val docBookings = (bookingCounts[profile.doctorId] ?: 0).toDouble()
                specsByDoctor[profile.doctorId]?.forEach { spec ->
                    pop[spec.specializationId] = (pop[spec.specializationId] ?: 0.0) + docBookings
                }
            }
            pop
        } else null
        val maxSpecPop = specialityPopularity?.values?.maxOrNull() ?: 1.0

        // Score every doctor
        data class Scored(val profile: PractitionerProfileEntity, val bookings: Int, val score: Double)

        val scored = profiles.map { profile ->
            val bookings = bookingCounts[profile.doctorId] ?: 0
            val ratingNorm   = profile.averageRating / 5.0
            val bookingsNorm = ln1p(bookings.toDouble()) / ln1p(maxBookings + 1.0)
            val reviewsNorm  = ln1p(profile.totalReviews.toDouble()) / ln1p(maxReviews + 1.0)
            val doctorScore  = ratingNorm * 0.50 + bookingsNorm * 0.35 + reviewsNorm * 0.15

            val finalScore = if (specializationId == null) {
                // Boost doctors whose speciality is in high demand
                val primarySpecId = specsByDoctor[profile.doctorId]?.firstOrNull()?.specializationId
                val specPop = specialityPopularity?.get(primarySpecId) ?: 0.0
                val specNorm = ln1p(specPop) / ln1p(maxSpecPop + 1.0)
                specNorm * 0.35 + doctorScore * 0.65
            } else {
                doctorScore
            }

            Scored(profile, bookings, finalScore)
        }.sortedByDescending { it.score }

        val total = scored.size.toLong()
        val pageItems = scored.drop((safePage - 1) * safeSize).take(safeSize)

        val items = pageItems.map { (profile, bookings, score) ->
            val specializationDetails = (specsByDoctor[profile.doctorId] ?: emptyList()).map { spec ->
                SpecializationWithNamesRes(
                    id = spec.id,
                    specializationId = spec.specializationId,
                    specializationName = specialityNames[spec.specializationId] ?: spec.specializationId,
                    subSpecializationId = spec.subSpecializationId,
                    subSpecializationName = spec.subSpecializationId?.let { subSpecialityNames[it] },
                )
            }
            RecommendedDoctorRes(
                profileId = profile.id,
                doctorId = profile.doctorId,
                doctorName = doctorInfo[profile.doctorId]?.first,
                profilePicture = doctorInfo[profile.doctorId]?.second,
                kmpdcRegNumber = profile.kmpdcRegNumber,
                title = profile.title,
                bio = profile.bio,
                yearsOfExperience = profile.yearsOfExperience,
                languages = profile.languages,
                facilityName = profile.facilityName,
                averageRating = profile.averageRating,
                totalReviews = profile.totalReviews,
                totalBookings = bookings,
                score = score,
                specializations = specializationDetails,
                createdAt = profile.createdAt,
                updatedAt = profile.updatedAt,
            )
        }

        Resource.Success(items to total)
    } catch (e: Exception) {
        log.error("Failed to compute recommendations — ${e.message}", e)
        Resource.Error(e.message ?: "Failed to fetch recommendations")
    }

    private suspend fun loadVerifiedDoctorIds(): Set<String> = try {
        complianceCol.find(
            Filters.and(
                Filters.eq("isApproved", true),
                Filters.eq("isMonetized", true),
            )
        ).toList()
            .mapNotNull { it.getString("doctorId") }
            .toSet()
    } catch (e: Exception) {
        log.warn("Could not load verified doctor IDs — ${e.message}")
        emptySet()
    }

    private suspend fun loadDoctorInfo(doctorIds: Set<String>): Map<String, Pair<String?, String?>> = try {
        if (doctorIds.isEmpty()) return emptyMap()
        usersCol.find(Filters.`in`("id", doctorIds)).toList()
            .associate { doc ->
                doc.getString("id") to (doc.getString("fullName") to doc.getString("profilePicture"))
            }
    } catch (e: Exception) {
        log.warn("Could not load doctor info — ${e.message}")
        emptyMap()
    }

    // Aggregate booking count per doctor from the appointments collection
    private suspend fun loadBookingCounts(): Map<String, Int> = try {
        val pipeline = listOf(
            Aggregates.group("\$doctorId", Accumulators.sum("count", 1))
        )
        appointmentsCol.aggregate<Document>(pipeline).toList()
            .associate { doc -> doc.getString("_id") to (doc.getInteger("count") ?: 0) }
    } catch (e: Exception) {
        log.warn("Could not load booking counts — ${e.message}")
        emptyMap()
    }

    private suspend fun loadSpecialityNames(ids: Set<String>): Map<String, String> = try {
        if (ids.isEmpty()) return emptyMap()
        specialitiesCol.find(Filters.`in`("id", ids)).toList()
            .associate { it.getString("id") to (it.getString("title") ?: "") }
    } catch (e: Exception) {
        log.warn("Could not load speciality names — ${e.message}")
        emptyMap()
    }

    private suspend fun loadSubSpecialityNames(ids: Set<String>): Map<String, String> = try {
        subSpecialitiesCol.find(Filters.`in`("id", ids)).toList()
            .associate { it.getString("id") to (it.getString("title") ?: "") }
    } catch (e: Exception) {
        log.warn("Could not load sub-speciality names — ${e.message}")
        emptyMap()
    }
}
