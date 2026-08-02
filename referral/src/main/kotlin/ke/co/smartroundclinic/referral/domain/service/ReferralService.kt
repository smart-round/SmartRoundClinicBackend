package ke.co.smartroundclinic.referral.domain.service

import ke.co.smartroundclinic.referral.domain.usecase.AcceptReferralUseCase
import ke.co.smartroundclinic.referral.domain.usecase.CreateReferralUseCase
import ke.co.smartroundclinic.referral.domain.usecase.DeclineReferralUseCase
import ke.co.smartroundclinic.referral.domain.usecase.GetMyReferralsUseCase
import ke.co.smartroundclinic.referral.domain.usecase.GetPendingReferralsUseCase
import ke.co.smartroundclinic.referral.domain.usecase.GetReferralHistoryUseCase
import ke.co.smartroundclinic.referral.domain.usecase.ReferralEligibilityUseCase
import ke.co.smartroundclinic.referral.domain.usecase.admin.GetAdminReferralStatsUseCase
import ke.co.smartroundclinic.referral.domain.usecase.admin.GetAdminReferralsUseCase
import ke.co.smartroundclinic.referral.presentation.dto.request.CreateReferralReq

class ReferralService(
    private val createReferralUseCase: CreateReferralUseCase,
    private val eligibilityUseCase: ReferralEligibilityUseCase,
    private val getMyReferralsUseCase: GetMyReferralsUseCase,
    private val getPendingReferralsUseCase: GetPendingReferralsUseCase,
    private val getReferralHistoryUseCase: GetReferralHistoryUseCase,
    private val acceptReferralUseCase: AcceptReferralUseCase,
    private val declineReferralUseCase: DeclineReferralUseCase,
    private val getAdminReferralsUseCase: GetAdminReferralsUseCase,
    private val getAdminReferralStatsUseCase: GetAdminReferralStatsUseCase,
) {
    suspend fun create(req: CreateReferralReq, referringDoctorId: String) = createReferralUseCase(req, referringDoctorId)
    suspend fun eligibility(appointmentId: String, doctorId: String) = eligibilityUseCase(appointmentId, doctorId)
    suspend fun mine(referringDoctorId: String) = getMyReferralsUseCase(referringDoctorId)
    suspend fun pending(patientId: String) = getPendingReferralsUseCase(patientId)
    suspend fun history(patientId: String) = getReferralHistoryUseCase(patientId)
    suspend fun accept(id: String, patientId: String) = acceptReferralUseCase(id, patientId)
    suspend fun decline(id: String, patientId: String) = declineReferralUseCase(id, patientId)
    suspend fun adminList(status: String?, page: Int, size: Int) = getAdminReferralsUseCase(status, page, size)
    suspend fun adminStats() = getAdminReferralStatsUseCase()
}
