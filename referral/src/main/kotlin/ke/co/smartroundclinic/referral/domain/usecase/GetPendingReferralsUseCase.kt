package ke.co.smartroundclinic.referral.domain.usecase

import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.infra.AppConfig
import ke.co.smartroundclinic.infra.storage.StorageRepository
import ke.co.smartroundclinic.referral.data.lookup.DoctorDisplayLookup
import ke.co.smartroundclinic.referral.domain.repository.ReferralRepository
import ke.co.smartroundclinic.referral.presentation.dto.response.ReferralRes
import ke.co.smartroundclinic.referral.presentation.dto.response.toRes

/** A patient's pending referral requests — backend endpoint built this round; consuming patient-app UI is a follow-up round. */
class GetPendingReferralsUseCase(
    private val repository: ReferralRepository,
    private val doctorDisplayLookup: DoctorDisplayLookup,
    private val storageRepository: StorageRepository,
) {
    suspend operator fun invoke(patientId: String): DefaultResponse<List<ReferralRes>?> {
        val result = repository.getPendingByPatient(patientId)
        val entities = result.data ?: emptyList()
        val receivingDoctorInfo = doctorDisplayLookup.bulkLookup(entities.map { it.receivingDoctorId }.toSet())

        // referringDoctorPicture is a snapshot key captured at creation time, and
        // receivingDoctorPicture is resolved live above — both are raw R2 keys that need
        // presigning before a client can actually load them.
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
