package ke.co.smartroundclinic.payments.domain.repository

import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.payments.data.remote.dto.request.CreateSubAccountReq
import ke.co.smartroundclinic.payments.data.remote.dto.response.CreateSubAccountRes
import ke.co.smartroundclinic.payments.data.remote.dto.request.UpdateSubAccountReq
import ke.co.smartroundclinic.payments.data.remote.dto.request.ValidateAccountReq
import ke.co.smartroundclinic.payments.data.remote.dto.response.GetSubAccountRes
import ke.co.smartroundclinic.payments.data.remote.dto.response.ListSubAccountsRes
import ke.co.smartroundclinic.payments.data.remote.dto.response.ResolveAccountRes
import ke.co.smartroundclinic.payments.data.remote.dto.response.ResolveCardBinRes
import ke.co.smartroundclinic.payments.data.remote.dto.response.UpdateSubAccountRes
import ke.co.smartroundclinic.payments.data.remote.dto.response.ValidateAccountRes

interface PaystackRepository {
    suspend fun createSubAccount(body: CreateSubAccountReq): Resource<CreateSubAccountRes>
    suspend fun updateSubAccount(idOrCode: String, body: UpdateSubAccountReq): Resource<UpdateSubAccountRes>
    suspend fun listSubAccounts(perPage: Int, page: Int, from: String?, to: String?): Resource<ListSubAccountsRes>
    suspend fun getSubAccount(idOrCode: String): Resource<GetSubAccountRes>
    suspend fun resolveAccount(accountNumber: String, bankCode: String): Resource<ResolveAccountRes>
    suspend fun validateAccount(body: ValidateAccountReq): Resource<ValidateAccountRes>
    suspend fun resolveCardBin(bin: String): Resource<ResolveCardBinRes>
}