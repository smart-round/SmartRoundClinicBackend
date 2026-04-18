package ke.co.smartroundclinic.admin.presentation.dto.request

import ke.co.smartroundclinic.admin.domain.model.ServiceTier
import kotlinx.serialization.Serializable
import org.bson.types.ObjectId
import java.time.Instant

@Serializable
data class CreateServiceTierReq(
    val name: String,
    val tierPrice: Double,
    val consultationDuration: Long,
    val gracePeriod: Long,
    val chatAccessWindow: Long,
    val followUpWindow: Long,
    val followUpFee: Long,
) {
    fun toModel() = ServiceTier(
        id = ObjectId().toString(),
        name = name,
        tierPrice = tierPrice,
        consultationDuration = consultationDuration,
        gracePeriod = gracePeriod,
        chatAccessWindow = chatAccessWindow,
        followUpWindow = followUpWindow,
        followUpFee = followUpFee,
        createdAt = Instant.now().toString(),
        updatedAt = null,
    )
}

@Serializable
data class UpdateServiceTierReq(
    val name: String? = null,
    val tierPrice: Double? = null,
    val consultationDuration: Long? = null,
    val gracePeriod: Long? = null,
    val chatAccessWindow: Long? = null,
    val followUpWindow: Long? = null,
    val followUpFee: Long? = null,
)
