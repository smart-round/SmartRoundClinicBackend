package ke.co.smartroundclinic.doctor.domain.usecase.bank

import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.doctor.domain.model.toModel
import ke.co.smartroundclinic.doctor.domain.repository.LocalBankRepository
import ke.co.smartroundclinic.doctor.presentation.dto.response.LocalBankRes
import ke.co.smartroundclinic.doctor.presentation.dto.response.toRes

class FindLocalBankByCodeUseCase(private val repository: LocalBankRepository) {
    suspend operator fun invoke(bankCode: String): DefaultResponse<LocalBankRes?> =
        repository.findByBankCode(bankCode).toDefaultResponse { it?.toModel()?.toRes() }
}