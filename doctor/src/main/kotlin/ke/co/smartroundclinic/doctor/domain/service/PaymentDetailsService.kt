package ke.co.smartroundclinic.doctor.domain.service

import ke.co.smartroundclinic.doctor.data.entity.PractitionerPaymentDetailsEntity
import ke.co.smartroundclinic.doctor.domain.usecase.payment.AddPaymentDetailsUseCase
import ke.co.smartroundclinic.doctor.domain.usecase.payment.DeletePaymentDetailsUseCase
import ke.co.smartroundclinic.doctor.domain.usecase.payment.GetAllPaymentDetailsUseCase
import ke.co.smartroundclinic.doctor.domain.usecase.payment.GetPaymentDetailsByIdUseCase
import ke.co.smartroundclinic.doctor.domain.usecase.payment.GetPaymentDetailsUseCase
import ke.co.smartroundclinic.doctor.domain.usecase.payment.UpdatePaymentDetailsUseCase

class PaymentDetailsService(
    private val addUseCase: AddPaymentDetailsUseCase,
    private val getUseCase: GetPaymentDetailsUseCase,
    private val updateUseCase: UpdatePaymentDetailsUseCase,
    private val deleteUseCase: DeletePaymentDetailsUseCase,
    private val getAllUseCase: GetAllPaymentDetailsUseCase,
    private val getByIdUseCase: GetPaymentDetailsByIdUseCase,
) {
    suspend fun add(entity: PractitionerPaymentDetailsEntity) = addUseCase(entity)
    suspend fun get(doctorId: String) = getUseCase(doctorId)
    suspend fun update(doctorId: String, accountName: String?, accountNumber: String?, bankCode: String?, branchCode: String?, bankName: String?, branchName: String?) =
        updateUseCase(doctorId, accountName, accountNumber, bankCode, branchCode, bankName, branchName)
    suspend fun delete(doctorId: String) = deleteUseCase(doctorId)
    suspend fun getAll(page: Int, size: Int) = getAllUseCase(page, size)
    suspend fun getById(id: String) = getByIdUseCase(id)
}