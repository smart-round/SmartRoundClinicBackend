package ke.co.smartroundclinic.doctor.data.entity

import org.bson.types.ObjectId
import kotlin.time.Clock

data class PractitionerCertificationsEntity(
    val id: String = ObjectId().toString(),
    val doctorId: String,
    val certificationName: String,
    val certificationDate: String,
    val certificationUrl: String? = null,
    val createdAt: String = Clock.System.now().toString(),
    val updatedAt: String? = null,
)