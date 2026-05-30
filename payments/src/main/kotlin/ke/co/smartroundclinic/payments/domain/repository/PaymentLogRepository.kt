package ke.co.smartroundclinic.payments.domain.repository

import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.payments.data.entity.PaymentLogEntity

interface PaymentLogRepository {
    suspend fun log(entity: PaymentLogEntity): Resource<PaymentLogEntity>
    suspend fun getByInvoiceId(invoiceId: String): Resource<List<PaymentLogEntity>>
    suspend fun getByAppointmentId(appointmentId: String): Resource<List<PaymentLogEntity>>
}
