package ke.co.smartroundclinic.scheduling.data.lookup

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Sorts
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import ke.co.smartroundclinic.common.MongoDBConstants
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import kotlinx.serialization.Serializable
import org.bson.types.ObjectId
import org.slf4j.LoggerFactory
import kotlin.time.Clock

/** Status lifecycle: PENDING (just created on cancellation) -> COMPLETED | FAILED (set by the payments process). */
@Serializable
data class RefundDoc(
    val id: String = ObjectId().toString(),
    val appointmentId: String,
    val doctorId: String,
    val patientId: String,
    val paymentId: String,
    val amount: Double,
    val currency: String,
    val reason: String?,
    val cancelledBy: String,
    val cancelledByRole: String,
    val status: String = "PENDING",
    val createdAt: String = Clock.System.now().toString(),
    val updatedAt: String? = null,
)

/**
 * Writes/reads the `refunds` collection in the payments service's own database (src_payments),
 * following the same raw cross-DB access pattern as [PaymentVerificationLookup] and
 * [AppointmentAdminLookup] — this backend does not own that database.
 */
class RefundLookup(paymentsDb: MongoDatabase) {

    private val log = LoggerFactory.getLogger(RefundLookup::class.java)
    private val refunds = paymentsDb.getCollection<RefundDoc>(MongoDBConstants.REFUNDS)

    suspend fun create(refund: RefundDoc) {
        try {
            refunds.insertOne(refund)
            log.info("Refund record created id=${refund.id} appointmentId=${refund.appointmentId} amount=${refund.amount} ${refund.currency}")
        } catch (e: Exception) {
            log.error("Failed to create refund record for appointmentId=${refund.appointmentId} — ${e.message}", e)
        }
    }

    suspend fun getById(id: String): RefundDoc? = try {
        refunds.find(Filters.eq(RefundDoc::id.name, id)).firstOrNull()
    } catch (e: Exception) {
        log.error("Failed to fetch refund id=$id — ${e.message}", e)
        null
    }

    suspend fun getByAppointmentId(appointmentId: String): RefundDoc? = try {
        refunds.find(Filters.eq(RefundDoc::appointmentId.name, appointmentId)).firstOrNull()
    } catch (e: Exception) {
        log.error("Failed to fetch refund for appointmentId=$appointmentId — ${e.message}", e)
        null
    }

    /** Batch-fetches refunds for a list of appointmentIds. Returns a map of appointmentId -> RefundDoc. */
    suspend fun getByAppointmentIds(appointmentIds: Collection<String>): Map<String, RefundDoc> = try {
        if (appointmentIds.isEmpty()) {
            emptyMap()
        } else {
            refunds.find(Filters.`in`(RefundDoc::appointmentId.name, appointmentIds))
                .toList()
                .associateBy { it.appointmentId }
        }
    } catch (e: Exception) {
        log.error("Failed to batch-fetch refunds — ${e.message}", e)
        emptyMap()
    }

    suspend fun getAll(status: String?, page: Int, size: Int): Pair<List<RefundDoc>, Long> = try {
        val filter = status?.takeIf { it.isNotBlank() }?.let { Filters.eq(RefundDoc::status.name, it.uppercase()) }
        val total = if (filter != null) refunds.countDocuments(filter) else refunds.countDocuments()
        val query = if (filter != null) refunds.find(filter) else refunds.find()
        val items = query.sort(Sorts.descending(RefundDoc::createdAt.name)).skip((page - 1) * size).limit(size).toList()
        items to total
    } catch (e: Exception) {
        log.error("Failed to fetch refunds — ${e.message}", e)
        emptyList<RefundDoc>() to 0L
    }
}
