package ke.co.smartroundclinic.scheduling.domain.service

import ke.co.smartroundclinic.scheduling.domain.usecase.appointment.BookAppointmentUseCase
import ke.co.smartroundclinic.scheduling.domain.usecase.appointment.CancelAppointmentUseCase
import ke.co.smartroundclinic.scheduling.domain.usecase.appointment.CompleteAppointmentUseCase
import ke.co.smartroundclinic.scheduling.domain.usecase.appointment.ConfirmAppointmentUseCase
import ke.co.smartroundclinic.scheduling.domain.usecase.appointment.GetAllAppointmentsUseCase
import ke.co.smartroundclinic.scheduling.domain.usecase.appointment.GetDoctorAppointmentDetailsUseCase
import ke.co.smartroundclinic.scheduling.domain.usecase.appointment.GetAppointmentByIdUseCase
import ke.co.smartroundclinic.scheduling.domain.usecase.appointment.GetDoctorAppointmentsUseCase
import ke.co.smartroundclinic.scheduling.domain.usecase.appointment.GetNextAppointmentUseCase
import ke.co.smartroundclinic.scheduling.domain.usecase.appointment.GetPatientAppointmentsUseCase
import ke.co.smartroundclinic.scheduling.domain.usecase.appointment.GetPatientAppointmentsForAdminUseCase
import ke.co.smartroundclinic.scheduling.domain.usecase.appointment.MarkNoShowUseCase
import ke.co.smartroundclinic.scheduling.presentation.dto.request.BookAppointmentReq
import ke.co.smartroundclinic.scheduling.presentation.dto.request.CancelAppointmentReq

class AppointmentService(
    private val bookAppointmentUseCase: BookAppointmentUseCase,
    private val getAppointmentByIdUseCase: GetAppointmentByIdUseCase,
    private val getAllAppointmentsUseCase: GetAllAppointmentsUseCase,
    private val getDoctorAppointmentDetailsUseCase: GetDoctorAppointmentDetailsUseCase,
    private val getPatientAppointmentsUseCase: GetPatientAppointmentsUseCase,
    private val getPatientAppointmentsForAdminUseCase: GetPatientAppointmentsForAdminUseCase,
    private val getDoctorAppointmentsUseCase: GetDoctorAppointmentsUseCase,
    private val getNextAppointmentUseCase: GetNextAppointmentUseCase,
    private val confirmAppointmentUseCase: ConfirmAppointmentUseCase,
    private val cancelAppointmentUseCase: CancelAppointmentUseCase,
    private val completeAppointmentUseCase: CompleteAppointmentUseCase,
    private val markNoShowUseCase: MarkNoShowUseCase,
) {
    suspend fun book(req: BookAppointmentReq, patientId: String) = bookAppointmentUseCase(req, patientId)
    suspend fun getById(id: String) = getAppointmentByIdUseCase(id)
    suspend fun getAll() = getAllAppointmentsUseCase()
    suspend fun getDoctorAppointmentDetails(doctorId: String, filter: String? = null) = getDoctorAppointmentDetailsUseCase(doctorId, filter)
    suspend fun getByPatient(patientId: String) = getPatientAppointmentsUseCase(patientId)
    suspend fun getByPatientForAdmin(patientId: String) = getPatientAppointmentsForAdminUseCase(patientId)
    suspend fun getByDoctor(doctorId: String, date: String) = getDoctorAppointmentsUseCase(doctorId, date)
    suspend fun getNext(doctorId: String, patientId: String) = getNextAppointmentUseCase(doctorId, patientId)
    suspend fun confirm(id: String, doctorId: String) = confirmAppointmentUseCase(id, doctorId)
    suspend fun cancel(id: String, userId: String, role: String, req: CancelAppointmentReq) =
        cancelAppointmentUseCase(id, userId, role, req.reason)
    suspend fun complete(id: String, doctorId: String) = completeAppointmentUseCase(id, doctorId)
    suspend fun markNoShow(id: String, doctorId: String) = markNoShowUseCase(id, doctorId)
}
