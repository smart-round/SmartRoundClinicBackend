package ke.co.smartroundclinic.doctor.domain.service

import ke.co.smartroundclinic.doctor.domain.usecase.bank.FindLocalBankByCodeUseCase
import ke.co.smartroundclinic.doctor.domain.usecase.bank.FindLocalBanksByBranchCodeUseCase
import ke.co.smartroundclinic.doctor.domain.usecase.bank.GetAllLocalBanksUseCase
import ke.co.smartroundclinic.doctor.domain.usecase.bank.SearchLocalBanksByNameUseCase

class LocalBankService(
    private val getAllUseCase: GetAllLocalBanksUseCase,
    private val searchByNameUseCase: SearchLocalBanksByNameUseCase,
    private val findByCodeUseCase: FindLocalBankByCodeUseCase,
    private val findByBranchCodeUseCase: FindLocalBanksByBranchCodeUseCase,
) {
    suspend fun getAll(page: Int, size: Int) = getAllUseCase(page, size)
    suspend fun searchByName(query: String, page: Int, size: Int) = searchByNameUseCase(query, page, size)
    suspend fun findByCode(bankCode: String) = findByCodeUseCase(bankCode)
    suspend fun findByBranchCode(branchCode: String) = findByBranchCodeUseCase(branchCode)
}