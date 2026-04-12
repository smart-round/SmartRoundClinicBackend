package ke.co.smartroundclinic.admin.domain.service

import ke.co.smartroundclinic.admin.domain.usecase.CreateSpecialityUseCase
import ke.co.smartroundclinic.admin.domain.usecase.CreateSubSpecialityUseCase
import ke.co.smartroundclinic.admin.domain.usecase.DeleteSubSpecialityUseCase
import ke.co.smartroundclinic.admin.domain.usecase.GetSpecialitiesUseCase
import ke.co.smartroundclinic.admin.domain.usecase.GetSpecialityByIdUseCase
import ke.co.smartroundclinic.admin.domain.usecase.GetSubSpecialitiesUseCase
import ke.co.smartroundclinic.admin.domain.usecase.UpdateSpecialityUseCase
import ke.co.smartroundclinic.admin.domain.usecase.UpdateSubSpecialityUseCase
import ke.co.smartroundclinic.admin.presentation.dto.request.CreateSpecialityReq
import ke.co.smartroundclinic.admin.presentation.dto.request.CreateSubSpecialityReq
import ke.co.smartroundclinic.admin.presentation.dto.request.UpdateSpecialityReq
import ke.co.smartroundclinic.admin.presentation.dto.request.UpdateSubSpecialityReq

class SpecialityService(
    private val createSpecialityUseCase: CreateSpecialityUseCase,
    private val updateSpecialityUseCase: UpdateSpecialityUseCase,
    private val getSpecialitiesUseCase: GetSpecialitiesUseCase,
    private val getSpecialityByIdUseCase: GetSpecialityByIdUseCase,
    private val createSubSpecialityUseCase: CreateSubSpecialityUseCase,
    private val updateSubSpecialityUseCase: UpdateSubSpecialityUseCase,
    private val getSubSpecialitiesUseCase: GetSubSpecialitiesUseCase,
    private val deleteSubSpecialityUseCase: DeleteSubSpecialityUseCase,
) {
    suspend fun createSpeciality(requests: List<CreateSpecialityReq>) =
        createSpecialityUseCase(requests)

    suspend fun updateSpeciality(id: String, body: UpdateSpecialityReq) =
        updateSpecialityUseCase(id, body.title, body.description, body.color, body.iconUrl)

    suspend fun getSpecialities() = getSpecialitiesUseCase()

    suspend fun getSpecialityById(id: String) = getSpecialityByIdUseCase(id)

    suspend fun createSubSpeciality(specialityId: String, body: CreateSubSpecialityReq) =
        createSubSpecialityUseCase(specialityId, body)

    suspend fun updateSubSpeciality(id: String, body: UpdateSubSpecialityReq) =
        updateSubSpecialityUseCase(id, body.title, body.description, body.color, body.iconUrl)

    suspend fun getSubSpecialities(specialityId: String) = getSubSpecialitiesUseCase(specialityId)

    suspend fun deleteSubSpeciality(id: String) = deleteSubSpecialityUseCase(id)
}
