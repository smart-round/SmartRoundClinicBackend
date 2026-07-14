package ke.co.smartroundclinic.admin.presentation.dto.request

import ke.co.smartroundclinic.admin.domain.model.CommissionRate
import kotlinx.serialization.Serializable
import org.bson.types.ObjectId
import kotlin.time.Clock

data class CreateCommissionRateReq(val commissionRate: Double) {
    fun toModel(adminId: String) = CommissionRate(
        id = ObjectId().toString(),
        adminId = adminId,
        commissionRate = commissionRate,
        createdAt = Clock.System.now().toString(),
        updatedAt = null,
    )
}
