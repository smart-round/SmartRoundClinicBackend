package ke.co.smartroundclinic.admin.domain.service

import ke.co.smartroundclinic.admin.domain.usecase.speciality.CreateSpecialityUseCase
import ke.co.smartroundclinic.admin.domain.usecase.subSpeciality.CreateSubSpecialityUseCase
import ke.co.smartroundclinic.admin.domain.usecase.speciality.DeleteSpecialityUseCase
import ke.co.smartroundclinic.admin.domain.usecase.subSpeciality.DeleteSubSpecialityUseCase
import ke.co.smartroundclinic.admin.domain.usecase.speciality.RemoveSpecialityIconUseCase
import ke.co.smartroundclinic.admin.domain.usecase.subSpeciality.RemoveSubSpecialityIconUseCase
import ke.co.smartroundclinic.admin.domain.usecase.speciality.UploadSpecialityIconUseCase
import ke.co.smartroundclinic.admin.domain.usecase.subSpeciality.UploadSubSpecialityIconUseCase
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
    private val uploadSpecialityIconUseCase: UploadSpecialityIconUseCase,
    private val removeSpecialityIconUseCase: RemoveSpecialityIconUseCase,
    private val uploadSubSpecialityIconUseCase: UploadSubSpecialityIconUseCase,
    private val removeSubSpecialityIconUseCase: RemoveSubSpecialityIconUseCase,
) {
    suspend fun createSpeciality(requests: List<CreateSpecialityReq>) =
        createSpecialityUseCase(requests)

    suspend fun updateSpeciality(id: String, body: UpdateSpecialityReq) =
        updateSpecialityUseCase(id, serviceTierId = body.serviceTierId,body.title,  body.description, body.color, body.iconUrl)

    suspend fun getSpecialities() = getSpecialitiesUseCase()

    suspend fun getSpecialityById(id: String) = getSpecialityByIdUseCase(id)

    suspend fun createSubSpeciality(specialityId: String, body: CreateSubSpecialityReq) =
        createSubSpecialityUseCase(specialityId, body)

    suspend fun updateSubSpeciality(id: String, body: UpdateSubSpecialityReq) =
        updateSubSpecialityUseCase(id, body.title, body.description, body.color, body.iconUrl)

    suspend fun getSubSpecialities(specialityId: String) = getSubSpecialitiesUseCase(specialityId)

    suspend fun deleteSpeciality(id: String) = deleteSpecialityUseCase(id)

    suspend fun deleteSubSpeciality(id: String) = deleteSubSpecialityUseCase(id)

    suspend fun uploadSpecialityIcon(id: String, imageBytes: ByteArray, contentType: String) =
        uploadSpecialityIconUseCase(id, imageBytes, contentType)

    suspend fun removeSpecialityIcon(id: String) = removeSpecialityIconUseCase(id)

    suspend fun uploadSubSpecialityIcon(id: String, imageBytes: ByteArray, contentType: String) =
        uploadSubSpecialityIconUseCase(id, imageBytes, contentType)

    suspend fun removeSubSpecialityIcon(id: String) = removeSubSpecialityIconUseCase(id)
}
