package ke.co.smartroundclinic.doctor.domain.usecase.bank

import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.doctor.domain.model.toModel
import ke.co.smartroundclinic.doctor.domain.repository.LocalBankRepository
import ke.co.smartroundclinic.doctor.presentation.dto.response.LocalBankRes
import ke.co.smartroundclinic.doctor.presentation.dto.response.toRes

class FindLocalBanksByBranchCodeUseCase(private val repository: LocalBankRepository) {
    suspend operator fun invoke(branchCode: String): DefaultResponse<List<LocalBankRes>?> =
        repository.findByBranchCode(branchCode).toDefaultResponse { items ->
            items?.map { it.toModel().toRes() }
        }
}