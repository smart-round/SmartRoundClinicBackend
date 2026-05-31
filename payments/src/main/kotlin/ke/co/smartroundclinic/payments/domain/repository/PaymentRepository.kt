package ke.co.smartroundclinic.payments.domain.repository

import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.payments.data.entity.PaymentEntity

interface PaymentRepository {
    suspend fun save(entity: PaymentEntity): Resource<PaymentEntity>
    suspend fun getById(id: String): Resource<PaymentEntity?>
    suspend fun getByAppointmentId(appointmentId: String): Resource<PaymentEntity?>
    suspend fun getByPatientId(patientId: String, page: Int, size: Int): Resource<Pair<List<PaymentEntity>, Long>>
    suspend fun getByDoctorId(doctorId: String, page: Int, size: Int): Resource<Pair<List<PaymentEntity>, Long>>
    suspend fun getAll(page: Int, size: Int, status: String?): Resource<Pair<List<PaymentEntity>, Long>>
    suspend fun getByTransactionRef(transactionRef: String): Resource<PaymentEntity?>
    suspend fun getAllByDoctorId(doctorId: String): Resource<List<PaymentEntity>>
    suspend fun getAllForAdmin(status: String? = null): Resource<List<PaymentEntity>>
    suspend fun updateStatus(id: String, status: String, transactionRef: String?, paymentMethod: String? = null): Resource<PaymentEntity?>
    suspend fun updateFromWebhook(
        id: String,
        status: String,
        invoiceId: String?,
        mpesaReference: String?,
        charges: String?,
        netAmount: String?,
        account: String?,
        paymentMethod: String?,
    ): Resource<PaymentEntity?>
}
