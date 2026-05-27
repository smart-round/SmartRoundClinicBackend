package ke.co.smartroundclinic.payments.domain.service

import ke.co.smartroundclinic.payments.domain.usecase.GetAllPaymentsUseCase
import ke.co.smartroundclinic.payments.domain.usecase.GetPaymentByAppointmentUseCase
import ke.co.smartroundclinic.payments.domain.usecase.GetPaymentByIdUseCase
import ke.co.smartroundclinic.payments.domain.usecase.GetPaymentsByDoctorUseCase
import ke.co.smartroundclinic.payments.domain.usecase.GetPaymentsByPatientUseCase
import ke.co.smartroundclinic.payments.domain.usecase.InitiatePaymentUseCase
import ke.co.smartroundclinic.payments.domain.usecase.UpdatePaymentStatusUseCase
import ke.co.smartroundclinic.payments.presentation.dto.request.InitiatePaymentReq
import ke.co.smartroundclinic.payments.presentation.dto.request.UpdatePaymentStatusReq

class PaymentService(
    private val initiateUseCase: InitiatePaymentUseCase,
    private val getByIdUseCase: GetPaymentByIdUseCase,
    private val getByAppointmentUseCase: GetPaymentByAppointmentUseCase,
    private val getByPatientUseCase: GetPaymentsByPatientUseCase,
    private val getByDoctorUseCase: GetPaymentsByDoctorUseCase,
    private val getAllUseCase: GetAllPaymentsUseCase,
    private val updateStatusUseCase: UpdatePaymentStatusUseCase,
) {
    suspend fun initiate(req: InitiatePaymentReq, patientId: String) = initiateUseCase(req, patientId)
    suspend fun getById(id: String) = getByIdUseCase(id)
    suspend fun getByAppointment(appointmentId: String) = getByAppointmentUseCase(appointmentId)
    suspend fun getByPatient(patientId: String, page: Int, size: Int) = getByPatientUseCase(patientId, page, size)
    suspend fun getByDoctor(doctorId: String, page: Int, size: Int) = getByDoctorUseCase(doctorId, page, size)
    suspend fun getAll(page: Int, size: Int, status: String?) = getAllUseCase(page, size, status)
    suspend fun updateStatus(id: String, req: UpdatePaymentStatusReq) = updateStatusUseCase(id, req.status, req.transactionRef)
}
