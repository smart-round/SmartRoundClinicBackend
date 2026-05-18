package ke.co.smartroundclinic.doctor.data.entity

import org.bson.types.ObjectId
import kotlin.time.Clock

data class DoctorRatingEntity(
    val id: String = ObjectId().toString(),
    val appointmentId: String,
    val doctorId: String,
    val patientId: String,
    val rating: Int,
    val comment: String? = null,
    val createdAt: String = Clock.System.now().toString(),
    val updatedAt: String? = null,
)
