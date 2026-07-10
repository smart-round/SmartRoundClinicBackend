package ke.co.smartroundclinic.consultation.data.entity

import ke.co.smartroundclinic.common.sortableNowIso
import org.bson.types.ObjectId

/** One doc per (doctorId, patientId) thread — last-read/last-delivered watermarks for each side. */
data class ConsultationThreadReadStateEntity(
    val id: String = ObjectId().toString(),
    val doctorId: String,
    val patientId: String,
    val doctorLastReadAt: String? = null,
    val doctorLastDeliveredAt: String? = null,
    val patientLastReadAt: String? = null,
    val patientLastDeliveredAt: String? = null,
    val updatedAt: String = sortableNowIso(),
)
