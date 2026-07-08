package ke.co.smartroundclinic.patient.data.entity

import org.bson.types.ObjectId
import kotlin.time.Clock

data class PatientRatingEntity(
    val id: String = ObjectId().toString(),
    val appointmentId: String,
    val doctorId: String,
    val patientId: String,
    val rating: Int,
    val comment: String? = null,
    val createdAt: String = Clock.System.now().toString(),
    val updatedAt: String? = null,
)
