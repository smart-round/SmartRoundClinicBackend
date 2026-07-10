package ke.co.smartroundclinic.consultation.data.entity

import ke.co.smartroundclinic.common.sortableNowIso
import org.bson.types.ObjectId

/** Marks a thread as hidden from [userId]'s own conversation list ("delete for me"). */
data class ConsultationHiddenThreadEntity(
    val id: String = ObjectId().toString(),
    val userId: String,
    val doctorId: String,
    val patientId: String,
    val hiddenAt: String = sortableNowIso(),
)
