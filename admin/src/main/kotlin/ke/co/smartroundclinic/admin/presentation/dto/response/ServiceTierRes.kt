package ke.co.smartroundclinic.admin.presentation.dto.response

import ke.co.smartroundclinic.admin.domain.model.ServiceTier
import kotlinx.serialization.Serializable

@Serializable
data class ServiceTierRes(
    val id: String,
    val name: String,
    val tierPrice: Double,
    val consultationDuration: Long,
    val gracePeriod: Long,
    val chatAccessWindow: Long,
    val followUpWindow: Long,
    val followUpFee: Long,
    val createdAt: String,
    val updatedAt: String?,
)

@Serializable
data class ServiceTierPageResult(
    val items: List<ServiceTierRes>,
    val total: Long,
    val page: Int,
    val size: Int,
    val pages: Long,
)

fun ServiceTier.toRes() = ServiceTierRes(
    id = id,
    name = name,
    tierPrice = tierPrice,
    consultationDuration = consultationDuration,
    gracePeriod = gracePeriod,
    chatAccessWindow = chatAccessWindow,
    followUpWindow = followUpWindow,
    followUpFee = followUpFee,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
