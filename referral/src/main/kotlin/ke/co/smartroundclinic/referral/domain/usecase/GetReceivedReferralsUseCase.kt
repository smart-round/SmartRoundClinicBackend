package ke.co.smartroundclinic.referral.domain.usecase

import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.PatientNameResolver
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.infra.AppConfig
import ke.co.smartroundclinic.infra.storage.StorageRepository
import ke.co.smartroundclinic.referral.domain.repository.ReferralRepository
import ke.co.smartroundclinic.referral.presentation.dto.response.ReferralRes
import ke.co.smartroundclinic.referral.presentation.dto.response.toRes

/** A doctor's own received-referrals list — patients other doctors have referred to them,
 * independent of whether the patient has responded or booked yet. */
class GetReceivedReferralsUseCase(
    private val repository: ReferralRepository,
    private val patientNameResolver: PatientNameResolver? = null,
    private val storageRepository: StorageRepository,
) {
    suspend operator fun invoke(receivingDoctorId: String): DefaultResponse<List<ReferralRes>?> {
        val result = repository.getByReceivingDoctor(receivingDoctorId)
        val entities = result.data ?: emptyList()

        val patientNames = patientNameResolver?.getPatientNames(entities.map { it.patientId }.distinct()) ?: emptyMap()

        // referringDoctorPicture is a raw R2 key snapshotted at creation time — presign before
        // a client can actually load it.
        val presignedByKey = entities.mapNotNull { it.referringDoctorPicture }
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
                entity.toModel().toRes(
                    patientName = patientNames[entity.patientId],
                ).copy(referringDoctorPicture = entity.referringDoctorPicture?.let { presignedByKey[it] })
            }
        }
    }
}
