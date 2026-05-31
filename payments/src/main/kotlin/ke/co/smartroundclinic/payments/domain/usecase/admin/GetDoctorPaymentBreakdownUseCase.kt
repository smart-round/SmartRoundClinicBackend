package ke.co.smartroundclinic.payments.domain.usecase.admin

import io.ktor.http.HttpStatusCode
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.payments.data.entity.PaymentEntity
import ke.co.smartroundclinic.payments.data.entity.WithdrawalEntity
import ke.co.smartroundclinic.payments.domain.repository.PaymentRepository
import ke.co.smartroundclinic.payments.domain.repository.WithdrawalRepository
import ke.co.smartroundclinic.payments.presentation.dto.response.DoctorPaymentBreakdownRes
import ke.co.smartroundclinic.payments.presentation.dto.response.DoctorPaymentBreakdownsRes

class GetDoctorPaymentBreakdownUseCase(
    private val paymentRepository: PaymentRepository,
    private val withdrawalRepository: WithdrawalRepository,
) {
    suspend fun forDoctor(doctorId: String): DefaultResponse<DoctorPaymentBreakdownRes?> {
        val payments = (paymentRepository.getAllByDoctorId(doctorId) as? Resource.Success)?.data ?: emptyList()
        val withdrawals = (withdrawalRepository.getByDoctorId(doctorId) as? Resource.Success)?.data ?: emptyList()
        return DefaultResponse(
            httpStatusCode = HttpStatusCode.OK.value,
            status = true,
            message = "Doctor payment breakdown fetched successfully",
            data = buildBreakdown(doctorId, payments, withdrawals),
        )
    }

    suspend fun forAll(): DefaultResponse<DoctorPaymentBreakdownsRes?> {
        val allPayments = (paymentRepository.getAllForAdmin() as? Resource.Success)?.data ?: emptyList()
        val allWithdrawals = (withdrawalRepository.getAllForAdmin() as? Resource.Success)?.data ?: emptyList()

        val doctorIds = allPayments.map { it.doctorId }.distinct()
        val breakdowns = doctorIds.map { id ->
            buildBreakdown(
                doctorId = id,
                payments = allPayments.filter { it.doctorId == id },
                withdrawals = allWithdrawals.filter { it.doctorId == id },
            )
        }.sortedByDescending { it.totalNetEarnings }

        return DefaultResponse(
            httpStatusCode = HttpStatusCode.OK.value,
            status = true,
            message = "Doctor payment breakdowns fetched successfully",
            data = DoctorPaymentBreakdownsRes(doctors = breakdowns, total = breakdowns.size),
        )
    }

    private fun buildBreakdown(
        doctorId: String,
        payments: List<PaymentEntity>,
        withdrawals: List<WithdrawalEntity>,
    ): DoctorPaymentBreakdownRes {
        val completed = payments.filter { it.status == PaymentEntity.PaymentStatus.COMPLETED }
        val pending = payments.filter { it.status == PaymentEntity.PaymentStatus.PENDING }
        val totalGross = completed.sumOf { it.amount }
        val totalCommission = completed.sumOf { it.amount * (it.commissionRate / 100.0) }
        val totalNetEarnings = totalGross - totalCommission

        val pendingW = withdrawals.filter { it.status == WithdrawalEntity.WithdrawalStatus.PENDING.name }.sumOf { it.amount }
        val completedW = withdrawals.filter { it.status == WithdrawalEntity.WithdrawalStatus.COMPLETED.name }.sumOf { it.amount }
        val totalWithdrawn = pendingW + completedW

        return DoctorPaymentBreakdownRes(
            doctorId = doctorId,
            totalGross = totalGross,
            totalCommission = totalCommission,
            totalNetEarnings = totalNetEarnings,
            totalWithdrawn = totalWithdrawn,
            pendingWithdrawals = pendingW,
            completedWithdrawals = completedW,
            availableBalance = totalNetEarnings - totalWithdrawn,
            completedPaymentsCount = completed.size,
            pendingPaymentsCount = pending.size,
            totalWithdrawalsCount = withdrawals.size,
        )
    }
}
