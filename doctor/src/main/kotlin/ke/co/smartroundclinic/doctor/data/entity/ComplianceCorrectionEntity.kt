package ke.co.smartroundclinic.doctor.data.entity

import org.bson.types.ObjectId
import kotlin.time.Clock

data class ComplianceCorrectionEntity(
    val id: String = ObjectId().toString(),
    val doctorId: String,
    val complianceId: String,
    val rejectionReason: String,
    val status: String = "PENDING",
    val submittedAt: String = Clock.System.now().toString(),
    val reviewedAt: String? = null,
    val reviewedBy: String? = null,
)
