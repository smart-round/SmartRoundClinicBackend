package ke.co.smartroundclinic.doctor.presentation.dto.request

import ke.co.smartroundclinic.doctor.domain.model.Specialization
import kotlinx.serialization.Serializable
import org.bson.types.ObjectId
import kotlin.time.Clock

@Serializable
data class AddSpecializationReq(
    val specializationId: String,
    val subSpecializationId: String? = null,
) {
    fun toModel(doctorId: String) = Specialization(
        id = ObjectId().toString(),
        doctorId = doctorId,
        specializationId = specializationId,
        subSpecializationId = subSpecializationId,
        createdAt = Clock.System.now().toString(),
    )
}

@Serializable
data class UpdateSpecializationReq(
    val specializationId: String,
    val subSpecializationId: String? = null,
)
