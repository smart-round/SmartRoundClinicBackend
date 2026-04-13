package ke.co.smartroundclinic.admin.domain.service

import ke.co.smartroundclinic.admin.domain.model.DoctorLicenceStatus
import ke.co.smartroundclinic.admin.domain.model.KmpdcRegisterType
import ke.co.smartroundclinic.admin.domain.usecase.FindKmpdcByRegNumberUseCase
import ke.co.smartroundclinic.admin.domain.usecase.GetKmpdcPractitionersUseCase
import ke.co.smartroundclinic.admin.domain.usecase.RefreshKmpdcRegisterUseCase
import ke.co.smartroundclinic.admin.domain.usecase.SearchKmpdcByNameUseCase

class KmpdcService(
    private val refreshKmpdcRegisterUseCase: RefreshKmpdcRegisterUseCase,
    private val getKmpdcPractitionersUseCase: GetKmpdcPractitionersUseCase,
    private val findKmpdcByRegNumberUseCase: FindKmpdcByRegNumberUseCase,
    private val searchKmpdcByNameUseCase: SearchKmpdcByNameUseCase,
) {
    suspend fun refresh() = refreshKmpdcRegisterUseCase()

    suspend fun getPractitioners(
        registerType: KmpdcRegisterType?,
        status: DoctorLicenceStatus?,
        page: Int,
        size: Int,
    ) = getKmpdcPractitionersUseCase(registerType, status, page, size)

    suspend fun findByRegNumber(regNumber: String) = findKmpdcByRegNumberUseCase(regNumber)

    suspend fun searchByName(query: String, page: Int, size: Int) =
        searchKmpdcByNameUseCase(query, page, size)
}