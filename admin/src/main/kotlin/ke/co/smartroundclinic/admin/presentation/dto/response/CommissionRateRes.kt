package ke.co.smartroundclinic.admin.presentation.dto.response

import ke.co.smartroundclinic.admin.domain.model.CommissionRate
import kotlinx.serialization.Serializable

@Serializable
data class CommissionRateRes(
    val id: String,
    val adminId: String,
    val commissionRate: Double,
    val createdAt: String,
    val updatedAt: String?,
)

@Serializable
data class CommissionRatePageResult(
    val items: List<CommissionRateRes>,
    val total: Long,
    val page: Int,
    val size: Int,
    val pages: Long,
)

fun CommissionRate.toRes() = CommissionRateRes(
    id = id,
    adminId = adminId,
    commissionRate = commissionRate,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
