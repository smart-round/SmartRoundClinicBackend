package ke.co.smartroundclinic.scheduling.domain.usecase.appointment

import io.ktor.http.HttpStatusCode
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.scheduling.data.lookup.AppointmentAdminLookup
import ke.co.smartroundclinic.scheduling.domain.repository.AppointmentRepository
import ke.co.smartroundclinic.scheduling.presentation.dto.response.AppointmentStatsRes
import java.time.LocalDate
import java.time.ZoneOffset

class GetAdminAppointmentStatsUseCase(
    private val repository: AppointmentRepository,
    private val lookup: AppointmentAdminLookup,
) {
    suspend operator fun invoke(): DefaultResponse<AppointmentStatsRes?> {
        val appointments = (repository.getAll() as? Resource.Success)?.data ?: emptyList()
        val today = LocalDate.now(ZoneOffset.UTC).toString()

        val byStatus = appointments.groupBy { it.status.uppercase() }
        val appointmentIds = appointments.map { it.id }
        val paymentsMap = lookup.getPaymentsByAppointmentIds(appointmentIds)

        val completedPayments = paymentsMap.values.filter { it.status.uppercase() == "COMPLETED" }
        val totalRevenue = completedPayments.sumOf { it.amount }
        val totalCommission = completedPayments.sumOf { it.platformFee }

        return DefaultResponse(
            httpStatusCode = HttpStatusCode.OK.value,
            status = true,
            message = "Appointment stats fetched successfully",
            data = AppointmentStatsRes(
                total = appointments.size,
                booked = byStatus["BOOKED"]?.size ?: 0,
                confirmed = byStatus["CONFIRMED"]?.size ?: 0,
                completed = byStatus["COMPLETED"]?.size ?: 0,
                cancelled = byStatus["CANCELLED"]?.size ?: 0,
                todayCount = appointments.count { it.date == today },
                totalRevenue = totalRevenue,
                totalCommission = totalCommission,
                totalNetRevenue = totalRevenue - totalCommission,
            )
        )
    }
}
