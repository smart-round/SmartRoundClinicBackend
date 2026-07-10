package ke.co.smartroundclinic.consultation.data.entity

import org.bson.types.ObjectId
import kotlin.time.Clock

/** Marks a thread as hidden from [userId]'s own conversation list ("delete for me"). */
data class ConsultationHiddenThreadEntity(
    val id: String = ObjectId().toString(),
    val userId: String,
    val doctorId: String,
    val patientId: String,
    val hiddenAt: String = Clock.System.now().toString(),
)
