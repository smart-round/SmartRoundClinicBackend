package ke.co.smartroundclinic.payments.domain.service

import ke.co.smartroundclinic.payments.data.remote.dto.request.UpdateSubAccountReq
import ke.co.smartroundclinic.payments.domain.usecase.subaccount.CreateMySubAccountUseCase
import ke.co.smartroundclinic.payments.domain.usecase.subaccount.GetMySubAccountUseCase
import ke.co.smartroundclinic.payments.domain.usecase.subaccount.GetSubAccountUseCase
import ke.co.smartroundclinic.payments.domain.usecase.subaccount.ListSubAccountsUseCase
import ke.co.smartroundclinic.payments.domain.usecase.subaccount.UpdateMySubAccountUseCase

class SubAccountService(
    private val createMyUseCase: CreateMySubAccountUseCase,
    private val getMyUseCase: GetMySubAccountUseCase,
    private val updateMyUseCase: UpdateMySubAccountUseCase,
    private val listUseCase: ListSubAccountsUseCase,
    private val getUseCase: GetSubAccountUseCase,
) {
    suspend fun createMine(doctorId: String) = createMyUseCase(doctorId)
    suspend fun getMine(doctorId: String) = getMyUseCase(doctorId)
    suspend fun updateMine(doctorId: String, req: UpdateSubAccountReq) = updateMyUseCase(doctorId, req)
    suspend fun list(perPage: Int, page: Int, from: String?, to: String?) = listUseCase(perPage, page, from, to)
    suspend fun getByIdOrCode(idOrCode: String) = getUseCase(idOrCode)
}
