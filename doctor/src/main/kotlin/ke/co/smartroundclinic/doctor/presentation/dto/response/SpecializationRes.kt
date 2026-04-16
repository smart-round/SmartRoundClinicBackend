package ke.co.smartroundclinic.doctor.presentation.dto.response

import ke.co.smartroundclinic.doctor.domain.model.Specialization
import kotlinx.serialization.Serializable

@Serializable
data class SpecializationRes(
    val id: String,
    val doctorId: String,
    val specializationId: String,
    val subSpecializationId: String?,
    val createdAt: String,
)

fun Specialization.toRes() = SpecializationRes(
    id = id,
    doctorId = doctorId,
    specializationId = specializationId,
    subSpecializationId = subSpecializationId,
    createdAt = createdAt,
)