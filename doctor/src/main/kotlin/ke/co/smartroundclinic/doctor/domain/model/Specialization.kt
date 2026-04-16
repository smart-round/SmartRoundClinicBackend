package ke.co.smartroundclinic.doctor.domain.model

import ke.co.smartroundclinic.doctor.data.entity.SpecializationEntity
import org.bson.types.ObjectId

data class Specialization(
    val id: String,
    val doctorId: String,
    val specializationId: String,
    val subSpecializationId: String?,
    val createdAt: String,
)

fun SpecializationEntity.toModel() = Specialization(
    id = id,
    doctorId = doctorId,
    specializationId = specializationId,
    subSpecializationId = subSpecializationId,
    createdAt = createdAt,
)

fun Specialization.toEntity() = SpecializationEntity(
    id = id,
    doctorId = doctorId,
    specializationId = specializationId,
    subSpecializationId = subSpecializationId,
    createdAt = createdAt,
)
