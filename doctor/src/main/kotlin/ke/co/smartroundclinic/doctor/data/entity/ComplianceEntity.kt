package ke.co.smartroundclinic.doctor.data.entity

import org.bson.types.ObjectId
import kotlin.time.Clock

data class ComplianceEntity(
    val id: String = ObjectId().toString(),
    val doctorId: String,
    val isApproved: Boolean = false,
    val approvedAt: Long? = null,
    val approvedBy: String? = null,
    val failedApprovalReason: String? = null,
    val termsOfServiceAgreed: Boolean = true,
    val createdAt: String = Clock.System.now().toString(),
    val updatedAt: String? = null,
)
