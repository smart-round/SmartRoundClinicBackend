package ke.co.smartroundclinic.doctor.domain.usecase.bank

import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.doctor.domain.model.toModel
import ke.co.smartroundclinic.doctor.domain.repository.LocalBankRepository
import ke.co.smartroundclinic.doctor.presentation.dto.response.LocalBankPageResult
import ke.co.smartroundclinic.doctor.presentation.dto.response.LocalBankRes
import ke.co.smartroundclinic.doctor.presentation.dto.response.toRes
import kotlin.math.ceil

class GetAllLocalBanksUseCase(private val repository: LocalBankRepository) {
    suspend operator fun invoke(page: Int, size: Int): DefaultResponse<List<LocalBankRes?>?> =
        repository.getAll().toDefaultResponse { bankEntities ->
            bankEntities?.map { it.toModel().toRes() }
        }
}



