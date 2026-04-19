package ke.co.smartroundclinic.admin.data.entity

import ke.co.smartroundclinic.admin.domain.model.CommissionRate
import org.bson.types.ObjectId
import kotlin.time.Clock

data class CommissionRateEntity(
    val id: String = ObjectId().toString(),
    val adminId: String,
    val commissionRate: Double,
    val createdAt: String = Clock.System.now().toString(),
    val updatedAt: String? = null,
) {
    fun toModel() = CommissionRate(
        id = id,
        adminId = adminId,
        commissionRate = commissionRate,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}

fun CommissionRate.toEntity() = CommissionRateEntity(
    id = id,
    adminId = adminId,
    commissionRate = commissionRate,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
