package ke.co.smartroundclinic.scheduling.domain.usecase.appointment

import io.ktor.http.HttpStatusCode
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.scheduling.data.entity.AppointmentEntity
import ke.co.smartroundclinic.scheduling.data.lookup.AppointmentAdminLookup
import ke.co.smartroundclinic.scheduling.data.lookup.AppointmentPaymentInfo
import ke.co.smartroundclinic.scheduling.domain.repository.AppointmentRepository
import ke.co.smartroundclinic.scheduling.presentation.dto.response.AdminAppointmentPaymentRes
import ke.co.smartroundclinic.scheduling.presentation.dto.response.AdminAppointmentRes
import ke.co.smartroundclinic.scheduling.presentation.dto.response.AdminAppointmentsPageRes
import kotlin.math.ceil

class GetAdminAllAppointmentsUseCase(
    private val repository: AppointmentRepository,
    private val lookup: AppointmentAdminLookup,
) {
    suspend operator fun invoke(
        status: String?,
        page: Int,
        size: Int,
    ): DefaultResponse<AdminAppointmentsPageRes?> {
        val result = repository.getAllForAdmin(status, page, size)
        if (result !is Resource.Success) {
            return DefaultResponse(
                httpStatusCode = HttpStatusCode.InternalServerError.value,
                status = false,
                message = (result as Resource.Error).message ?: "Failed to fetch appointments",
                data = null,
            )
        }

        val (items, total) = result.data ?: return DefaultResponse(
            httpStatusCode = HttpStatusCode.OK.value,
            status = true,
            message = "Appointments fetched successfully",
            data = AdminAppointmentsPageRes(items = emptyList(), total = 0, page = page, size = size, pages = 0),
        )

        // Batch-fetch user names and payments in two round-trips
        val userIds = items.flatMap { listOf(it.doctorId, it.patientId) }.distinct()
        val namesMap = lookup.getUserNames(userIds)
        val paymentsMap = lookup.getPaymentsByAppointmentIds(items.map { it.id })

        return DefaultResponse(
            httpStatusCode = HttpStatusCode.OK.value,
            status = true,
            message = "Appointments fetched successfully",
            data = AdminAppointmentsPageRes(
                items = items.map { it.toAdminRes(namesMap, paymentsMap[it.id]) },
                total = total,
                page = page,
                size = size,
                pages = ceil(total.toDouble() / size).toLong(),
            )
        )
    }

    private fun AppointmentEntity.toAdminRes(
        names: Map<String, String>,
        payment: AppointmentPaymentInfo?,
    ) = AdminAppointmentRes(
        id = id,
        doctorId = doctorId,
        doctorName = names[doctorId] ?: "Unknown Doctor",
        patientId = patientId,
        patientName = names[patientId] ?: "Unknown Patient",
        date = date,
        slotStart = slotStart,
        slotEnd = slotEnd,
        consultationDuration = consultationDuration,
        status = status,
        bookedAt = bookedAt,
        notes = notes,
        cancellationReason = cancellationReason,
        cancelledBy = cancelledBy,
        updatedAt = updatedAt,
        payment = payment?.let {
            AdminAppointmentPaymentRes(
                paymentId = it.paymentId,
                amount = it.amount,
                currency = it.currency,
                status = it.status,
                commissionRate = it.commissionRate,
                platformFee = it.platformFee,
                netAmount = it.netAmount,
            )
        },
    )
}
