package ke.co.smartroundclinic.payments.domain.service

import ke.co.smartroundclinic.infra.IntaSendConfig
import ke.co.smartroundclinic.payments.domain.usecase.GetPaymentByIdUseCase
import ke.co.smartroundclinic.payments.domain.usecase.GetPaymentsByPatientUseCase
import ke.co.smartroundclinic.payments.domain.usecase.admin.GetAllWithdrawalsAdminUseCase
import ke.co.smartroundclinic.payments.domain.usecase.admin.GetCommissionLogsAdminUseCase
import ke.co.smartroundclinic.payments.domain.usecase.admin.GetCommissionTimeSummaryUseCase
import ke.co.smartroundclinic.payments.domain.usecase.admin.GetEarningsChartUseCase
import ke.co.smartroundclinic.payments.domain.usecase.admin.GetDoctorPaymentBreakdownUseCase
import ke.co.smartroundclinic.payments.domain.usecase.admin.GetPlatformOverviewUseCase
import ke.co.smartroundclinic.payments.domain.usecase.wallet.GetDoctorWalletBalanceAdminUseCase
import ke.co.smartroundclinic.payments.domain.usecase.wallet.GetDoctorWalletStatementAdminUseCase
import ke.co.smartroundclinic.payments.domain.usecase.wallet.GetPlatformWalletBalanceUseCase
import ke.co.smartroundclinic.payments.domain.usecase.wallet.GetPlatformWalletStatementUseCase

class AdminPaymentsService(
    private val overviewUseCase: GetPlatformOverviewUseCase,
    private val breakdownUseCase: GetDoctorPaymentBreakdownUseCase,
    private val withdrawalsUseCase: GetAllWithdrawalsAdminUseCase,
    private val commissionLogsUseCase: GetCommissionLogsAdminUseCase,
    private val commissionTimeSummaryUseCase: GetCommissionTimeSummaryUseCase,
    private val earningsChartUseCase: GetEarningsChartUseCase,
    private val getByPatientUseCase: GetPaymentsByPatientUseCase,
    private val getByIdUseCase: GetPaymentByIdUseCase,
    private val getWalletBalanceUseCase: GetPlatformWalletBalanceUseCase,
    private val getWalletStatementUseCase: GetPlatformWalletStatementUseCase,
    private val getDoctorWalletBalanceUseCase: GetDoctorWalletBalanceAdminUseCase,
    private val getDoctorWalletStatementUseCase: GetDoctorWalletStatementAdminUseCase,
    private val config: IntaSendConfig,
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
    suspend fun getByPatient(patientId: String, page: Int, size: Int) = getByPatientUseCase(patientId, page, size)
    suspend fun getById(id: String) = getByIdUseCase(id)
    suspend fun getCollectionsWalletBalance() = getWalletBalanceUseCase(config.collectionsWalletId)
    suspend fun getCollectionsWalletStatement(page: Int) = getWalletStatementUseCase(config.collectionsWalletId, page)
    suspend fun getCommissionWalletBalance() = getWalletBalanceUseCase(config.commissionWalletId)
    suspend fun getCommissionWalletStatement(page: Int) = getWalletStatementUseCase(config.commissionWalletId, page)
    suspend fun getDoctorWalletBalance(doctorId: String) = getDoctorWalletBalanceUseCase(doctorId)
    suspend fun getDoctorWalletStatement(doctorId: String, page: Int) = getDoctorWalletStatementUseCase(doctorId, page)
}
