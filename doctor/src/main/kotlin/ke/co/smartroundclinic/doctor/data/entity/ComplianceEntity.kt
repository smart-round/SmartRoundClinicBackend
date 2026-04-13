package ke.co.smartroundclinic.doctor.data.entity

import org.bson.types.ObjectId

data class ComplianceEntity(
    val id:String = ObjectId().toString(),
    val doctorId:String,
    val isApproved:Boolean,
    val approvedAt:Long? = null,
    val approvedBy:String? = null,
    val failedApprovalReason: String?
)
