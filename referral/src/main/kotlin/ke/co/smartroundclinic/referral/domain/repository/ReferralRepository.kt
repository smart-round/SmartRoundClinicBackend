package ke.co.smartroundclinic.referral.domain.repository

import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.referral.data.entity.ReferralEntity

interface ReferralRepository {
    suspend fun create(entity: ReferralEntity): Resource<ReferralEntity>
    suspend fun getById(id: String): Resource<ReferralEntity?>
    suspend fun getByReferringDoctor(doctorId: String): Resource<List<ReferralEntity>>
    suspend fun getPendingByPatient(patientId: String): Resource<List<ReferralEntity>>

    /** Every referral (any status) for this patient — lets a patient revisit an already
     * accepted/declined referral and still book with that doctor if they change their mind. */
    suspend fun getByPatient(patientId: String): Resource<List<ReferralEntity>>

    /** The still-open (PENDING, or ACCEPTED but not yet booked) referral for this source appointment, if any — enforces "one active referral per completed visit". */
    suspend fun getActiveBySourceAppointment(appointmentId: String): Resource<ReferralEntity?>

    suspend fun accept(id: String, patientId: String): Resource<ReferralEntity?>
    suspend fun decline(id: String, patientId: String): Resource<ReferralEntity?>

    /** Records that [id] resulted in [appointmentId] once the patient books with the receiving doctor. */
    suspend fun linkResultingAppointment(id: String, appointmentId: String): Resource<Boolean>

    suspend fun getAllForAdmin(status: String?, page: Int, size: Int): Resource<Pair<List<ReferralEntity>, Long>>

    /** Unfiltered, unpaginated — used for admin dashboard stats aggregation. */
    suspend fun getAll(): Resource<List<ReferralEntity>>
}
