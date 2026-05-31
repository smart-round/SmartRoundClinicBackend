package ke.co.smartroundclinic.payments.domain.service

import ke.co.smartroundclinic.payments.domain.usecase.admin.GetAllWithdrawalsAdminUseCase
import ke.co.smartroundclinic.payments.domain.usecase.admin.GetCommissionLogsAdminUseCase
import ke.co.smartroundclinic.payments.domain.usecase.admin.GetCommissionTimeSummaryUseCase
import ke.co.smartroundclinic.payments.domain.usecase.admin.GetEarningsChartUseCase
import ke.co.smartroundclinic.payments.domain.usecase.admin.GetDoctorPaymentBreakdownUseCase
import ke.co.smartroundclinic.payments.domain.usecase.admin.GetPlatformOverviewUseCase

class AdminPaymentsService(
    private val overviewUseCase: GetPlatformOverviewUseCase,
    private val breakdownUseCase: GetDoctorPaymentBreakdownUseCase,
    private val withdrawalsUseCase: GetAllWithdrawalsAdminUseCase,
    private val commissionLogsUseCase: GetCommissionLogsAdminUseCase,
    private val commissionTimeSummaryUseCase: GetCommissionTimeSummaryUseCase,
    private val earningsChartUseCase: GetEarningsChartUseCase,
) {
    suspend fun getOverview() = overviewUseCase()
    suspend fun getAllDoctorBreakdowns() = breakdownUseCase.forAll()
    suspend fun getDoctorBreakdown(doctorId: String) = breakdownUseCase.forDoctor(doctorId)
    suspend fun getAllWithdrawals(page: Int, size: Int, status: String?) = withdrawalsUseCase.getAll(page, size, status)
    suspend fun getWithdrawalsByDoctor(doctorId: String) = withdrawalsUseCase.getByDoctor(doctorId)
    suspend fun getAllCommissionLogs(page: Int, size: Int) = commissionLogsUseCase.getAll(page, size)
    suspend fun getCommissionLogsByDoctor(doctorId: String) = commissionLogsUseCase.getByDoctor(doctorId)
    suspend fun getCommissionTimeSummary() = commissionTimeSummaryUseCase()
    suspend fun getEarningsChart(from: String?, to: String?) = earningsChartUseCase(from, to)
}
