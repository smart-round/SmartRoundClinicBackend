package ke.co.smartroundclinic.doctorchat.data.entity

import ke.co.smartroundclinic.common.sortableNowIso
import org.bson.types.ObjectId

/**
 * One doc per unordered pair of doctors — the permanent doctor-to-doctor thread plus its
 * call-room bookkeeping, mirroring [ke.co.smartroundclinic.consultation.data.entity.ConsultationThreadEntity]'s
 * shape for the patient-doctor side, but for a symmetric doctor/doctor pair instead of a
 * doctor/patient one. `doctorAId`/`doctorBId` are always stored in sorted order (see
 * DoctorChatThreadRepositoryImpl.pairFilter) so `(A,B)` and `(B,A)` resolve to the same thread.
 */
data class DoctorChatThreadEntity(
    val id: String = ObjectId().toString(),
    val doctorAId: String,
    val doctorBId: String,
    val videoRoomId: String? = null,
    val lastVideoRoomId: String? = null,
    val createdAt: String = sortableNowIso(),
    val updatedAt: String? = null,
)
