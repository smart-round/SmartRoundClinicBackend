package ke.co.smartroundclinic.admin.data.entity

import ke.co.smartroundclinic.admin.domain.model.ServiceTier
import org.bson.types.ObjectId
import java.time.Instant

data class ServiceTierEntity(
    val id: String = ObjectId().toString(),
    val name: String,
    val tierPrice: Double,
    val consultationDuration: Long,
    val gracePeriod: Long,
    val chatAccessWindow: Long,
    val followUpWindow: Long,
    val followUpFee: Long,
    val createdAt: String = Instant.now().toString(),
    val updatedAt: String? = null,
) {
    fun toModel() = ServiceTier(
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
}

fun ServiceTier.toEntity() = ServiceTierEntity(
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
