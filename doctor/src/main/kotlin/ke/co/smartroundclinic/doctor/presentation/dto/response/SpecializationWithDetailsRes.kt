package ke.co.smartroundclinic.doctor.presentation.dto.response

import kotlinx.serialization.Serializable

@Serializable
data class SpecialityInfoRes(
    val id: String,
    val title: String,
    val description: String,
    val color: String,
    val iconUrl: String?,
)

@Serializable
data class SubSpecialityInfoRes(
    val id: String,
    val title: String,
    val description: String,
    val color: String,
    val iconUrl: String?,
)

@Serializable
data class SpecializationWithDetailsRes(
    val id: String,
    val doctorId: String,
    val specialization: SpecialityInfoRes,
    val subSpecialization: SubSpecialityInfoRes?,
    val createdAt: String,
    val updatedAt: String?,
)
