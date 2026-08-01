package ke.co.smartroundclinic.referral.domain.usecase

import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.PatientNameResolver
import ke.co.smartroundclinic.referral.data.lookup.DoctorDisplayLookup
import ke.co.smartroundclinic.referral.domain.repository.ReferralRepository
import ke.co.smartroundclinic.referral.presentation.dto.response.ReferralRes
import ke.co.smartroundclinic.referral.presentation.dto.response.toRes

/** A doctor's own sent-referrals list. */
class GetMyReferralsUseCase(
    private val repository: ReferralRepository,
    private val doctorDisplayLookup: DoctorDisplayLookup,
    private val patientNameResolver: PatientNameResolver? = null,
) {
    suspend operator fun invoke(referringDoctorId: String): DefaultResponse<List<ReferralRes>?> {
        val result = repository.getByReferringDoctor(referringDoctorId)
        val entities = result.data ?: emptyList()

        val patientNames = patientNameResolver?.getPatientNames(entities.map { it.patientId }.distinct()) ?: emptyMap()
        val receivingDoctorInfo = doctorDisplayLookup.bulkLookup(entities.map { it.receivingDoctorId }.toSet())

        return result.toDefaultResponse { items ->
            items?.map { entity ->
                val receiving = receivingDoctorInfo[entity.receivingDoctorId]
                entity.toModel().toRes(
                    patientName = patientNames[entity.patientId],
                    receivingDoctorName = receiving?.name,
                    receivingDoctorPicture = receiving?.profilePicture,
                )
            }
        }
    }
}
