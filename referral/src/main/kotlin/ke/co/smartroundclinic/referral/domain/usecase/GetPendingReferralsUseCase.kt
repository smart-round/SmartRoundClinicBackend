package ke.co.smartroundclinic.referral.domain.usecase

import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.referral.data.lookup.DoctorDisplayLookup
import ke.co.smartroundclinic.referral.domain.repository.ReferralRepository
import ke.co.smartroundclinic.referral.presentation.dto.response.ReferralRes
import ke.co.smartroundclinic.referral.presentation.dto.response.toRes

/** A patient's pending referral requests — backend endpoint built this round; consuming patient-app UI is a follow-up round. */
class GetPendingReferralsUseCase(
    private val repository: ReferralRepository,
    private val doctorDisplayLookup: DoctorDisplayLookup,
) {
    suspend operator fun invoke(patientId: String): DefaultResponse<List<ReferralRes>?> {
        val result = repository.getPendingByPatient(patientId)
        val entities = result.data ?: emptyList()
        val receivingDoctorInfo = doctorDisplayLookup.bulkLookup(entities.map { it.receivingDoctorId }.toSet())

        return result.toDefaultResponse { items ->
            items?.map { entity ->
                val receiving = receivingDoctorInfo[entity.receivingDoctorId]
                entity.toModel().toRes(
                    receivingDoctorName = receiving?.name,
                    receivingDoctorPicture = receiving?.profilePicture,
                )
            }
        }
    }
}
