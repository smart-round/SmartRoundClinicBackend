package ke.co.smartroundclinic.admin.domain.service

import ke.co.smartroundclinic.admin.domain.usecase.speciality.CreateSpecialityUseCase
import ke.co.smartroundclinic.admin.domain.usecase.subSpeciality.CreateSubSpecialityUseCase
import ke.co.smartroundclinic.admin.domain.usecase.speciality.DeleteSpecialityUseCase
import ke.co.smartroundclinic.admin.domain.usecase.subSpeciality.DeleteSubSpecialityUseCase
import ke.co.smartroundclinic.admin.domain.usecase.speciality.GetSpecialitiesUseCase
import ke.co.smartroundclinic.admin.domain.usecase.speciality.GetSpecialityByIdUseCase
import ke.co.smartroundclinic.admin.domain.usecase.subSpeciality.GetSubSpecialitiesUseCase
import ke.co.smartroundclinic.admin.domain.usecase.speciality.UpdateSpecialityUseCase
import ke.co.smartroundclinic.admin.domain.usecase.subSpeciality.UpdateSubSpecialityUseCase
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
    private val deleteSpecialityUseCase: DeleteSpecialityUseCase,
    private val deleteSubSpecialityUseCase: DeleteSubSpecialityUseCase,
) {
    suspend fun createSpeciality(req: CreateSpecialityReq, imageBytes: ByteArray?, contentType: String?) =
        createSpecialityUseCase(req, imageBytes, contentType)

    suspend fun updateSpeciality(id: String, req: UpdateSpecialityReq, imageBytes: ByteArray?, contentType: String?) =
        updateSpecialityUseCase(id, req, imageBytes, contentType)

    suspend fun getSpecialities() = getSpecialitiesUseCase()

    suspend fun getSpecialityById(id: String) = getSpecialityByIdUseCase(id)

    suspend fun createSubSpeciality(
        specialityId: String,
        req: CreateSubSpecialityReq,
        imageBytes: ByteArray?,
        contentType: String?,
    ) = createSubSpecialityUseCase(specialityId, req, imageBytes, contentType)

    suspend fun updateSubSpeciality(
        id: String,
        req: UpdateSubSpecialityReq,
        imageBytes: ByteArray?,
        contentType: String?,
    ) = updateSubSpecialityUseCase(id, req, imageBytes, contentType)

    suspend fun getSubSpecialities(specialityId: String) = getSubSpecialitiesUseCase(specialityId)

    suspend fun deleteSpeciality(id: String) = deleteSpecialityUseCase(id)

    suspend fun deleteSubSpeciality(id: String) = deleteSubSpecialityUseCase(id)
}
