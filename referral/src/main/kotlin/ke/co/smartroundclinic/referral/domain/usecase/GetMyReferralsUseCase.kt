package ke.co.smartroundclinic.referral.domain.usecase

import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.PatientNameResolver
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.common.UserProfilePictureResolver
import ke.co.smartroundclinic.infra.AppConfig
import ke.co.smartroundclinic.infra.storage.StorageRepository
import ke.co.smartroundclinic.referral.data.lookup.DoctorDisplayLookup
import ke.co.smartroundclinic.referral.domain.repository.ReferralRepository
import ke.co.smartroundclinic.referral.presentation.dto.response.ReferralRes
import ke.co.smartroundclinic.referral.presentation.dto.response.toRes

/** A doctor's own sent-referrals list. */
class GetMyReferralsUseCase(
    private val repository: ReferralRepository,
    private val doctorDisplayLookup: DoctorDisplayLookup,
    private val patientNameResolver: PatientNameResolver? = null,
    private val userProfilePictureResolver: UserProfilePictureResolver? = null,
    private val storageRepository: StorageRepository,
) {
    suspend operator fun invoke(referringDoctorId: String): DefaultResponse<List<ReferralRes>?> {
        val result = repository.getByReferringDoctor(referringDoctorId)
        val entities = result.data ?: emptyList()

        val patientIds = entities.map { it.patientId }.distinct()
        val patientNames = patientNameResolver?.getPatientNames(patientIds) ?: emptyMap()
        val patientPictures = userProfilePictureResolver?.getProfilePictureUrls(patientIds) ?: emptyMap()
        val receivingDoctorInfo = doctorDisplayLookup.bulkLookup(entities.map { it.receivingDoctorId }.toSet())

        // receivingDoctorInfo's profilePicture is a raw R2 key resolved live from auth_user — needs
        // presigning before a client can actually load it (same fix already applied to the
        // patient-facing pending/history use cases).
        val presignedByKey = entities.mapNotNull { receivingDoctorInfo[it.receivingDoctorId]?.profilePicture }
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
                    patientName = patientNames[entity.patientId],
                    patientProfilePicture = patientPictures[entity.patientId],
                    receivingDoctorName = receiving?.name,
                    receivingDoctorPicture = receiving?.profilePicture?.let { presignedByKey[it] },
                )
            }
        }
    }
}
