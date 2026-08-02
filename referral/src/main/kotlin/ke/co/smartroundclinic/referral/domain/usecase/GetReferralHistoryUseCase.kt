package ke.co.smartroundclinic.referral.domain.usecase

import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.infra.AppConfig
import ke.co.smartroundclinic.infra.storage.StorageRepository
import ke.co.smartroundclinic.referral.data.lookup.DoctorDisplayLookup
import ke.co.smartroundclinic.referral.domain.repository.ReferralRepository
import ke.co.smartroundclinic.referral.presentation.dto.response.ReferralRes
import ke.co.smartroundclinic.referral.presentation.dto.response.toRes

/** A patient's full referral history (any status) — unlike [GetPendingReferralsUseCase], this
 * also surfaces already-accepted/declined referrals, so a patient who changes their mind can
 * still find and book with a doctor they were previously referred to. */
class GetReferralHistoryUseCase(
    private val repository: ReferralRepository,
    private val doctorDisplayLookup: DoctorDisplayLookup,
    private val storageRepository: StorageRepository,
) {
    suspend operator fun invoke(patientId: String): DefaultResponse<List<ReferralRes>?> {
        val result = repository.getByPatient(patientId)
        val entities = result.data ?: emptyList()
        val receivingDoctorInfo = doctorDisplayLookup.bulkLookup(entities.map { it.receivingDoctorId }.toSet())

        val presignedByKey = (entities.mapNotNull { it.referringDoctorPicture } +
            entities.mapNotNull { receivingDoctorInfo[it.receivingDoctorId]?.profilePicture })
            .toSet()
            .associateWith { key ->
                (storageRepository.presignedGetUrl(
                    bucket = AppConfig.r2.bucket,
                    key = key,
                    expiresInSeconds = 86400,
                ) as? Resource.Success)?.data
            }

        return result.toDefaultResponse { items ->
            items?.map { entity ->
                val receiving = receivingDoctorInfo[entity.receivingDoctorId]
                entity.toModel().toRes(
                    receivingDoctorName = receiving?.name,
                    receivingDoctorPicture = receiving?.profilePicture?.let { presignedByKey[it] },
                ).copy(referringDoctorPicture = entity.referringDoctorPicture?.let { presignedByKey[it] })
            }
        }
    }
}
