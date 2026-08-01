package ke.co.smartroundclinic.referral.domain.usecase.admin

import io.ktor.http.HttpStatusCode
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.PatientNameResolver
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.referral.data.lookup.DoctorDisplayLookup
import ke.co.smartroundclinic.referral.domain.repository.ReferralRepository
import ke.co.smartroundclinic.referral.presentation.dto.response.AdminReferralsPageRes
import ke.co.smartroundclinic.referral.presentation.dto.response.toRes
import kotlin.math.ceil

class GetAdminReferralsUseCase(
    private val repository: ReferralRepository,
    private val doctorDisplayLookup: DoctorDisplayLookup,
    private val patientNameResolver: PatientNameResolver? = null,
) {
    suspend operator fun invoke(status: String?, page: Int, size: Int): DefaultResponse<AdminReferralsPageRes?> {
        val result = repository.getAllForAdmin(status, page, size)
        if (result !is Resource.Success) {
            return DefaultResponse(
                httpStatusCode = HttpStatusCode.InternalServerError.value,
                status = false,
                message = (result as? Resource.Error)?.message ?: "Failed to fetch referrals",
                data = null,
            )
        }

        val (items, total) = result.data ?: return DefaultResponse(
            httpStatusCode = HttpStatusCode.OK.value,
            status = true,
            message = "Referrals fetched successfully",
            data = AdminReferralsPageRes(items = emptyList(), total = 0, page = page, size = size, pages = 0),
        )

        val patientNames = patientNameResolver?.getPatientNames(items.map { it.patientId }.distinct()) ?: emptyMap()
        val doctorIds = (items.map { it.receivingDoctorId } + items.map { it.referringDoctorId }).distinct().toSet()
        val doctorInfo = doctorDisplayLookup.bulkLookup(doctorIds)

        return DefaultResponse(
            httpStatusCode = HttpStatusCode.OK.value,
            status = true,
            message = "Referrals fetched successfully",
            data = AdminReferralsPageRes(
                items = items.map { entity ->
                    val receiving = doctorInfo[entity.receivingDoctorId]
                    entity.toModel().toRes(
                        patientName = patientNames[entity.patientId],
                        receivingDoctorName = receiving?.name,
                        receivingDoctorPicture = receiving?.profilePicture,
                    )
                },
                total = total,
                page = page,
                size = size,
                pages = ceil(total.toDouble() / size).toLong(),
            ),
        )
    }
}
